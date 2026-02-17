/* ================================================================
   SECTION 1: USER CODE
   ================================================================ */
import java.io.*;
import java.util.*;

%%

/* ================================================================
   SECTION 2: OPTIONS & DECLARATIONS
   ================================================================ */
%class   Yylex
%type    Token
%unicode
%line
%column
%public

%{
    private int         whitespaceRemoved = 0;
    private SymbolTable symbolTable       = new SymbolTable();

    private Token token(TokenType type) {
        return new Token(type, yytext(), yyline + 1, yycolumn + 1);
    }

    public int         getWhitespaceRemoved() { return whitespaceRemoved; }
    public SymbolTable getSymbolTable()        { return symbolTable;       }
%}

/* --- Macros ---------------------------------------------------- */
DIGIT        = [0-9]
LETTER_UPPER = [A-Z]
LETTER_LOWER = [a-z]
SIGN         = [+\-]
UNDERSCORE   = _
WHITESPACE   = [ \t\r\n]+

/* Identifier: MUST start uppercase (Section 3.2) */
IDENTIFIER   = {LETTER_UPPER}({LETTER_LOWER}|{DIGIT}|{UNDERSCORE}){0,30}

/* Numeric */
INTEGER      = {SIGN}?{DIGIT}+
FLOAT        = {SIGN}?{DIGIT}+\.{DIGIT}{1,6}([eE]{SIGN}?{DIGIT}+)?

/* String & Character */
STRING       = \"([^\"\\\n]|\\[\"\\ntr])*\"
CHARACTER    = \'([^\'\\\n]|\\[\'\\ntr])\'

/* Comments */
SINGLE_CMT   = "##"[^\n]*
MULTI_CMT    = "#*"([^*]|"*"+[^*#])*"*"+"#"

%%

/* ================================================================
   SECTION 3: RULES
   ================================================================ */

/* 1. Comments */
{MULTI_CMT}       { return token(TokenType.MULTI_LINE_COMMENT);    }
{SINGLE_CMT}      { return token(TokenType.SINGLE_LINE_COMMENT);   }

/* 2. Multi-character operators (longest match - before single char) */
"**"              { return token(TokenType.ARITHMETIC_OP);          }
"=="              { return token(TokenType.RELATIONAL_OP);          }
"!="              { return token(TokenType.RELATIONAL_OP);          }
"<="              { return token(TokenType.RELATIONAL_OP);          }
">="              { return token(TokenType.RELATIONAL_OP);          }
"&&"              { return token(TokenType.LOGICAL_OP);             }
"||"              { return token(TokenType.LOGICAL_OP);             }
"++"              { return token(TokenType.INCREMENT_OP);           }
"--"              { return token(TokenType.DECREMENT_OP);           }
"+="              { return token(TokenType.ASSIGNMENT_OP);          }
"-="              { return token(TokenType.ASSIGNMENT_OP);          }
"*="              { return token(TokenType.ASSIGNMENT_OP);          }
"/="              { return token(TokenType.ASSIGNMENT_OP);          }

/* 3. Keywords (explicit rules - fixes the ERROR bug)
      Must come BEFORE the IDENTIFIER rule               */
"start"           { return token(TokenType.KEYWORD); }
"finish"          { return token(TokenType.KEYWORD); }
"loop"            { return token(TokenType.KEYWORD); }
"condition"       { return token(TokenType.KEYWORD); }
"declare"         { return token(TokenType.KEYWORD); }
"output"          { return token(TokenType.KEYWORD); }
"input"           { return token(TokenType.KEYWORD); }
"function"        { return token(TokenType.KEYWORD); }
"return"          { return token(TokenType.KEYWORD); }
"break"           { return token(TokenType.KEYWORD); }
"continue"        { return token(TokenType.KEYWORD); }
"else"            { return token(TokenType.KEYWORD); }

/* 4. Boolean literals (before identifier) */
"true"            { return token(TokenType.BOOLEAN_LITERAL);        }
"false"           { return token(TokenType.BOOLEAN_LITERAL);        }

/* 5. Identifiers (uppercase start only - Section 3.2) */
{IDENTIFIER}      {
                    symbolTable.insert(yytext(), yyline + 1, yycolumn + 1);
                    return token(TokenType.IDENTIFIER);
                  }

/* 6. Floating-point (before integer - longest match) */
{FLOAT}           { return token(TokenType.FLOATING_POINT_LITERAL); }

/* 7. Integer */
{INTEGER}         { return token(TokenType.INTEGER_LITERAL);        }

/* 8. String literal */
{STRING}          { return token(TokenType.STRING_LITERAL);         }

/* 9. Character literal */
{CHARACTER}       { return token(TokenType.CHARACTER_LITERAL);      }

/* 10. Single-character operators */
"+"               { return token(TokenType.ARITHMETIC_OP);          }
"-"               { return token(TokenType.ARITHMETIC_OP);          }
"*"               { return token(TokenType.ARITHMETIC_OP);          }
"/"               { return token(TokenType.ARITHMETIC_OP);          }
"%"               { return token(TokenType.ARITHMETIC_OP);          }
"<"               { return token(TokenType.RELATIONAL_OP);          }
">"               { return token(TokenType.RELATIONAL_OP);          }
"!"               { return token(TokenType.LOGICAL_OP);             }
"="               { return token(TokenType.ASSIGNMENT_OP);          }

/* 11. Punctuators */
"("               { return token(TokenType.PUNCTUATOR);             }
")"               { return token(TokenType.PUNCTUATOR);             }
"{"               { return token(TokenType.PUNCTUATOR);             }
"}"               { return token(TokenType.PUNCTUATOR);             }
"["               { return token(TokenType.PUNCTUATOR);             }
"]"               { return token(TokenType.PUNCTUATOR);             }
","               { return token(TokenType.PUNCTUATOR);             }
";"               { return token(TokenType.PUNCTUATOR);             }
":"               { return token(TokenType.PUNCTUATOR);             }

/* 12. Whitespace */
{WHITESPACE}      {
                    whitespaceRemoved += yytext().length();
                    return token(TokenType.WHITESPACE);
                  }

/* 13. Anything else is an error */
[^]               { return token(TokenType.ERROR);                  }
