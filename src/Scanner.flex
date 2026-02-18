
import java.io.*;
import java.util.*;

%%

%class   Yylex
%type    Token
%unicode
%line
%column
%public

%{
    private int          whitespaceRemoved = 0;
    private int          linesProcessed    = 0;
    private SymbolTable  symbolTable       = new SymbolTable();
    private ErrorHandler errorHandler      = new ErrorHandler();  // Part 3

    private Token token(TokenType type) {
        // Track highest line number seen so far
        if (yyline + 1 > linesProcessed) linesProcessed = yyline + 1;
        return new Token(type, yytext(), yyline + 1, yycolumn + 1);
    }

    public int          getWhitespaceRemoved() { return whitespaceRemoved; }
    public int          getLinesProcessed()     { return linesProcessed;    }
    public SymbolTable  getSymbolTable()        { return symbolTable;       }
    public ErrorHandler getErrorHandler()       { return errorHandler;      }  // Part 3
%}



/* 3.1 Keywords: (start|finish|loop|condition|declare|output|input|function|return|break|continue|else) */
KEYWORD = (start|finish|loop|condition|declare|output|input|function|return|break|continue|else)

/* 3.7 Boolean Literals: (true|false) */
BOOLEAN = (true|false)

/* 3.2 Identifiers: [A-Z][a-z0-9_]{0,30} */
IDENTIFIER = [A-Z][a-z0-9_]{0,30}

/* 3.3 Integer Literals: [+-]?[0-9]+ */
INTEGER = [+\-]?[0-9]+

/* 3.4 Floating-Point Literals: [+-]?[0-9]+\.[0-9]{1,6}([eE][+-]?[0-9]+)? */
FLOAT = [+\-]?[0-9]+\.[0-9]{1,6}([eE][+\-]?[0-9]+)?

/* 3.5 String Literals: "([ ^"\\\n]|\\["\\ntr])*"
   In JFlex: [^\"\\\n] means "not quote, backslash, or newline" */
STRING = \"([^\"\\\n]|\\[\"\\ntr])*\"

