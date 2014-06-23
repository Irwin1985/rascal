/*******************************************************************************
 * Copyright (c) 2009-2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   * Mark Hills - Mark.Hills@cwi.nl (CWI)
*******************************************************************************/
package org.rascalmpl.library.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.rascalmpl.interpreter.utils.RuntimeExceptionFactory;

public class ShellExec {
	
	private static HashMap<IInteger, Process> runningProcesses = new HashMap<IInteger, Process>();
	private static HashMap<IInteger, InputStreamReader> processInputStreams = new HashMap<IInteger, InputStreamReader>();
	private static HashMap<IInteger, InputStreamReader> processErrorStreams = new HashMap<IInteger, InputStreamReader>();
	private static HashMap<IInteger, OutputStreamWriter> processOutputStreams = new HashMap<IInteger, OutputStreamWriter>();
	private static IInteger processCounter = null;
	
	private final IValueFactory vf;

	public ShellExec(IValueFactory vf) {
		this.vf = vf;
	}

	public IInteger createProcess(IString processCommand) {
		return createProcessInternal(processCommand,null,null,null);
	}

	public IInteger createProcess(IString processCommand, ISourceLocation workingDir) {
		return createProcessInternal(processCommand,null,null,workingDir);
	}

	public IInteger createProcess(IString processCommand, IList arguments) {
		return createProcessInternal(processCommand,arguments,null,null);
	}

	public IInteger createProcess(IString processCommand, IList arguments, ISourceLocation workingDir) {
		return createProcessInternal(processCommand,arguments,null,workingDir);
	}

	public IInteger createProcess(IString processCommand, IMap envVars) {
		return createProcessInternal(processCommand,null,envVars,null);
	}

	public IInteger createProcess(IString processCommand, IMap envVars, ISourceLocation workingDir) {
		return createProcessInternal(processCommand,null,envVars,workingDir);
	}

	public IInteger createProcess(IString processCommand, IList arguments, IMap envVars) {
		return createProcessInternal(processCommand,arguments,envVars,null);
	}

	public IInteger createProcess(IString processCommand, IList arguments, IMap envVars, ISourceLocation workingDir) {
		return createProcessInternal(processCommand,arguments,envVars,workingDir);
	}

	private synchronized IInteger createProcessInternal(IString processCommand, IList arguments, IMap envVars, ISourceLocation workingDir) {
		try {
			// Build the arg array using the command and the command arguments passed in the arguments list
			List<String> args = new ArrayList<String>();
			args.add(processCommand.getValue());
			for (int n = 0; n < arguments.length(); ++n) {
				if (arguments.get(n) instanceof IString) 
					args.add(((IString)arguments.get(n)).getValue());
				else
					throw RuntimeExceptionFactory.illegalArgument(arguments.get(n),null, null);
			}
			ProcessBuilder pb = new ProcessBuilder(args);
			
			// Built the environment var map using the envVars map
			Map<String,String> vars = new HashMap<String,String>();
			if (envVars != null && envVars.size() > 0) { 
				for (IValue varKey : envVars) {
					if (varKey instanceof IString) {
						IString strKey = (IString) varKey;
						IValue varVal = envVars.get(varKey);
						if (varVal instanceof IString) {
							IString strVal = (IString) varVal;
							vars.put(strKey.getValue(),  strVal.getValue());
						} else {
							throw RuntimeExceptionFactory.illegalArgument(varVal,null, null);
						}
					} else {
						throw RuntimeExceptionFactory.illegalArgument(varKey,null, null);
					}
				}
			}
			Map<String,String> currentEnv = pb.environment();
			try {
				for (String strKey : vars.keySet()) {
					currentEnv.put(strKey, vars.get(strKey));
				}
			} catch (UnsupportedOperationException uoe) {
				throw RuntimeExceptionFactory.permissionDenied(vf.string("Modifying environment variables is not allowed on this machine."), null, null);
			} catch (IllegalArgumentException iae) {
				throw RuntimeExceptionFactory.permissionDenied(vf.string("Modifying environment variables is not allowed on this machine."), null, null);
			}
			
			File cwd = null;
			if (workingDir != null) {
				cwd = new File(workingDir.getURI().getPath());
				pb.directory(cwd);
			}
			
			Process newProcess = pb.start();
			if (processCounter == null) processCounter = vf.integer(0);
			processCounter = processCounter.add(vf.integer(1));
			runningProcesses.put(processCounter, newProcess);
			return processCounter;
		} catch (IOException e) {
			throw RuntimeExceptionFactory.javaException(e, null, null);
		}
	}

