package org.rascalmpl.repl;

import java.io.File;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.rascalmpl.library.experiments.Compiler.RVM.Interpreter.NoSuchRascalFunction;
import org.rascalmpl.library.experiments.Compiler.RVM.Interpreter.ideservices.IDEServices;
import org.rascalmpl.library.util.PathConfig;

import io.usethesource.vallang.ISourceLocation;
import jline.Terminal;
import jline.console.ConsoleReader;
import jline.console.UserInterruptException;
import jline.console.completer.CandidateListCompletionHandler;
import jline.console.completer.Completer;
import jline.console.history.FileHistory;
import jline.console.history.PersistentHistory;
import jline.internal.ShutdownHooks;
import jline.internal.ShutdownHooks.Task;

public class BaseREPL {
    protected final ConsoleReader reader;
    private final OutputStream originalStdOut;
    protected final boolean prettyPrompt;
    protected final boolean allowColors;
    protected final Writer stdErr;
    protected volatile boolean keepRunning = true;
    private volatile Task historyFlusher = null;
    private volatile PersistentHistory history = null;
    private final Queue<String> commandQueue = new ConcurrentLinkedQueue<String>();
    protected IDEServices ideServices;
    private final ILanguageProtocol language;
    
    private static byte CANCEL_RUNNING_COMMAND = (byte)ctrl('C'); 
    private static byte STOP_REPL = (byte)ctrl('D'); 
    private static byte STACK_TRACE = (byte)ctrl('\\'); 


    public BaseREPL(ILanguageProtocol language, PathConfig pcfg, InputStream stdin, OutputStream stdout, boolean prettyPrompt, boolean allowColors, File file, Terminal terminal, IDEServices ideServices) throws IOException, URISyntaxException {
        this(language, pcfg, stdin, stdout, prettyPrompt, allowColors, file != null ? new FileHistory(file) : null, terminal, ideServices);
    }

    public BaseREPL(ILanguageProtocol language, PathConfig pcfg, InputStream stdin, OutputStream stdout, boolean prettyPrompt, boolean allowColors, ISourceLocation file, Terminal terminal, IDEServices ideServices) throws IOException, URISyntaxException {
        this(language, pcfg, stdin, stdout, prettyPrompt, allowColors, file != null ? new SourceLocationHistory(file) : null, terminal, ideServices);
    }

   
    private BaseREPL(ILanguageProtocol language, PathConfig pcfg, InputStream stdin, OutputStream stdout, boolean prettyPrompt, boolean allowColors, PersistentHistory history, Terminal terminal, IDEServices ideServices) throws IOException, URISyntaxException {
        this.originalStdOut = stdout;
        this.language = language;
        
        if (!(stdin instanceof NotifieableInputStream) && !(stdin.getClass().getCanonicalName().contains("jline"))) {
            stdin = new NotifieableInputStream(stdin, new byte[] { CANCEL_RUNNING_COMMAND, STOP_REPL, STACK_TRACE }, (Byte b) -> handleEscape(b));
        }
        reader = new ConsoleReader(stdin, stdout, terminal);
        this.ideServices = ideServices;
        if (history != null) {
            this.history = history;
            reader.setHistory(history);
            historyFlusher = new Task() {
                @Override
                public void run() throws Exception {
                    history.flush();
                }
            };
            ShutdownHooks.add(historyFlusher);
        }
        reader.setExpandEvents(false);

        prettyPrompt = prettyPrompt && terminal.isAnsiSupported();
        this.prettyPrompt = prettyPrompt;
        this.allowColors = allowColors;
        if (prettyPrompt && allowColors) {
            this.stdErr = new RedErrorWriter(reader.getOutput());
        }
        else if (prettyPrompt) {
            this.stdErr = new ItalicErrorWriter(reader.getOutput());
        }
        else {
            this.stdErr = new FilterWriter(reader.getOutput()) { }; // create a basic wrapper to avoid locking on stdout and stderr
        }
        initialize(reader.getOutput(), stdErr);
        if (supportsCompletion()) {
            reader.addCompleter(new Completer(){
                @Override
                public int complete(String buffer, int cursor, List<CharSequence> candidates) {
                    try {
                        CompletionResult res = completeFragment(buffer, cursor);
                        candidates.clear();
                        if (res != null && res.getOffset() > -1 && !res.getSuggestions().isEmpty()) {
                            candidates.addAll(res.getSuggestions());
                            return res.getOffset();
                        }
                        return -1;
                    }
                    catch(Throwable t) {
                        // the completer should never fail, this breaks jline
                        return -1;
                    }
                }
            });
            if (reader.getCompletionHandler() instanceof CandidateListCompletionHandler) {
                ((CandidateListCompletionHandler)reader.getCompletionHandler()).setPrintSpaceAfterFullCompletion(printSpaceAfterFullCompletion());
            }
            reader.setHandleUserInterrupt(true);
        }
    }


