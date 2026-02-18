# KW Language - Lexical Analyzer
## CS4031 Compiler Construction | Assignment 01

---

## Team Members

| Name | Roll Number |
|------|-------------|
| Komail Raza | 23I-0717 |
| M. Waqas | 23I-0569 |

---

## Language Name and File Extension

- Language Name: KW (KeyWord Language)
- File Extension: `.kw`

---

## Keywords

All 12 keywords are case-sensitive and must be written exactly as shown.

| Keyword | Meaning |
|---------|---------|
| start | Marks the beginning of the program |
| finish | Marks the end of the program |
| declare | Declares and initializes a variable |
| loop | Starts a loop; condition is checked before each iteration |
| condition | Conditional block; executes the body if the condition is true |
| else | Alternate branch executed when the condition is false |
| function | Defines a named function |
| return | Returns a value from a function |
| input | Reads a value from the user |
| output | Prints a value to the screen |
| break | Exits the nearest enclosing loop immediately |
| continue | Skips to the next iteration of the nearest loop |

---

## Identifier Rules

An identifier must:
- Start with exactly one uppercase letter A-Z
- Be followed by lowercase letters, digits, or underscores only
- Be at most 31 characters total including the first letter

Regex: `[A-Z][a-z0-9_]{0,30}`

### Valid Identifiers

```
Count
X
Total_sum
My_value2
Result2024
Pi
```

### Invalid Identifiers

```
count          starts with lowercase letter
Variable       second character is uppercase, not allowed after first
2Count         starts with a digit
myVariable     starts with lowercase letter
_wrong         starts with underscore
```

---

## Literal Formats

### Integer Literals

Regex: `[+-]?[0-9]+`

```
42
+100
-567
0
```

Invalid:
```
12.34     has a decimal point, treated as float
1,000     comma is not allowed
```

### Floating-Point Literals

Regex: `[+-]?[0-9]+\.[0-9]{1,6}([eE][+-]?[0-9]+)?`

Must have 1 to 6 digits after the decimal point. Scientific notation is optional.

```
3.14
+2.5
-0.123456
1.5e10
2.0E-3
```

Invalid:
```
3.          no digits after decimal point
.14         no digits before decimal point
1.2345678   more than 6 decimal digits, malformed
```

### String Literals

Regex: `"([^"\\\n]|\\["\\ntr])*"`

Enclosed in double quotes. Supported escape sequences: `\"`, `\\`, `\n`, `\t`, `\r`

```
"Hello World"
"Say \"Hello\" to everyone"
"Line1\nLine2"
"Col1\tCol2\tCol3"
"Back\\slash"
```

### Character Literals

Regex: `'([^'\\\n]|\\['\\ntr])'`

Exactly one character enclosed in single quotes. Supported escape sequences: `\'`, `\\`, `\n`, `\t`, `\r`

```
'A'
'z'
'9'
'\''
'\\'
'\n'
'\t'
'\r'
```

### Boolean Literals

Regex: `(true|false)` — case-sensitive, lowercase only

```
true
false
```

---

## Operators

The scanner recognises six operator token types. The table below lists every operator
exactly as defined in `Scanner.flex`, grouped by token type and ordered highest to lowest
precedence.

### Full Operator List by Token Type

| Token Type | Operator(s) | Regex from spec |
|------------|-------------|-----------------|
| INCREMENT_OP | ++ | `(\+\+)` |
| DECREMENT_OP | -- | `(--)` |
| ARITHMETIC_OP | ** + - * / % | `(\*\*\|[+\-*/%])` |
| RELATIONAL_OP | == != <= >= < > | `(==\|!=\|<=\|>=\|<\|>)` |
| LOGICAL_OP | && \|\| ! | `(&&\|\|\|\|\|!)` |
| ASSIGNMENT_OP | = += -= *= /= | `(\+=\|-=\|\*=\|/=\|=)` |

### Precedence Table (1 = highest, 10 = lowest)