	public synchronized void killProcess(IInteger processId) {
		if (!runningProcesses.containsKey(processId))
			throw RuntimeExceptionFactory.illegalArgument(processId, null, null);

		if (processInputStreams.containsKey(processId)) {
			try {
				processInputStreams.get(processId).close();
			} catch (IOException e) {
				// eat it, we are just throwing it away anyway
			} finally {
				processInputStreams.remove(processId);
			}
		}
		
		if (processErrorStreams.containsKey(processId)) {
			try {
				processErrorStreams.get(processId).close();
			} catch (IOException e) {
				// eat it, we are just throwing it away anyway
			} finally {
				processErrorStreams.remove(processId);
			}
		}

		if (processOutputStreams.containsKey(processId)) {
			try {
				processOutputStreams.get(processId).close();
			} catch (IOException e) {
				// eat it, we are just throwing it away anyway
			} finally {
				processOutputStreams.remove(processId);
			}
		}

		Process runningProcess = runningProcesses.get(processId);
		runningProcess.destroy();
		runningProcesses.remove(processId);
		return;
	}
	
	public synchronized IString readFrom(IInteger processId) {
		if (!runningProcesses.containsKey(processId))
			throw RuntimeExceptionFactory.illegalArgument(processId, null, null);
		try {
			Process runningProcess = runningProcesses.get(processId);
			InputStreamReader isr = null;
			if (processInputStreams.containsKey(processId)) {
				isr = processInputStreams.get(processId);
			} else {
				isr = new InputStreamReader(runningProcess.getInputStream());
				processInputStreams.put(processId, isr);
			}
			StringBuffer line = new StringBuffer();
			while (isr.ready()) {
				line.append((char)isr.read());
			}
			return vf.string(line.toString());
		} catch (IOException e) {
			throw RuntimeExceptionFactory.javaException(e, null, null);
		}
	}
	
	public synchronized IString readFromErr(IInteger processId) {
		if (!runningProcesses.containsKey(processId))
			throw RuntimeExceptionFactory.illegalArgument(processId, null, null);
		try {
			Process runningProcess = runningProcesses.get(processId);
			InputStreamReader isr = null;
			if (processErrorStreams.containsKey(processId)) {
				isr = processErrorStreams.get(processId);
			} else {
				isr = new InputStreamReader(runningProcess.getErrorStream());
				processErrorStreams.put(processId, isr);
			}
			StringBuffer line = new StringBuffer();
			while (isr.ready()) {
				line.append((char)isr.read());
			}
			return vf.string(line.toString());
		} catch (IOException e) {
			throw RuntimeExceptionFactory.javaException(e, null, null);
		}
	}

	public synchronized IString readEntireStream(IInteger processId) {
		if (!runningProcesses.containsKey(processId))
			throw RuntimeExceptionFactory.illegalArgument(processId, null, null);
		try {
			Process runningProcess = runningProcesses.get(processId);
			BufferedReader br = new BufferedReader(new InputStreamReader(runningProcess.getInputStream()));
			StringBuffer lines = new StringBuffer();
			String line = "";
			while (null != (line = br.readLine())) {
				lines.append(line);
			}
			if (br != null) br.close();
			return vf.string(lines.toString());
		} catch (IOException e) {
			throw RuntimeExceptionFactory.javaException(e, null, null);
		}
	}

	public synchronized IString readEntireErrStream(IInteger processId) {
		if (!runningProcesses.containsKey(processId))
			throw RuntimeExceptionFactory.illegalArgument(processId, null, null);
		try {
			Process runningProcess = runningProcesses.get(processId);
			BufferedReader br = new BufferedReader(new InputStreamReader(runningProcess.getErrorStream()));
			StringBuffer lines = new StringBuffer();
			String line = "";
			while (null != (line = br.readLine())) {
				lines.append(line);
			}
			if (br != null) br.close();
			return vf.string(lines.toString());
		} catch (IOException e) {
			throw RuntimeExceptionFactory.javaException(e, null, null);
		}
	}

	public void writeTo(IInteger processId, IString msg) {
		if (!runningProcesses.containsKey(processId))
			throw RuntimeExceptionFactory.illegalArgument(processId, null, null);
		try {
			Process runningProcess = runningProcesses.get(processId);
			OutputStreamWriter osw = null;
			if (processOutputStreams.containsKey(processId)) {
				osw = processOutputStreams.get(processId);
			} else {
				osw = new OutputStreamWriter(runningProcess.getOutputStream());
				processOutputStreams.put(processId, osw);
			}
			osw.append(msg.getValue());
			osw.flush();
		} catch (IOException e) {
			throw RuntimeExceptionFactory.javaException(e, null, null);
		}
	}

}
