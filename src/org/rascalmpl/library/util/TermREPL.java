package org.rascalmpl.library.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.rascalmpl.interpreter.Evaluator;
import org.rascalmpl.interpreter.IEvaluatorContext;
import org.rascalmpl.interpreter.result.ICallableValue;
import org.rascalmpl.library.lang.json.io.JsonValueWriter;
import org.rascalmpl.repl.BaseREPL;
import org.rascalmpl.repl.CompletionResult;
import org.rascalmpl.repl.ILanguageProtocol;
import org.rascalmpl.repl.REPLContentServer;
import org.rascalmpl.repl.REPLContentServerManager;
import org.rascalmpl.uri.URIResolverRegistry;

import com.google.gson.stream.JsonWriter;

import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import io.usethesource.vallang.IBool;
import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IInteger;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.IWithKeywordParameters;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import jline.TerminalFactory;

public class TermREPL {
    private final IValueFactory vf;
    private ILanguageProtocol lang;
   
    
    public TermREPL(IValueFactory vf) {
        this.vf = vf;
    }

    public void startREPL(IConstructor repl, IString title, IString welcome, IString prompt, IString quit,
        ISourceLocation history, IValue handler, IValue completor, IValue stacktrace, IEvaluatorContext ctx) {
        try {
            lang = new TheREPL(vf, title, welcome, prompt, quit, history, handler, completor, stacktrace, ctx.getInput(), ctx.getStdErr(), ctx.getStdOut());
            new BaseREPL(lang, null, ctx.getInput(), ctx.getStdErr(), ctx.getStdOut(), true, true, history, TerminalFactory.get(), null).run();
        } catch (Throwable e) {
            e.printStackTrace(ctx.getErrorPrinter());
        }
    }

    public static class TheREPL implements ILanguageProtocol {
        private final REPLContentServerManager contentManager = new REPLContentServerManager();
        private final TypeFactory tf = TypeFactory.getInstance();
        private OutputStream stdout;
        private OutputStream stderr;
        private InputStream input;
        private String currentPrompt;
        private String quit;
        private final ICallableValue handler;
        private final ICallableValue completor;
        private final IValueFactory vf;
        private final ICallableValue stacktrace;
        
        public TheREPL(IValueFactory vf, IString title, IString welcome, IString prompt, IString quit, ISourceLocation history,
            IValue handler, IValue completor, IValue stacktrace, InputStream input, OutputStream stderr, OutputStream stdout) {
            this.vf = vf;
            this.input = input;
            this.stderr = stderr;
            this.stdout = stdout;
            this.handler = (ICallableValue) handler;
            this.completor = (ICallableValue) completor;
            this.stacktrace = (ICallableValue) stacktrace;
            this.currentPrompt = prompt.getValue();
            this.quit = quit.getValue();
        }

        @Override
        public void cancelRunningCommandRequested() {
            handler.getEval().interrupt();
            handler.getEval().__setInterrupt(false);
        }
        
        @Override
        public void terminateRequested() {
            handler.getEval().interrupt();
        }
        
        @Override
        public void stop() {
            handler.getEval().interrupt();
        }
        
        @Override
        public void stackTraceRequested() {
            stacktrace.call(new Type[0], new IValue[0], null);
        }

        @Override
        public void initialize(InputStream input, OutputStream stdout, OutputStream stderr) {
            this.stdout = stdout;
            this.stderr = stderr;
            this.input = input;
        }

        @Override
        public String getPrompt() {
            return currentPrompt;
        }

        @Override
        public void handleInput(String line, Map<String, InputStream> output, Map<String,String> metadata) throws InterruptedException {
            
            if (line.trim().equals(quit)) {
                throw new InterruptedException(quit);
            }
            else {
                try {
                    handler.getEval().__setInterrupt(false);
                    IConstructor content = (IConstructor)call(handler, new Type[] { tf.stringType() }, new IValue[] { vf.string(line) });
               
                    if (content.has("id")) {
                        handleInteractiveContent(output, metadata, content);
                    }
                    else {
                        IConstructor response = (IConstructor) content.get("response");
                        switch (response.getName()) {
                            case "response":
                                handlePlainTextResponse(output, response);
                                break;
                            case "fileResponse":
                                handleFileResponse(output, response);
                                break;
                            case "jsonResponse":
                                handleJSONResponse(output, response);
                        }
                    }
                }
                catch (IOException e) {
                    output.put("text/plain", new ByteArrayInputStream(e.getMessage().getBytes()));
                }
            }
        }

