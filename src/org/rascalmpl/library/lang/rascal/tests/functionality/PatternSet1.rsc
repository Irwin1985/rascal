module lang::rascal::tests::functionality::PatternSet1

public int ModVar42 = 42;
public int ModVar44 = 44;
public set[int] ModVarSet_41_42_43 = {41, 42, 43};

test bool matchModuleVar1() = ModVar42 := 42;
test bool matchModuleVar3() = ModVarSet_41_42_43 := ModVarSet_41_42_43;

//  matchSet1
        
test bool matchSet1() = {} := {};
test bool matchSet2() = {1} := {1};
test bool matchSet3() = {1, 2} := {1, 2};
        
test bool matchSet4() = {int _} := {1};
test bool matchSet5() = {int _, int _} := {1, 2};
        
test bool matchSet6() = {_} := {1};
test bool matchSet7() = {_, _} := {1, 2};
test bool matchSet8() = !({_} := {1, 2});
test bool matchSet9() = !({_, _} := {1});
        
test bool matchSet10() = !({_, _} := {1, 2, 3});
  
test bool matchSet11() = !({_, _, _} := {1, 2});
         
test bool matchSet12() = !({} := {1});
test bool matchSet13() = !({1} := {2});
test bool matchSet14() = !({1,2} := {1,3});
  
test bool matchSet15() = {*int X} := {} && X == {};
  
test bool matchSet16() =  {*int X} := {1} && X == {1};
test bool matchSet17() = {*int X} := {1,2} && X == {1,2};
        
test bool matchSet18() = {*Y} := {1,2} && Y == {1,2};
  
// TODO: Test related to + multivariables are commented out since they are not yet supported by the Rascal syntax
        
//  test bool assertTrue() = {Y+} := {1,2} && Y == {1,2};
test bool matchSet19() = {*int _} := {1,2}; 
test bool matchSet20() = {*_} := {1,2}; 
//  test bool matchSet() = {_+} := {1,2}; 
test bool matchSet21() = ({int N, 2, N} := {1,2}) && (N == 1);
        
test bool matchSet22() = !(({int N, 2, N} := {1,2,3}));
bool assertFalse3() = ({int N, 2, N} := {1,2,"a"});
        
test bool matchSet23() {int N = 3; return {N, 2, 1} := {1,2,3};}
test bool matchSet24() {set[int] S = {3}; return {*S, 2, 1} := {1,2,3};}
test bool matchSet25() {set[int] S = {2, 3}; return {*S, 1} := {1,2,3};}
  
test bool matchSet26() = {1, *int X, 2} := {1,2} && X == {};
test bool matchSet27() = {1, *X, 2} := {1,2} && X == {};
test bool matchSet28() = {1, *_, 2} := {1,2};
//  test bool matchSet() = !({ {1, X+, 2} := {1,2});
//  test bool matchSet() = !({ {1, _+, 2} := {1,2};}) _+ does not exist yet
test bool matchSet29() = {1, *X, 2} := {1,2} && X == {};
test bool matchSet30() = !({1, *_, 2} := {1,3});
test bool matchSet31() = !({1, *_, 2} := {1,3});
        
test bool matchSet32() = {1, *int X, 2} := {1,2,3} && X == {3};
test bool matchSet33() = {1, *X, 2} := {1,2,3} && X == {3};
//  test bool matchSet() = {1, X+, 2} := {1,2,3} && X == {3};
test bool matchSet34() = {1, *_, 2} := {1,2,3};
//  test bool matchSet() = {1, _+, 2} := {1,2,3};
        
test bool matchSet35() = {1, *int X, 2} := {1,2,3,4} && X == {3,4};
  
test bool matchSet36() = {*int X, *int Y} := {} && X == {} && Y == {};
test bool matchSet37() = {1, *int X, *int Y} := {1} && X == {} && Y == {};
test bool matchSet38() = {*int X, 1, *int Y} := {1} && X == {} && Y == {};
test bool matchSet39() = {*int X, *int Y, 1} := {1} && X == {} && Y == {};
  
test bool matchSet40() = !({*int _, *int _, 1} := {2});
test bool matchSet41() = !({*_, *_, 1} := {2});
        
test bool matchSet42() = {*int X, *int Y} := {1} && ((X == {} && Y == {1}) || (X == {1} && Y == {}));   /* added parentheses */
test bool matchSet43() = {*X, *Y} := {1} && ((X == {} && Y == {1}) || (X == {1} && Y == {}));           /* added parentheses */
  
