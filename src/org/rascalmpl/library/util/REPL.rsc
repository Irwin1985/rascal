module util::REPL

extend Content;

alias Completion
 = tuple[int offset, list[str] suggestions];

data REPL
  = repl(
     str title = "", 
     str welcome = "", 
     str prompt = "\n\>",
     str quit = "", 
     loc history = |home:///.term-repl-history|, 
     Content (str command) handler = echo,
     Completion(str line, int cursor) completor = noSuggestions,
     str () stacktrace = str () { return ""; }
   );

private Content echo(str line) = text(line);
   
private Completion noSuggestions(str _, int _) = <0, []>;

@javaClass{org.rascalmpl.library.util.TermREPL}
@reflect
java void startREPL(REPL repl, 
  
  // filling in defaults from the repl constructor, for use in the Java code:
  str title = repl.title, 
  str welcome = repl.welcome, 
  str prompt = repl.prompt, 
  str quit = repl.quit,
  loc history = repl.history,
  Content (str ) handler = repl.handler,
  Completion(str , int) completor = repl.completor,
  str () stacktrace = repl.stacktrace);