| Precedence | Operator(s) | Token Type | Description |
|:----------:|-------------|------------|-------------|
| 1 | ++ | INCREMENT_OP | Increment |
| 1 | -- | DECREMENT_OP | Decrement |
| 2 | ** | ARITHMETIC_OP | Exponentiation |
| 3 | * / % | ARITHMETIC_OP | Multiply, Divide, Modulo |
| 4 | + - | ARITHMETIC_OP | Addition, Subtraction |
| 5 | < > <= >= | RELATIONAL_OP | Less/greater than comparisons |
| 6 | == != | RELATIONAL_OP | Equal to, Not equal to |
| 7 | ! | LOGICAL_OP | Logical NOT |
| 8 | && | LOGICAL_OP | Logical AND |
| 9 | \|\| | LOGICAL_OP | Logical OR |
| 10 | = | ASSIGNMENT_OP | Simple assignment |
| 10 | += -= *= /= | ASSIGNMENT_OP | Compound assignment |

Note: `++` and `--` are matched before `+` and `-` in the scanner because multi-character
operators are checked first. Similarly `**` is checked before `*`, and `==` before `=`.

---

## Comment Syntax

### Single-Line Comments

Start with `##`. The scanner ignores everything after `##` on the same line.

```
## This is a single-line comment
declare Count = 0  ## inline comment after code
```

### Multi-Line Comments

Enclosed between `#*` and `*#`. Can span any number of lines. Everything inside is ignored.

```
#*
   This is a
   multi-line comment
*#
```

---

## Sample Programs

### Program 1 - Sum of integers from 1 to 10

```
start
    declare Sum = 0
    declare I = 1

    loop I <= 10
        Sum += I
        I++

    output Sum
finish
```

Calculates the sum of all integers from 1 to 10 and prints the result.

---

### Program 2 - Grade checker using condition and else

```
start
    declare Score = 0
    input Score

    condition Score >= 90
        output "Grade: A"
    condition Score >= 80
        output "Grade: B"
    condition Score >= 70
        output "Grade: C"
    else
        output "Grade: F"
finish
```

Reads a numeric score from the user and prints the corresponding letter grade.

---

### Program 3 - Compute power using a function and loop

```
## Computes X raised to the power N
start
    declare X = 2
    declare N = 8
    declare Ans = 1
    declare Exp = 8

    function Compute_power
        loop Exp > 0
            Ans = Ans * X
            Exp--
        return Ans

    output Ans
finish
```

Defines a function that computes X raised to the power N using repeated multiplication, then outputs the result.

---

### Program 4 - Strings, characters, booleans, and escape sequences

```
#*
   Demonstrates string and character literals,
   boolean variables, and conditional output
*#
start
    declare Greeting = "Hello, World!"
    declare Initial = 'K'
    declare Is_valid = true
    declare Pi = 3.14159

    output Greeting
    output Initial
    output Is_valid
    output Pi

    condition Is_valid == true
        output "Status: active"
    else
        output "Status: inactive"
finish
```

Shows how different literal types are declared, printed, and used in a condition block.

---

## Project Structure

```
23i-0717_23i-0569_C/
|
+-- docs/
|   |
|   +-- Automata_Design.pdf
|   +-- Comparison.pdf
|   +-- DFA.drawio.xml
|   +-- MINI_DFA.drawio.xml
|   +-- NFA.xml
|   +-- NFA_CONJOINED.xml
+-- src/
|   |
|   +-- ErrorHandler.java
|   +-- JFlexMain.java
|   +-- Main.java
|   +-- ManualScanner.java
|   +-- Scanner.flex
|   +-- SymbolTable.java
|   +-- Token.java
|   +-- TokenType.java
|   +-- Yylex.java
|
+-- tests/
|   |
|   +-- test1.kw
|   +-- test2.kw
|   +-- test3.kw
|   +-- test4.kw
|   +-- test5.kw
+-- .gitignore
+-- 23i-0717_23i-0569_C.iml
```

---

## Compilation and Execution Instructions

### IDE: IntelliJ IDEA

The project was developed and tested using IntelliJ IDEA. The `src` folder is set as the source root. The `tests` folder sits at the project root level so both scanners can locate test files at runtime.

---

### Run Configurations

Three run configurations are set up in IntelliJ IDEA. Select the desired one from the dropdown at the top right of the IDE window and click the Run button.

**JFlexMain**

