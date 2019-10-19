/*******************************************************************************
 * Copyright (c) 2009-2012 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Paul Klint, Jurgen Vinju
 */

package org.rascalmpl.test.infrastructure;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.rascalmpl.interpreter.Evaluator;
import org.rascalmpl.interpreter.ITestResultListener;
import org.rascalmpl.interpreter.NullRascalMonitor;
import org.rascalmpl.interpreter.TestEvaluator;
import org.rascalmpl.interpreter.env.GlobalEnvironment;
import org.rascalmpl.interpreter.env.ModuleEnvironment;
import org.rascalmpl.interpreter.load.StandardLibraryContributor;
import org.rascalmpl.interpreter.result.AbstractFunction;
import org.rascalmpl.interpreter.utils.RascalManifest;
import org.rascalmpl.uri.URIResolverRegistry;
import org.rascalmpl.uri.URIUtil;
import org.rascalmpl.uri.project.ProjectURIResolver;
import org.rascalmpl.values.ValueFactoryFactory;

import io.usethesource.vallang.ISourceLocation;

public class RascalJUnitTestRunner extends Runner {
    private static Evaluator evaluator;
    private static GlobalEnvironment heap;
    private static ModuleEnvironment root;
    private static PrintWriter stderr;
    private static PrintWriter stdout;
    private Description desc;

    private final String prefix;
    private final ISourceLocation projectRoot;
    private final Class<?> clazz;

    static {
        heap = new GlobalEnvironment();
        root = heap.addModule(new ModuleEnvironment("___junit_test___", heap));

        stderr = new PrintWriter(System.err);
        stdout = new PrintWriter(System.out);
        evaluator = new Evaluator(ValueFactoryFactory.getValueFactory(), stderr, stdout,  root, heap);
        evaluator.addRascalSearchPathContributor(StandardLibraryContributor.getInstance());
        evaluator.getConfiguration().setErrors(true);
    }  

    public RascalJUnitTestRunner(Class<?> clazz) {
        this.prefix = clazz.getAnnotation(RascalJUnitTestPrefix.class).value();
        this.projectRoot = inferProjectRoot(clazz);
        this.clazz = clazz;
        
        System.err.println("Rascal JUnit Project root: " + projectRoot);

        if (projectRoot != null) {
            configureProjectEvaluator(evaluator, projectRoot);
        }
        else {
            throw new IllegalArgumentException("could not setup tests for " + clazz.getCanonicalName());
        }
    }

    public static void configureProjectEvaluator(Evaluator evaluator, ISourceLocation projectRoot) {
        URIResolverRegistry reg = URIResolverRegistry.getInstance();
        String projectName = new RascalManifest().getProjectName(projectRoot);
        reg.registerLogical(new ProjectURIResolver(projectRoot, projectName));
        List<String> sourceRoots = new RascalManifest().getSourceRoots(projectRoot);
        
        ISourceLocation root = URIUtil.correctLocation("project", projectName, "");
        for (String src : sourceRoots) {
            ISourceLocation path = URIUtil.getChildLocation(root, src);
            evaluator.addRascalSearchPath(path);
        }
    }

    public static ISourceLocation inferProjectRoot(Class<?> clazz) {
        try {
            String file = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
            if (file.endsWith(".jar")) {
                throw new IllegalArgumentException("can not run Rascal JUnit tests from within a jar file");
            }

            File current = new File(file);
            while (current != null && current.exists() && current.isDirectory()) {
                if (new File(current, "META-INF/RASCAL.MF").exists()) {
                    return URIUtil.createFileLocation(current.getAbsolutePath());
                }
                current = current.getParentFile();
            }
        }
        catch (URISyntaxException e) {
            return null;
        }
        
        return null;
    }

    public static String computeTestName(String name, ISourceLocation loc) {
        return name + ": <" + loc.getOffset() +"," + loc.getLength() +">";
    }

