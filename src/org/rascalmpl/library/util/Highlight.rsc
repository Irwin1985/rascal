module util::Highlight

import ParseTree;
import String;

// A comment

public map[str, str] htmlEscapes = (
	"\<": "&lt;",
	"\>": "&gt;",
	"&" : "&amp;"
);

str highlight2html(appl(prod(lit(str l), _, _), _)) = span("keyword", l)
  when /^[a-zA-Z0-9_\-]*$/ := l;

str highlight2html(appl(prod(_, _, {_*, \tag("category"(str cat))}), list[Tree] as))
  = span(cat, ( "" | it + highlight2html(a) | a <- as ));

str highlight2html(appl(prod(_, _, set[Attr] attrs), list[Tree] as))
  = ( "" | it + highlight2html(a) | a <- as )
  when {_*, \tag("category"(str _))} !:= attrs;

str highlight2html(appl(regular(_), list[Tree] as))
  = ( "" | it + highlight2html(a) | a <- as );

str highlight2html(amb({k, _*})) = highlight2html(k);

default str highlight2html(Tree t) = escape(unparse(t), htmlEscapes);

str span(str class, str src) = "\<span class=\"<class>\"\><src>\</span\>";

// Latex

public map[str, str] texEscapes = (
	"\\": "\\textbackslash{}",
	"\<": "\\textless{};",
	"\>": "\\textgreater{};",
	"%": "\\%{};",
	"&" : "\\&{}",
	"_" : "\\_{}",
	"^" : "\\^{}",
	"{" : "\\{{}",
	"}" : "\\}{}",
	"$" : "\\${}"
);

str highlight2latex(appl(prod(lit(str l), _, _), _)) = catCmd("keyword", l)
  when /^[a-zA-Z0-9_\-]*$/ := l;

str highlight2latex(appl(prod(_, _, {_*, \tag("category"(str cat))}), list[Tree] as))
  = catCmd(cat, ( "" | it + highlight2latex(a) | a <- as ));

str highlight2latex(appl(prod(_, _, set[Attr] attrs), list[Tree] as))
  = ( "" | it + highlight2latex(a) | a <- as )
  when {_*, \tag("category"(str _))} !:= attrs;

str highlight2latex(appl(regular(_), list[Tree] as))
  = ( "" | it + highlight2latex(a) | a <- as );

str highlight2latex(amb({k, _*})) = highlight2latex(k);

default str highlight2latex(Tree t) = escape(unparse(t), texEscapes);

str catCmd(str class, str src) = "\\CAT{<class>}{<src>}";
