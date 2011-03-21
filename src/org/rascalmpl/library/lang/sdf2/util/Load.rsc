module lang::sdf2::util::Load

import lang::sdf2::syntax::Sdf2;
import IO;
import Exception;
import String;
import Set;

public SDF loadSDF2Module(str name, list[loc] path) {
  set[Module] modules = {};
  set[str] newnames = {name};
  set[str] done = {};
  
  while (newnames != {}) {
    <n,newnames> = takeOneFrom(newnames);
    
    if (n notin done) {
      file = find(replaceAll(n,"::","/") + ".sdf", path);
      mod = parse(#Module, file);
      modules += mod;
      newnames += getImports(mod);
      done += {n};
    }
  }

  def = "definition
         '
         '<for (Module m <- modules) {>
         '<m><}>";
  
  return parse(#SDF, def);
}

public set[str] getImports(Module mod) {
  return { "<name>" | /Import i := mod,  /ModuleId name := i};
}
