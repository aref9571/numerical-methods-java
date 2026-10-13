# Numerical Methods in Java

A command-line application for solving common numerical-methods problems in Java: error analysis and Taylor series, root finding, systems of linear equations, interpolation, numerical differentiation and integration, and ordinary differential equations (including Runge–Kutta methods and systems of ODEs).

## Status

Early development. The expression parser for arithmetic is implemented; functions, constants and the numerical methods are not implemented yet.

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
| Operators | `+`, `-`, `*`, `/`, `^` and parentheses |

Precedence, from highest to lowest:

1. Parentheses
2. Exponentiation `^`, right-associative: `2^3^2` is `2^(3^2)` = 512
3. Unary `+` and `-`: `-2^2` is `-(2^2)` = -4, and `2^-1` = 0.5
4. `*` and `/`, left-associative: `8/2*4` = 16
5. `+` and `-`, left-associative: `10-4-3` = 3

Multiplication must be written explicitly: `2*x`, not `2x`.

Evaluation fails with a clear message for division by zero, a variable without a value, and any result that is not a finite number, such as `0^-1` or `(-8)^(1/3)`.

## Run the tests

```bash
./mvnw clean test
```

## License

No license has been chosen yet. All rights reserved.
