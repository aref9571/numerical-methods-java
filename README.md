# Numerical Methods in Java

A command-line application for solving common numerical-methods problems in Java: error analysis and Taylor series, root finding, systems of linear equations, interpolation, numerical differentiation and integration, and ordinary differential equations (including Runge–Kutta methods and systems of ODEs).

## Status

Early development. The expression parser, including functions and constants, is implemented; the numerical methods are not implemented yet.

## Requirements

- Java 21 (JDK)
- No separate Maven installation is needed; the Maven Wrapper downloads the correct Maven version.

## Build and run

```bash
git clone https://github.com/aref9571/numerical-methods-java.git
cd numerical-methods-java
./mvnw clean package
java -jar target/numerical-methods-0.1.0-SNAPSHOT.jar
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

## Expression syntax

Expressions are real-valued and use the following rules.

| Element | Examples |
|---|---|
| Numbers | `2`, `2.5`, `.25`, `1e-6`, `2.5E+3` |
| Variables | `x`, `t`, `y1`, `y2` (an English letter followed by letters or digits) |
| Constants | `pi`, `e` |
| Functions | `sin`, `cos`, `tan`, `asin`, `acos`, `atan`, `exp`, `ln`, `log10`, `sqrt`, `abs` |
| Operators | `+`, `-`, `*`, `/`, `^` and parentheses |

Precedence, from highest to lowest:

1. Parentheses
2. Exponentiation `^`, right-associative: `2^3^2` is `2^(3^2)` = 512
3. Unary `+` and `-`: `-2^2` is `-(2^2)` = -4, and `2^-1` = 0.5
4. `*` and `/`, left-associative: `8/2*4` = 16
5. `+` and `-`, left-associative: `10-4-3` = 3

Multiplication must be written explicitly: `2*x`, not `2x`.

Functions take exactly one argument in parentheses, for example `sin(x)` or `sqrt(x^2 + 1)`. Trigonometric functions use radians, `ln` is the natural logarithm and `log10` is the base-10 logarithm. Names are case-sensitive, so `Sin(x)` is an unknown function. The names `pi` and `e` and the function names cannot be used as variables.

Evaluation fails with a clear message for division by zero, a variable without a value, a value outside a function's domain such as `sqrt(-1)`, `ln(0)` or `asin(2)`, and any other result that is not a finite number, such as `0^-1` or `(-8)^(1/3)`.

Expressions are limited to 1000 characters and 100 levels of nesting (parentheses, function calls, unary signs and chained `^`). Longer or deeper input is rejected with a syntax error instead of exhausting memory or the call stack.

Syntax errors report the column where the problem was found, counting from 1.

| Input | Result |
|---|---|
| `x^3 - x - 2` with `x = 2` | `4` |
| `exp(-x^2/2) / sqrt(2*pi)` with `x = 0` | `0.3989...` |
| `2*sin(pi/6) + ln(e)` | `2` |
| `2x + 1` | Missing operator between '2' and 'x'; multiplication must be written explicitly, for example 2*x at column 2 |
| `2*(x+1` | The '(' at column 3 is never closed, expected ')' at column 7 |
| `sqrt(-1)` | sqrt(-1.0) is undefined or not a finite number |

## Run the tests

```bash
./mvnw clean test
```

## License

No license has been chosen yet. All rights reserved.
