@ignoreCompiler{Not yet implemented in type checker}
module lang::rascal::tests::functionality::KeywordParameterImportTests2::Import2

data L(str e = "e", str f = e + e) = n(str g = f + f);

data L(str h = "") = p();

L createN2() = n();
L createP2() = p();