test bool matchSet44() = {*int X, *int Y, *int Z} := {} && X == {} && Y == {} && Z == {};
test bool matchSet45() = {*X, *Y, *Z} := {} && X == {} && Y == {} && Z == {};
test bool matchSet46() = {*int X, *int Y, *int Z} := {1} && ((X == {1} && Y == {} && Z == {}) || (X == {} && Y == {1} && Z == {}) || (X == {} && Y == {} && Z == {1})); /* added parentheses */
test bool matchSet47() = {*X, *Y, *Z} := {1} && ((X == {1} && Y == {} && Z == {}) || (X == {} && Y == {1} && Z == {}) || (X == {} && Y == {} && Z == {1}));             /* added parentheses */
  
test bool matchSet48() = {int X, *int Y} := {1} && X == 1 && Y == {};
test bool matchSet49() = {*int X, int Y} := {1} && X == {} && Y == 1;
test bool matchSet50() = {*X, int Y} := {1} && X == {} && Y == 1;
//  test bool matchSet() = !({ {X+, int Y} := {1};})
test bool matchSet51() = {*int _, int _} := {1}; 
test bool matchSet52() = {*_, int _} := {1}; 
test bool matchSet53() =  {*_, _} := {1}; 
//  test bool matchSet() = !({_+, _} := {1});
  
test bool matchSet54() = {*int X, int Y} := {1, 2} && ((X == {1} && Y == 2) || (X == {2} && Y == 1));   /* added parentheses */
test bool matchSet55() = {*X, int Y} := {1, 2} && ((X == {1} && Y == 2) || (X == {2} && Y == 1));       /* added parentheses */
        
test bool matchSet56() = {*int X, int Y} := {1, 2} && ((X == {1} && Y == 2) || (X == {2} && Y == 1));   /* added parentheses */
test bool matchSet57() = {*int X, *real Y} := { 1, 5.5, 2, 6.5} && (X == {1,2} && Y == {5.5, 6.5});
test bool matchSet58() = {*X, *Y} := { 1, 5.5, 2, 6.5} && (X == {1, 5.5, 2, 6.5} && Y == {}
                                                          || X == {} && Y == {1, 5.5, 2, 6.5});
        
test bool matchSet59() {set[int] x = {}; return {} := x;} 

test bool matchSet60(){
    res = {};
    for({*int a, *int b, *int c} := {1,2,3,4,5}) { res = res + {{a,b,c}}; }
    return res == 
        {
          {
            {},
            {5,1,3,2,4}
          },
          {
            {},
            {4},
            {5,1,3,2}
          },
          {
            {},
            {2},
            {5,1,3,4}
          },
          {
            {},
            {5,1,3},
            {2,4}
          },
          {
            {5,1,3},
            {2},
            {4}
          },
          {
            {},
            {5,1,2,4},
            {3}
          },
          {
            {},
            {5,1,2},
            {3,4}
          },
          {
            {3},
            {4},
            {5,1,2}
          },
          {
            {},
            {3,2},
            {5,1,4}
          },
          {
            {3},
            {2},
            {5,1,4}
          },
          {
            {},
            {3,2,4},
            {5,1}
          },
          {
            {5,1},
            {4},
            {3,2}
          },
          {
            {5,1},
            {2},
            {3,4}
          },
          {
            {2,4},
            {5,1},
            {3}
          },
          {
            {},
            {1},
            {5,3,2,4}
          },
          {
            {},
            {1,4},
            {5,3,2}
          },
          {
            {1},
            {4},
            {5,3,2}
          },
          {
            {},
            {1,2},
            {5,3,4}
          },
          {
            {1},
            {2},
            {5,3,4}
          },
          {
            {},
            {1,2,4},
            {5,3}
          },
          {
            {5,3},
            {4},
            {1,2}
          },
          {
            {5,3},
            {2},
            {1,4}
          },
          {
            {1},
            {5,3},
            {2,4}
          },
          {
            {},
            {5,2,4},
            {1,3}
          },
          {
            {5,2,4},
            {1},
            {3}
          },
          {
            {},
            {5,2},
            {1,3,4}
          },
          {
            {1,3},
            {4},
            {5,2}
          },
          {
            {3},
            {5,2},
            {1,4}
          },
          {
            {1},
            {5,2},
            {3,4}
          },
          {
            {},
            {5,4},
            {1,3,2}
          },
          {
            {1,3},
            {2},
            {5,4}
          },
          {
            {3},
            {5,4},
            {1,2}
          },
          {
            {1},
            {5,4},
            {3,2}
          },
          {
            {},
            {5},
            {1,3,2,4}
          },
          {
            {5},
            {4},
            {1,3,2}
          },
          {
            {5},
            {2},
            {1,3,4}
          },
          {
            {5},
            {1,3},
            {2,4}
          },
          {
            {5},
            {1,2,4},
            {3}
          },
          {
            {5},
            {1,2},
            {3,4}
          },
          {
            {5},
            {1,4},
            {3,2}
          },
          {
            {5},
            {1},
            {3,2,4}
          }
        };
}