    public static char ctrl(char ch) {
        assert 'A' <= ch && ch <= '_'; 
        return (char)((((int)ch) - 'A') + 1);
    }


    /**
     * During the constructor call initialize is called after the REPL is setup enough to have a stdout and std err to write to.
     * @param pcfg the PathConfig to be used
     * @param stdout the output stream to write normal output to.
     * @param stderr the error stream to write error messages on, depending on the environment and options passed, will print in red.
     * @param ideServices TODO
     * @throws NoSuchRascalFunction 
     * @throws IOException 
     * @throws URISyntaxException 
     */
    protected void initialize(Writer stdout, Writer stderr) throws IOException, URISyntaxException {
        language.initialize(stdout, stderr);
    }

    /**
     * Will be called everytime a new prompt is printed.
     * @return The string representing the prompt.
     */
    protected String getPrompt() {
        return language.getPrompt();
    }

    /**
     * After a newline is pressed, the current line is handed to this method.
     * @param line the current line entered.
     * @throws InterruptedException throw this exception to stop the REPL (instead of calling .stop())
     */
    protected void handleInput(String line) throws InterruptedException {
        Map<String,String> output = new HashMap<>();
        
        language.handleInput(line, output, new HashMap<>());
        
        // TODO: maybe we can do this cleaner, but this works for now
        String out = output.get("text/plain");
        
        if (out != null) {
            PrintWriter w = new PrintWriter(originalStdOut);
            w.print(out);
            w.flush();
        }
    }

    /**
     * If a line is canceled with ctrl-C this method is called too handle the reset in the child-class.
     * @throws InterruptedException throw this exception to stop the REPL (instead of calling .stop())
     */
    protected void handleReset(Map<String,String> output, Map<String,String> metadata) throws InterruptedException {
        language.handleReset(output, metadata);
    }

    /**
     * Test if completion of statement in the current line is supported
     * @return true if the completeFragment method can provide completions
     */
    protected boolean supportsCompletion() {
        return language.supportsCompletion();
    }

    /**
     * If the completion succeeded with one match, should a space be printed aftwards?
     * @return true if completed fragment should be followed by a space
     */
    protected boolean printSpaceAfterFullCompletion() {
        return language.printSpaceAfterFullCompletion();
    }

    /**
     * If a user hits the TAB key, the current line and the offset is provided to try and complete a fragment of the current line.
     * @param line The current line.
     * @param cursor The cursor offset in the line.
     * @return suggestions for the line.
     */
    protected CompletionResult completeFragment(String line, int cursor) {
        return language.completeFragment(line, cursor);
    }

    /**
     * This method gets called from another thread, and indicates the user pressed CTLR-C during a call to handleInput.
     * 
     * Interrupt the handleInput code as soon as possible, but leave stuff in a valid state.
     */
    protected void cancelRunningCommandRequested() {
        language.cancelRunningCommandRequested();
    }

    /**
     * This method gets called from another thread, and indicates the user pressed CTLR-D during a call to handleInput.
     * 
     * Quit the code from handleInput as soon as possible, assume the REPL will close after this.
     */
    protected void terminateRequested() {
        language.terminateRequested();
    }

    /**
     * This method gets called from another thread, indicates a user pressed CTRL+\ during a call to handleInput.
     * 
     * If possible, print the current stack trace.
     */
    protected void stackTraceRequested() {
        language.stackTraceRequested();
    }