Runs the JFlex-generated scanner.

- Type: Application
- Main class: JFlexMain
- Working directory: project root folder

Requires `Yylex.java` to exist. Run LEX RUNNER first if it does not.

---

**MANUAL SCANNER**

Runs the hand-coded DFA scanner.

- Type: Application
- Main class: ManualScanner
- Working directory: project root folder

---

**LEX RUNNER**

Runs the JFlex tool on `Scanner.flex` to generate `Yylex.java`.

- Type: Shell Script or Application depending on how JFlex is installed
- Program or main class: path to the JFlex executable (e.g. jflex.jar or jflex binary)
- Arguments: `src/Scanner.flex`
- Working directory: project root folder

This only needs to be run once, or again any time `Scanner.flex` is modified.

---

### Setting Up Run Configurations from Scratch

1. Open IntelliJ IDEA and load the project
2. Go to Run > Edit Configurations
3. Click the + button at the top left of the dialog
4. Choose Application
5. Set the Name to MANUAL SCANNER
6. Set Main class to ManualScanner
7. Set Working directory to the project root (the folder containing `src` and `tests`)
8. Click OK
9. Repeat steps 3 to 8 for JFlexMain using Name JFlexMain and Main class JFlexMain
10. For LEX RUNNER, add a Shell Script configuration pointing to JFlex with `src/Scanner.flex` as the argument

---

### Running via Terminal (without IntelliJ)

#### Step 1: Generate Yylex.java from Scanner.flex

```
jflex src/Scanner.flex
```

Download JFlex from https://jflex.de if not installed.

#### Step 2: Compile all source files

```
javac -d out src/*.java
```

#### Step 3: Run the Manual Scanner

```
java -cp out ManualScanner
```

#### Step 4: Run the JFlex Scanner

```
java -cp out JFlexMain
```

Both programs will list the available `.kw` files and prompt for a selection. Enter 0 to exit.

---

### Expected Output Format

Each token is printed on its own line:

```
<TOKEN_TYPE, "lexeme", Line: X, Col: Y>
```

Example:

```
<KEYWORD, "start", Line: 1, Col: 1>
<KEYWORD, "declare", Line: 2, Col: 5>
<IDENTIFIER, "Count", Line: 2, Col: 13>
<ASSIGNMENT_OP, "=", Line: 2, Col: 19>
<INTEGER_LITERAL, "42", Line: 2, Col: 21>
```

After all tokens, the scanner prints:

- Pre-processing: total tokens, whitespace removed, lines processed
- Symbol table: identifier name, type, first line, first column, frequency count
- Error report: each error grouped by type with line, column, lexeme, and reason
- Error summary: count per error type and overall total

---

## Error Handling

The scanner detects all of the following errors and recovers without stopping.

| Error Type | Example | Recovery |
|------------|---------|----------|
| INVALID_CHARACTER | @ or $ | Skip character, emit ERROR token, continue |
| MALFORMED_FLOAT | 3.1234567 (more than 6 decimal digits) | Consume all digits, report error, emit truncated float token |
| UNTERMINATED_STRING | "Hello with no closing quote | Consume including the newline, emit ERROR token, continue |
| UNTERMINATED_CHAR | 'AB with wrong length or no closing quote | Consume to newline or closing quote, emit ERROR token |
| INVALID_IDENTIFIER | count or _wrong or 2name | Consume the full word, emit ERROR token, continue |
| IDENTIFIER_TOO_LONG | identifier over 31 characters | Truncate to 31 characters, emit IDENTIFIER token, continue |
| UNCLOSED_COMMENT | #* with no closing | Consume to end of file, emit ERROR token |

All errors are collected during scanning and printed together at the end, grouped by error type.

---

## Test Files

| File | Contents |
|------|----------|
| test1.kw | All valid token types: keywords, all literal types, loop, booleans, operators |
| test2.kw | Arithmetic and logical operators, compound assignment, conditions, functions |
| test3.kw | String and character literals with all supported escape sequences |
| test4.kw | Error cases: invalid identifiers, malformed float, unterminated literals, bad characters |
| test5.kw | Single-line and multi-line comments including keywords inside comments that must be ignored |