/* 3.6 Character Literals: '([ ^'\\\n]|\\['\\ntr])' */
CHARACTER = '([^'\\\n]|\\['\\ntr])'

/* 3.10 Comments - Single-line: ##[ ^\n]* */
SINGLE_COMMENT = "##"[^\n]*

/* 3.10 Comments - Multi-line: #\*([ ^*]|\*+[ ^*#])*\*+# */
MULTI_COMMENT = "#*"([^*]|\*+[^*#])*"*"+"#"

/* 3.11 Whitespace: [ \t\r\n]+ */
WHITESPACE = [ \t\r\n]+

/* ERROR DETECTION PATTERNS (Part 3)*/

/* Invalid identifier - starts with lowercase */
INVALID_ID_LOWER = [a-z][a-zA-Z0-9_]*

/* Invalid identifier - starts with underscore */
INVALID_ID_UNDER = _[a-zA-Z0-9_]*

/* Identifier too long (>31 chars) */
IDENTIFIER_LONG = [A-Z][a-z0-9_]{31}[a-z0-9_]+

/* Malformed float - more than 6 fractional digits */
FLOAT_MALFORMED = [+\-]?[0-9]+\.[0-9]{7,50}

/* Unterminated string - no closing quote before newline */
STRING_UNTERM = \"([^\"\\\n]|\\[\"\\ntr])*[\n]

/* Unterminated character - greedily consumes everything up to newline/EOF.
   The optional trailing ' also handles wrong-length literals like 'AB' as one error token */
CHAR_UNTERM = '([^'\\\n]|\\['\\ntr])*\'?

/* Unclosed multi-line comment - reaches EOF without *# */
MULTI_UNCLOSED = "#*"([^*]|\*+[^*#])*

%%




  // 1. COMMENTS (highest priority)


{MULTI_COMMENT}       { return token(TokenType.MULTI_LINE_COMMENT);    }
{SINGLE_COMMENT}      { return token(TokenType.SINGLE_LINE_COMMENT);   }

/* ERROR: Unclosed multi-line comment */
{MULTI_UNCLOSED}      {
                        errorHandler.detectUnclosedComment(yytext(), yyline+1, yycolumn+1);
                        return token(TokenType.ERROR);
                      }


  // 2. MULTI-CHARACTER OPERATORS (before single-char operators)


/* Arithmetic: ** */
"**"                  { return token(TokenType.ARITHMETIC_OP);          }

/* Relational: ==, !=, <=, >= */
"=="                  { return token(TokenType.RELATIONAL_OP);          }
"!="                  { return token(TokenType.RELATIONAL_OP);          }
"<="                  { return token(TokenType.RELATIONAL_OP);          }
">="                  { return token(TokenType.RELATIONAL_OP);          }

/* Logical: &&, || */
"&&"                  { return token(TokenType.LOGICAL_OP);             }
"||"                  { return token(TokenType.LOGICAL_OP);             }

/* Assignment: +=, -=, *=, /= */
"+="                  { return token(TokenType.ASSIGNMENT_OP);          }
"-="                  { return token(TokenType.ASSIGNMENT_OP);          }
"*="                  { return token(TokenType.ASSIGNMENT_OP);          }
"/="                  { return token(TokenType.ASSIGNMENT_OP);          }

/* Increment/Decrement: ++, -- */
"++"                  { return token(TokenType.INCREMENT_OP);           }
"--"                  { return token(TokenType.DECREMENT_OP);           }

//3. KEYWORDS (must come BEFORE identifiers)

"start"               { return token(TokenType.KEYWORD);                }
"finish"              { return token(TokenType.KEYWORD);                }
"loop"                { return token(TokenType.KEYWORD);                }
"condition"           { return token(TokenType.KEYWORD);                }
"declare"             { return token(TokenType.KEYWORD);                }
"output"              { return token(TokenType.KEYWORD);                }
"input"               { return token(TokenType.KEYWORD);                }
"function"            { return token(TokenType.KEYWORD);                }
"return"              { return token(TokenType.KEYWORD);                }
"break"               { return token(TokenType.KEYWORD);                }
"continue"            { return token(TokenType.KEYWORD);                }
"else"                { return token(TokenType.KEYWORD);                }

//4. BOOLEAN LITERALS (before identifiers)

"true"                { return token(TokenType.BOOLEAN_LITERAL);        }
"false"               { return token(TokenType.BOOLEAN_LITERAL);        }

//NUMERIC LITERALS

/* ERROR: Malformed float - too many fractional digits */
{FLOAT_MALFORMED}     {
                        errorHandler.detectMalformedFloat(yytext(), yyline+1, yycolumn+1);
                        int dotIndex = yytext().indexOf('.');
                        String valid = yytext().substring(0, dotIndex + 7);
                        return new Token(TokenType.FLOATING_POINT_LITERAL, valid, yyline+1, yycolumn+1);
                      }

/* Valid float: [+-]?[0-9]+\.[0-9]{1,6}([eE][+-]?[0-9]+)? */
{FLOAT}               { return token(TokenType.FLOATING_POINT_LITERAL); }

/* Valid integer: [+-]?[0-9]+ */
{INTEGER}             { return token(TokenType.INTEGER_LITERAL);        }

//6. IDENTIFIERS


// ERROR: Identifier too long (>31 chars)
{IDENTIFIER_LONG}     {
                        errorHandler.detectIdentifierTooLong(yytext(), yyline+1, yycolumn+1);
                        String truncated = yytext().substring(0, 31);
                        symbolTable.insert(truncated, yyline+1, yycolumn+1);
                        return new Token(TokenType.IDENTIFIER, truncated, yyline+1, yycolumn+1);
                      }

// ERROR: Invalid identifier - starts with lowercase
{INVALID_ID_LOWER}    {
                        errorHandler.detectInvalidIdentifier(yytext(), yyline+1, yycolumn+1);
                        return token(TokenType.ERROR);
                      }

// ERROR: Invalid identifier - starts with underscore
{INVALID_ID_UNDER}    {
                        errorHandler.detectInvalidIdentifier(yytext(), yyline+1, yycolumn+1);
                        return token(TokenType.ERROR);
                      }

// Valid identifier: [A-Z][a-z0-9_]{0,30}
{IDENTIFIER}          {
                        symbolTable.insert(yytext(), yyline+1, yycolumn+1);
                        return token(TokenType.IDENTIFIER);
                      }

//7. STRING LITERALS

/* ERROR: Unterminated string */
{STRING_UNTERM}       {
                        errorHandler.detectUnterminatedString(yytext(), yyline+1, yycolumn+1);
                        return token(TokenType.ERROR);
                      }

/* Valid string: "([ ^"\\\n]|\\["\\ntr])*" */
{STRING}              { return token(TokenType.STRING_LITERAL);         }

//8. CHARACTER LITERALS

/* Valid character: '([ ^'\\\n]|\\['\\ntr])' */
{CHARACTER}           { return token(TokenType.CHARACTER_LITERAL);      }

/* ERROR: Unterminated/invalid character literal */
{CHAR_UNTERM}         {
                        errorHandler.detectUnterminatedChar(yytext(), yyline+1, yycolumn+1);
                        return token(TokenType.ERROR);
                      }

//9. SINGLE-CHARACTER OPERATORS

/* Arithmetic: +, -, *, /, % */
"+"                   { return token(TokenType.ARITHMETIC_OP);          }
"-"                   { return token(TokenType.ARITHMETIC_OP);          }
"*"                   { return token(TokenType.ARITHMETIC_OP);          }
"/"                   { return token(TokenType.ARITHMETIC_OP);          }
"%"                   { return token(TokenType.ARITHMETIC_OP);          }

/* Relational: <, > */
"<"                   { return token(TokenType.RELATIONAL_OP);          }
">"                   { return token(TokenType.RELATIONAL_OP);          }

/* Logical: ! */
"!"                   { return token(TokenType.LOGICAL_OP);             }

/* Assignment: = */
"="                   { return token(TokenType.ASSIGNMENT_OP);          }

// 10. PUNCTUATORS: ( ) { } [ ] , ; :

"("                   { return token(TokenType.PUNCTUATOR);             }
")"                   { return token(TokenType.PUNCTUATOR);             }
"{"                   { return token(TokenType.PUNCTUATOR);             }
"}"                   { return token(TokenType.PUNCTUATOR);             }
"["                   { return token(TokenType.PUNCTUATOR);             }
"]"                   { return token(TokenType.PUNCTUATOR);             }
","                   { return token(TokenType.PUNCTUATOR);             }
";"                   { return token(TokenType.PUNCTUATOR);             }
":"                   { return token(TokenType.PUNCTUATOR);             }

//11. WHITESPACE: [ \t\r\n]+

{WHITESPACE}          {
                        whitespaceRemoved += yytext().length();
                        if (yyline + 1 > linesProcessed) linesProcessed = yyline + 1;
                        return token(TokenType.WHITESPACE);
                      }

//12. ERROR: Invalid character (catch-all)

[^]                   {
                        errorHandler.detectInvalidChar(yytext().charAt(0), yyline+1, yycolumn+1);
                        return token(TokenType.ERROR);
                      }