    public static List<String> getRecursiveModuleList(ISourceLocation root, List<String> result) throws IOException {
        Queue<ISourceLocation> todo = new LinkedList<>();
        todo.add(root);
        while (!todo.isEmpty()) {
            ISourceLocation currentDir = todo.poll();
            String prefix = currentDir.getPath().replaceFirst(root.getPath(), "").replaceFirst("/", "").replaceAll("/", "::");
            for (ISourceLocation ent : URIResolverRegistry.getInstance().list(currentDir)) {
                if (ent.getPath().endsWith(".rsc")) {
                    if (prefix.isEmpty()) {
                        result.add(URIUtil.getLocationName(ent).replace(".rsc", ""));
                    }
                    else {
                        result.add(prefix + "::" + URIUtil.getLocationName(ent).replace(".rsc", ""));
                    }
                }
                else {
                    if (URIResolverRegistry.getInstance().isDirectory(ent)) {
                        todo.add(ent);
                    }
                }
            }
        }
        return result;

    }
    @Override
    public Description getDescription() {		
        Description desc = Description.createSuiteDescription(prefix);
        this.desc = desc;

        try {
            List<String> modules = new ArrayList<>(10);
            for (String src : new RascalManifest().getSourceRoots(projectRoot)) {
                getRecursiveModuleList(URIUtil.getChildLocation(projectRoot, src + "/" + prefix.replaceAll("::", "/")), modules);
            }
            
            Collections.shuffle(modules); // make sure the import order is different, not just the reported modules

            for (String module : modules) {
                String name = prefix + "::" + module;
                Description modDesc = Description.createSuiteDescription(name);

                try {
                    evaluator.doImport(new NullRascalMonitor(), name);
                    List<AbstractFunction> tests = heap.getModule(name.replaceAll("\\\\","")).getTests();
                
                    if (tests.isEmpty()) {
                        continue;
                    }
                    
                    desc.addChild(modDesc);

                    // the order of the tests aren't decided by this list so no need to randomly order them.
                    for (AbstractFunction f : tests) {
                        modDesc.addChild(Description.createTestDescription(clazz, computeTestName(f.getName(), f.getAst().getLocation())));
                    }
                }
                catch (Throwable e) {
                    System.err.println(e);
                    desc.addChild(modDesc);

                    Description testDesc = Description.createTestDescription(clazz, name + "compilation failed", new CompilationFailed() {
                        @Override
                        public Class<? extends Annotation> annotationType() {
                            return getClass();
                        }
                    });

                    modDesc.addChild(testDesc);
                }
            }

            return desc;
        } catch (IOException e) {
            throw new RuntimeException("could not create test suite", e);
        } 
    }

    @Override
    public void run(final RunNotifier notifier) {
        if (desc == null) {
            desc = getDescription();
        }
        notifier.fireTestRunStarted(desc);

        for (Description mod : desc.getChildren()) {
            if (mod.getAnnotations().stream().anyMatch(t -> t instanceof CompilationFailed)) {
                notifier.fireTestFailure(new Failure(desc, new IllegalArgumentException(mod.getDisplayName() + " had importing errors")));
                continue;
            }

            Listener listener = new Listener(notifier, mod);
            TestEvaluator runner = new TestEvaluator(evaluator, listener);
            runner.test(mod.getDisplayName());
        }

        notifier.fireTestRunFinished(new Result());
    }

    private final class Listener implements ITestResultListener {
        private final RunNotifier notifier;
        private final Description module;

        private Listener(RunNotifier notifier, Description module) {
            this.notifier = notifier;
            this.module = module;
        }

        private Description getDescription(String name, ISourceLocation loc) {
            String testName = computeTestName(name, loc);

            for (Description child : module.getChildren()) {
                if (child.getMethodName().equals(testName)) {
                    return child;
                }
            }

            throw new IllegalArgumentException(name + " test was never registered");
        }


        @Override
        public void start(String context, int count) {
            notifier.fireTestRunStarted(module);
        }

        @Override
        public void ignored(String test, ISourceLocation loc) {
            notifier.fireTestIgnored(getDescription(test, loc));
        }

        @Override
        public void report(boolean successful, String test, ISourceLocation loc,	String message, Throwable t) {
            Description desc = getDescription(test, loc);
            notifier.fireTestStarted(desc);

            if (!successful) {
                notifier.fireTestFailure(new Failure(desc, t != null ? t : new Exception(message != null ? message : "no message")));
            }
            else {
                notifier.fireTestFinished(desc);
            }
        }

        @Override
        public void done() {
            notifier.fireTestRunFinished(new Result());
        }
    }
}