        private void handleInteractiveContent(Map<String, InputStream> output, Map<String, String> metadata,
            IConstructor content) throws IOException, UnsupportedEncodingException {
            String id = ((IString) content.get("id")).getValue();
            Function<IValue, IValue> callback = liftProviderFunction(content.get("callback"));
            REPLContentServer server = contentManager.addServer(id, callback);
            
            Response response = server.serve("/", Method.GET, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
            String URL = "http://localhost:" + server.getListeningPort() + "/";
            metadata.put("url", URL);
            
            output.put(response.getMimeType(), response.getData());
            
            String message = "Serving visual content at |" + URL + "|";
            output.put("text/plain", new ByteArrayInputStream(message.getBytes("UTF8")));
        }

        private Function<IValue, IValue> liftProviderFunction(IValue callback) {
            ICallableValue func = (ICallableValue) callback;

            return (t) -> {
                // This function will be called from another thread (the webserver)
                // That is problematic if the current repl is doing something else at that time.
                // The evaluator is already locked by the outer Rascal REPL (if this REPL was started from `startREPL`).
//              synchronized(eval) {
                  return func.call(
                      new Type[] { REPLContentServer.requestType },
                      new IValue[] { t },
                      Collections.emptyMap()).getValue();
//              }
            };
        }
        
        private void handleJSONResponse(Map<String, InputStream> output, IConstructor response) throws IOException {
            IValue data = response.get("val");
            IWithKeywordParameters<? extends IConstructor> kws = response.asWithKeywordParameters();
            
            IValue dtf = kws.getParameter("dateTimeFormat");
            IValue ics = kws.getParameter("implicitConstructors");
            IValue ipn = kws.getParameter("implicitNodes");
            IValue dai = kws.getParameter("dateTimeAsInt");
            
            JsonValueWriter writer = new JsonValueWriter()
                .setCalendarFormat(dtf != null ? ((IString) dtf).getValue() : "yyyy-MM-dd\'T\'HH:mm:ss\'Z\'")
                .setConstructorsAsObjects(ics != null ? ((IBool) ics).getValue() : true)
                .setNodesAsObjects(ipn != null ? ((IBool) ipn).getValue() : true)
                .setDatesAsInt(dai != null ? ((IBool) dai).getValue() : true);

              final ByteArrayOutputStream baos = new ByteArrayOutputStream();
              
              JsonWriter out = new JsonWriter(new OutputStreamWriter(baos, Charset.forName("UTF8")));
              
              writer.write(out, data);
              out.flush();
              out.close();
              
              output.put("application/json",  new ByteArrayInputStream(baos.toByteArray()));
        }

        private void handleFileResponse(Map<String, InputStream> output, IConstructor response)
            throws UnsupportedEncodingException {
            IString fileMimetype = (IString) response.get("mimeType");
            ISourceLocation file = (ISourceLocation) response.get("file");
            try {
                output.put(fileMimetype.getValue(), URIResolverRegistry.getInstance().getInputStream(file));
            }
            catch (IOException e) {
                output.put("text/plain", new ByteArrayInputStream(e.getMessage().getBytes("UTF8")));
            }
        }

        private void handlePlainTextResponse(Map<String, InputStream> output, IConstructor response)
            throws UnsupportedEncodingException {
            IString content = (IString) response.get("content");
            IString contentMimetype = (IString) response.get("mimeType");
                  
            output.put(contentMimetype.getValue(), new ByteArrayInputStream(content.getValue().getBytes("UTF8")));
        }

        @Override
        public boolean supportsCompletion() {
            return true;
        }

        @Override
        public boolean printSpaceAfterFullCompletion() {
            return false;
        }

        private IValue call(ICallableValue f, Type[] types, IValue[] args) {
            synchronized (f.getEval()) {
                Evaluator eval = (Evaluator) f.getEval();
                OutputStream prevErr = eval.getStdErr();
                OutputStream prevOut = eval.getStdOut();
                try {
                    eval.overrideDefaultWriters(input, stdout, stderr);
                    return f.call(types, args, null).getValue();
                }
                finally {
                    try {
                        stdout.flush();
                        stderr.flush();
                        eval.overrideDefaultWriters(eval.getInput(), prevOut, prevErr);
                    }
                    catch (IOException e) {
                        // ignore
                    }
                }
            }
        }

        @Override
        public CompletionResult completeFragment(String line, int cursor) {
            ITuple result = (ITuple)call(completor, new Type[] { tf.stringType(), tf.integerType() },
                            new IValue[] { vf.string(line), vf.integer(cursor) }); 

            List<String> suggestions = new ArrayList<>();

            for (IValue v: (IList)result.get(1)) {
                suggestions.add(((IString)v).getValue());
            }

            if (suggestions.isEmpty()) {
                return null;
            }

            int offset = ((IInteger)result.get(0)).intValue();

            return new CompletionResult(offset, suggestions);
        }
        
        @Override
        public void handleReset(Map<String, InputStream> output, Map<String, String> metadata) throws InterruptedException {
            handleInput("", output, metadata);
        }

        @Override
        public boolean isStatementComplete(String command) {
            return true;
        }
    }
}