test bool matchSet61(){
    res = {};
    for({6, *int a, int _, *int b, int _, 2, *int c} := {1,2,3,4,5,6,7}) { res = res + {{a,b,c}}; }
    return res ==
        {
          {
            {},
            {5,7,1}
          },
          {
            {},
            {5,7},
            {1}
          },
          {
            {},
            {7},
            {5,1}
          },
          {
            {},
            {5},
            {7,1}
          },
          {
            {5},
            {7},
            {1}
          },
          {
            {},
            {5,7,3}
          },
          {
            {},
            {5,7},
            {3}
          },
          {
            {},
            {7},
            {5,3}
          },
          {
            {},
            {5},
            {7,3}
          },
          {
            {5},
            {7},
            {3}
          },
          {
            {},
            {5,1,3}
          },
          {
            {},
            {5,1},
            {3}
          },
          {
            {},
            {1},
            {5,3}
          },
          {
            {},
            {5},
            {1,3}
          },
          {
            {5},
            {1},
            {3}
          },
          {
            {},
            {7,1,3}
          },
          {
            {},
            {3},
            {7,1}
          },
          {
            {},
            {1},
            {7,3}
          },
          {
            {},
            {7},
            {1,3}
          },
          {
            {7},
            {1},
            {3}
          },
          {
            {},
            {5,7,4}
          },
          {
            {},
            {5,7},
            {4}
          },
          {
            {},
            {7},
            {5,4}
          },
          {
            {},
            {5},
            {7,4}
          },
          {
            {5},
            {7},
            {4}
          },
          {
            {},
            {5,1,4}
          },
          {
            {},
            {5,1},
            {4}
          },
          {
            {},
            {1},
            {5,4}
          },
          {
            {},
            {5},
            {1,4}
          },
          {
            {5},
            {1},
            {4}
          },
          {
            {},
            {7,1,4}
          },
          {
            {},
            {7,1},
            {4}
          },
          {
            {},
            {1},
            {7,4}
          },
          {
            {},
            {7},
            {1,4}
          },
          {
            {7},
            {1},
            {4}
          },
          {
            {},
            {5,3,4}
          },
          {
            {},
            {5,3},
            {4}
          },
          {
            {},
            {3},
            {5,4}
          },
          {
            {},
            {5},
            {3,4}
          },
          {
            {5},
            {3},
            {4}
          },
          {
            {},
            {7,3,4}
          },
          {
            {},
            {7,3},
            {4}
          },
          {
            {},
            {3},
            {7,4}
          },
          {
            {},
            {7},
            {3,4}
          },
          {
            {7},
            {3},
            {4}
          },
          {
            {},
            {1,3,4}
          },
          {
            {},
            {1,3},
            {4}
          },
          {
            {},
            {3},
            {1,4}
          },
          {
            {},
            {1},
            {3,4}
          },
          {
            {1},
            {3},
            {4}
          }
        };
}

test bool matchSetModuleVar1() = {ModVar42} := {42};
test bool matchSetModuleVar2() = {*ModVarSet_41_42_43} := ModVarSet_41_42_43;
test bool matchSetModuleVar3() = {ModVar44, *ModVarSet_41_42_43} := {ModVar44, *ModVarSet_41_42_43};
@ignoreInterpreter{Seems to be a bug in the interpreter}
test bool matchSetModuleVar4() = {ModVar44, ModVarSet_41_42_43} := {ModVar44, ModVarSet_41_42_43};


// matchNestedSet

test bool matchNestedSet1() = !({} := {{2}});
  
