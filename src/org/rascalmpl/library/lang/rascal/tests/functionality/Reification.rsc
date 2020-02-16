@contributor{Jurgen Vinju}
module lang::rascal::tests::functionality::Reification

import ParseTree;

test bool c() = #str.symbol == \str();
test bool i() = #int.symbol == \int();
test bool r() = #real.symbol == \real();
test bool n1() = #num.symbol == \num();
test bool n2() = #node.symbol == \node();
test bool v() = #void.symbol == \void();
test bool vl() = #value.symbol == \value();
test bool l() = #list[int].symbol == \list(\int());
test bool s() = #set[int].symbol == \set(\int());
test bool m1() = #map[int,str].symbol == \map(\int(),\str());
test bool m2() = #map[int k,str v].symbol == \map(label("k",\int()),label("v",\str()));
test bool f() = #int (int).symbol == \func(\int(),[\int()],[]);
test bool p() = #&T <: list[&U].symbol == \parameter("T", \list(\parameter("U",\value())));

@ignoreCompiler{Remove-after-transition-to-compile: minor diff in reification}
test bool relLabels1() = #rel[int a, int b].symbol == \set(\tuple([label("a", \int()),label("b", \int())]));

test bool everyTypeCanBeReifiedWithoutExceptions(&T u) = _ := typeOf(u);

data P = prop(str name) | and(P l, P r) | or(P l, P r) | not(P a) | t() | f() | axiom(P mine = t());

test bool allConstructorsAreDefined() 
  = (0 | it + 1 | /cons(_,_,_,_) := #P.definitions) == 7;

test bool allConstructorsForAnAlternativeDefineTheSameSort() 
  = !(/choice(def, /cons(label(_,def),_,_,_)) !:= #P.definitions);
  
test bool typeParameterReificationIsStatic1(&F _) = #&F.symbol == \parameter("F",\value());
test bool typeParameterReificationIsStatic2(list[&F] _) = #list[&F].symbol == \list(\parameter("F",\value()));

@ignore{issue #1007}
test bool typeParameterReificationIsStatic3(&T <: list[&F] f) = #&T.symbol == \parameter("T", \list(\parameter("F",\value())));

test bool dynamicTypesAreAlwaysGeneric(value v) = !(type[value] _ !:= type(typeOf(v),()));

// New tests which can be enabled after succesful bootstrap
data P(int size = 0);

@ignore{Does not work after changed TypeReifier in compiler}
test bool allConstructorsHaveTheCommonKwParam()
  =  all(/choice(def, /cons(_,_,kws,_)) := #P.definitions, label("size", \int()) in kws);
   
test bool axiomHasItsKwParam()
  =  /cons(label("axiom",_),_,kws,_) := #P.definitions && label("mine", \adt("P",[])) in kws;  

@ignore{Does not work after changed TypeReifier in compiler}  
test bool axiomsKwParamIsExclusive()
  =  all(/cons(label(!"axiom",_),_,kws,_) := #P.definitions, label("mine", \adt("P",[])) notin kws);
  
  
  