    private String previousPrompt = "";
    public static final String PRETTY_PROMPT_PREFIX = Ansi.ansi().reset().bold().fg(Color.BLACK).toString();
    public static final String PRETTY_PROMPT_POSTFIX = Ansi.ansi().boldOff().reset().toString();

    protected void updatePrompt() {
        String newPrompt = getPrompt();
        if (!newPrompt.equals(previousPrompt)) {
            previousPrompt = newPrompt;
            if (prettyPrompt) {
                reader.setPrompt(PRETTY_PROMPT_PREFIX + newPrompt + PRETTY_PROMPT_POSTFIX);
            }
            else {
                reader.setPrompt(newPrompt);
            }
        }
    }

    /**
     * Queue a command (separated by newlines) to be "entered"
     */
    public void queueCommand(String command) {
        commandQueue.addAll(Arrays.asList(command.split("[\\n\\r]")));
    }


    private volatile boolean handlingInput = false;

    private boolean handleEscape(Byte b) {
        if (handlingInput) {
            if (b == CANCEL_RUNNING_COMMAND) {
                cancelRunningCommandRequested();
                return true;
            }
            else if (b == STOP_REPL) {
                // jline already handles this
                // but we do have to stop the interpreter
                terminateRequested();
                this.stop();
                return true;
            }
            else if (b == STACK_TRACE) {
                stackTraceRequested();
                return true;
            }
        }
        return false;
    }

    /**
     * This will run the console in the current thread, and will block until it is either:
     * <ul>
     *  <li> handleInput throws an InteruptedException.
     *  <li> input reaches the end of the stream
     *  <li> either the input or output stream throws an IOException 
     * </ul>
     */
    public void run() throws IOException {
        try {
            updatePrompt();
            while(keepRunning) {
                boolean handledQueue = false;
                String queuedCommand;
                while ((queuedCommand = commandQueue.poll()) != null) {
                    handledQueue = true;
                    reader.resetPromptLine(reader.getPrompt(), queuedCommand, 0);
                    reader.println();
                    reader.getHistory().add(queuedCommand);
                    try {
                        handlingInput = true;
                        handleInput(queuedCommand);
                    }
                    finally {
                        handlingInput = false;
                    }
                }
                if (handledQueue) {
                    String oldPrompt = reader.getPrompt();
                    reader.resetPromptLine("", "", 0);
                    reader.setPrompt(oldPrompt);
                }

                updatePrompt();
                try {
                    String line = reader.readLine(reader.getPrompt(), null, null);
                    if (line == null) { // EOF
                        break;
                    }
                    try {
                        handlingInput = true;
                        handleInput(line);
                    }
                    finally {
                        handlingInput = false;
                    }
                }
                catch (UserInterruptException u) {
                    reader.println();
                    Map<String,String> output = new HashMap<>();
                    
                    handleReset(output, new HashMap<>());
                    
                    for (String out : output.values()) {
                        reader.print(out);
                    }
                    
                    updatePrompt();
                }

            }
        }
        catch (IOException e) {
            try (PrintWriter err = new PrintWriter(stdErr, true)) {
                err.println("REPL Failed: ");
                if (!err.checkError()) {
                    e.printStackTrace(err);
                }
                else {
                    e.printStackTrace();
                }
                err.flush();
                stdErr.flush();
            }
            throw e;
        }
        catch (InterruptedException e) {
            // we are closing down, so do nothing, the finally clause will take care of it
        }
        finally {
            reader.getOutput().flush();
            originalStdOut.flush();
            if (historyFlusher != null) {
                ShutdownHooks.remove(historyFlusher);
                history.flush();
            }
            reader.shutdown();
        }
    }

    /**
     * stop the REPL without waiting for it to stop
     */
    public void stop() {
        language.stop();
        keepRunning = false;
        reader.shutdown();
    }
    
    public Terminal getTerminal() {
        return reader.getTerminal();
    }

    public InputStream getInput() {
        return reader.getInput();
    }

    public PrintStream getOutput() {
        return new PrintStream(originalStdOut);
    }
}
