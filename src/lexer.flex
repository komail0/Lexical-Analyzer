%%
%class Lexer
%public
%unicode
%type void

%%

"if"        { System.out.println("IF"); }
"else"      { System.out.println("ELSE"); }
[0-9]+      { System.out.println("NUMBER"); }
[a-zA-Z_][a-zA-Z0-9_]* { System.out.println("ID"); }
[ \t\n\r]+  { /* ignore whitespace */ }
.           { System.out.println("UNKNOWN"); }