test bool matchNestedSet3() = {} := {};
test bool matchNestedSet4() = {{1}} := {{1}};
test bool matchNestedSet5() = {{1,2}} := {{1,2}};
          
test bool matchNestedSet6() = !({{1}} := {{2}});
test bool matchNestedSet7() = !({{1,2}} := {{1,2,3}});
          
test bool matchNestedSet8() = {*set[int] _} := {};
          
test bool matchNestedSet9() = {*set[int] _} := {{1}};
test bool matchNestedSet10() = {*set[int] _} := {{1,2}};
          
test bool matchNestedSet11() = ({{1}, *set[int] L, {6,7,8}} := {{1},{2,3},{4,5},{6,7,8}}) && (L == {{2,3},{4,5}});
test bool matchNestedSet12() = !(({{1}, *set[int] L, {6,7,8}} := {{1},{2,3},{4,5},{8}}) && (L == {{2,3},{4,5}}));


 /*TODO: fails*/  
 @Ignore  
test bool matchNestedSet13() = ({{1}, *set[int] L, {6,7,8}, *L} := {{1},{2,3},{4,5},{6,7,8},{2,3},{4,5}}) && (L == {{2,3},{4,5}});

//    matchSetMultiVars

test bool matchSetMultiVars1() = {1, *S, 4, 5}:= {1, 2, 3, 4, 5} && S == {2, 3};
test bool matchSetMultiVars2() = {1, *_, 4, 5} := {1, 2, 3, 4, 5};
      
//    matchSetSpliceVars

test bool matchSetSpliceVars1() = {1, *S, 4, 5}:= {1, 2, 3, 4, 5} && S == {2, 3};
test bool matchSetSpliceVars2() = {1, * int S, 4, 5}:= {1, 2, 3, 4, 5} && S == {2, 3};
test bool matchSetSpliceVars3() = {1, *_, 4, 5} := {1, 2, 3, 4, 5};
test bool matchSetSpliceVars4() = {1, * int _, 4, 5} := {1, 2, 3, 4, 5};

// matchListSetVariableScopes

 data PAIR = a1() | b1() | c1() | d1() | pair(PAIR q1, PAIR q2) | s1(set[PAIR] S) | l1(list[PAIR] L);
        
test bool matchListSetVariableScopes1() = {PAIR D, pair(D, b1())} := {pair(a1(),b1()), a1()} && D == a1();
test bool matchListSetVariableScopes2() = {PAIR D, pair(D, b1())} !:= {pair(a1(),b1()), c1()};
        
test bool matchListSetVariableScopes3() = {pair(PAIR D, b1()), D} := {pair(a1(),b1()), a1()} && D == a1();
test bool matchListSetVariableScopes4() = {pair(PAIR D, b1()), D} !:= {pair(a1(),b1()), c1()};
        
test bool matchListSetVariableScopes5() = {pair(s1(set[PAIR] S1), c1()), *S1} :=  {pair(s1({a1(), b1()}), c1()), a1(), b1()} && S1 == {a1(), b1()};
test bool matchListSetVariableScopes6() = {pair(s1(set[PAIR] S1), c1()), *S1} !:= {pair(s1({a1(), b1()}), c1()), a1(), d1()};
        
test bool matchListSetVariableScopes7() {list[PAIR] L1 = [a1(), b1()]; return [*L1, c1()] := [a1(), b1(), c1()];}
test bool matchListSetVariableScopes8() {list[PAIR] L1 = [a1(), b1()]; return [*L1, c1()] !:= [a1(), d1(), c1()];}
        
test bool matchListSetVariableScopes9() = [pair(l1(list[PAIR] L1), c1()), *L1] := [pair(l1([a1(), b1()]), c1()), a1(), b1()];
test bool matchListSetVariableScopes10() = [pair(l1(list[PAIR] L1), c1()), *L1] !:= [pair(l1([a1(), b1()]), c1()), a1(), d1()];
        
test bool matchListSetVariableScopes11() = [pair(PAIR L1, b1()), L1] := [pair(a1(), b1()), a1()];
test bool matchListSetVariableScopes12() = [pair(PAIR L1, b1()), L1] !:= [pair(a1(), b1()), d1()];
  
// matchSetExternalVar
  
test bool matchSetExternalVar1() {set[int] S; return ({1, *S, 2} := {1,2,3} && S == {3});}