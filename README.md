# Numerical Methods in Java

A command-line application for solving common numerical-methods problems in Java: error analysis and Taylor series, root finding, systems of linear equations, interpolation, numerical differentiation and integration, and ordinary differential equations (including Runge–Kutta methods and systems of ODEs).

## Status

Early development. The expression parser, symbolic derivatives, the shared conventions for solver results and input validation, the error metrics, Taylor polynomials, the bisection, false position, Newton and secant root-finding methods, fixed-point iteration with Aitken extrapolation and a root scan over an interval are implemented; the other numerical methods are not implemented yet.

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

Not supported: complex numbers, functions with more than one argument, user-defined functions, piecewise functions and other textbook notations not listed above.

Number literals must fit in a `double` without losing precision: `1e999` is too large and a non-zero literal below about `2.2e-308`, such as `1e-400`, is too small. Both are syntax errors.

Functions take exactly one argument in parentheses, for example `sin(x)` or `sqrt(x^2 + 1)`. Trigonometric functions use radians, `ln` is the natural logarithm and `log10` is the base-10 logarithm. Names are case-sensitive, so `Sin(x)` is an unknown function. The names `pi` and `e` and the function names cannot be used as variables.

Evaluation fails with a clear message for division by zero, a variable without a value, a value outside a function's domain such as `sqrt(-1)`, `ln(0)` or `asin(2)`, and any other result that is not a finite number, such as `0^-1` or `(-8)^(1/3)`. `tan` is reported as undefined when its argument lies on a pole such as `pi/2`, within a few units of floating-point rounding; values close to a pole, such as `tan(1.5707)`, are still returned.

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

## Expression text and derivatives

Parsed expressions can be shown as fully bracketed text, so it is clear how the input was read: `-x^2` is shown as `-(x^2)` and `x^3 - x - 2` as `((x^3) - x) - 2`. The text can be parsed again and gives the same expression.

Derivatives are computed symbolically with the sum, product, quotient, power and chain rules, for every supported function. Trivial terms are simplified, so the derivative of `x^3 - x - 2` is `(3 * (x^2)) - 1`. Other variables are treated as constants.

| Expression | Derivative |
|---|---|
| `x^n` with `n` not depending on `x` | `n * x^(n - 1)` |
| `a^x` with `a` not depending on `x` | `a^x * ln(a)` |
| `u^v` in general | `u^v * (v' * ln(u) + v * u' / u)`, so `u` must be positive |
| `abs(u)` | `u / abs(u)`, undefined where `u = 0` |
| `sqrt(u)`, `asin(u)`, `acos(u)` | undefined where the function has a vertical tangent, for example `sqrt` at 0 |

Evaluating a derivative fails with a clear message at points where it is undefined, instead of returning a value. A derivative is only meaningful where the original expression is defined: for example, the derivative of `ln(x)` is `1 / x`, which can be evaluated at `x = -1` even though `ln(-1)` is undefined, so the original expression should always be evaluated first. Derivatives with more than 100000 terms are rejected.

## Results and errors

Iterative methods stop when the change between steps is within the tolerance or when they reach the iteration limit.

- The tolerance must be a positive, finite number. The default is `1e-8`.
- The iteration limit must be between 1 and 100000. The default is 100.
- By default the tolerance works as an absolute tolerance for values up to 1 in size and as a relative tolerance for larger values: a step passes when `|change| <= tolerance * max(1, |value|)`.
- In absolute mode a step passes when `|change| <= tolerance`, whatever the size of the value. This is the usual stopping rule for bisection, `b - c <= tolerance`.

Each solver result has one of three statuses:

| Status | Meaning |
|---|---|
| `CONVERGED` | The stopping test passed. |
| `ITERATION_LIMIT_REACHED` | The limit was reached first. The last approximation is reported, but it is not presented as a solution. |
| `BREAKDOWN` | The method could not continue, for example because a derivative became zero or a function value was undefined. The reason is reported. |

Root-finding results also report the approximate root, the function value there, the number of iterations and an error estimate. The error estimate depends on the method and is not a guaranteed bound.

Invalid input, such as a non-finite number, a tolerance of 0 or an iteration limit outside the allowed range, is rejected with an error before any calculation starts. Non-convergence is a normal result status, not an error. All results depend on floating-point precision, the input domain, each method's assumptions and the stopping test.

## Error metrics

For a true value `p` and an approximation `p*`:

| Metric | Formula |
|---|---|
| Absolute error | `\|p - p*\|` |
| Relative error | `\|p - p*\| / \|p\|` |
| Percentage error | `100 * \|p - p*\| / \|p\|` |

Both values must be finite numbers. When the true value is 0, the relative and percentage errors are undefined, and they are reported as errors instead of returning infinity or NaN. Use the absolute error in that case. A result too large for a `double` is also reported as an error.

These metrics measure the error of one approximation against a known true value. They are not theoretical error bounds: a bound limits the error a method can make under stated conditions, without needing the true value, while a measured error only describes the case that was computed.

## Taylor polynomials

The Taylor polynomial of order `n` about a center `a` is

`T(x) = f(a) + f'(a) * (x - a) + f''(a) / 2! * (x - a)^2 + ... + f^(n)(a) / n! * (x - a)^n`

The coefficients come from the symbolic derivatives above, so they are exact up to floating-point rounding. The center and the target point must be finite, and the order must be between 0 and 20. When the function can be evaluated at the target point, the result also reports the exact value and the observed absolute and relative error; the relative error is left out when the exact value is 0.

Limitations:

- If the function or one of its derivatives is undefined at the center, for example `sqrt(x)` or `abs(x)` about 0, the calculation fails with a message naming the derivative.
- Symbolic derivatives grow quickly for quotients, roots, logarithms and `tan`. Functions such as `1/(1+x^2)`, `sqrt(1+x)` or `ln(x)` usually reach the 100000-term limit at orders above about 6 to 8, and this is reported as an error. `sin`, `cos`, `exp` and polynomials work up to order 20.
- A higher order does not always help: far from the center, or outside the radius of convergence, as for `ln(x)` about 1 at `x = 3`, the error can grow with the order.
- The observed error compares the polynomial with the computed function value at one point. It is not the Taylor remainder bound and does not guarantee the error at other points.

## Bracketing root finding

Bisection and false position (regula falsi) look for a root of `f` in an interval `[a, b]`. Both need `a < b`, finite endpoints, an interval whose length fits in a `double`, `f` defined at both endpoints and a sign change: `f(a)` and `f(b)` must have opposite signs. Otherwise the input is rejected before any iteration starts. If `f(a)` or `f(b)` is exactly 0, that endpoint is returned after 0 iterations (the lower one if both are roots).

- **Bisection** takes the midpoint and keeps the half where the sign changes. It stops when the interval is within the tolerance, or when `f` is exactly 0 at a midpoint.
- **False position** takes the point where the straight line through `(a, f(a))` and `(b, f(b))` crosses zero. When two successive points are within the tolerance, it checks `f` one tolerance step further towards the other end. It stops only if the sign changes there, so the root is known to lie within that step; otherwise it keeps that point as the new endpoint and continues.

For both methods the reported error estimate is the length of an interval that contains a sign change. If `f` is continuous there, this is a bound on the distance to a root.

Limitations:

- A root where `f` touches zero without changing sign, such as `(x - 1)^2`, cannot be bracketed.
- A sign change can also come from a pole or a jump, as for `tan(x)` on `[1, 2]` or `1/x` on `[-1, 2]`. If the interval closes in on a point where `|f|` is larger than at both starting endpoints, or `f` is undefined at a new point, the result is `BREAKDOWN`, not a root.
- False position can be very slow when one endpoint never moves, for example `(x - 1)^3` on `[0, 3]`, which reaches the default limit of 100 iterations while bisection converges in 29. The result is then `ITERATION_LIMIT_REACHED`.
- Bisection halves the interval every step, so after `n` steps the root lies within `(b - a) / 2^n` of the midpoint. With an absolute tolerance it therefore needs `ceil(log2((b - a) / tolerance))` iterations, for example 10 for `x^6 - x - 1` on `[1, 2]` with tolerance `0.001`.
- Bisection cannot split an interval below the spacing of `double` values. A tolerance smaller than that spacing, such as `1e-20` for a root near 1.4, ends in `BREAKDOWN` with the best approximation found.

## Open root finding

Newton's method and the secant method start from one or two guesses instead of a bracket. Each returns the usual result (root, `f` at the root, error estimate, iteration count and status) together with the list of iterates, from which the observed order of convergence can be estimated (about 2 for Newton, about 1.6 for secant when the iteration converges cleanly).

- **Newton** uses `x_next = x - f(x) / f'(x)`. The derivative is computed symbolically, or an expression for it can be supplied. It stops when the step is within the tolerance or `f` is exactly 0.
- **Secant** replaces the derivative by the slope through the last two points. It needs two different starting values. It stops only after two consecutive steps are within the tolerance, so one accidentally small step is not enough.

A starting value where `f` is undefined, or equal starting values for the secant method, are rejected before any iteration. Problems during the iteration give `BREAKDOWN` with a reason, never an invented root: a zero or undefined derivative, an iterate outside the domain of `f`, a flat secant line (`f` equal at both points), or a secant step of exactly 0 where `f` is not 0 ("this is not a root").

Limitations:

- Both methods can diverge or cycle from a poor start. For example Newton on `x^3 - 2*x + 2` from 0 jumps between 0 and 1 until the iteration limit, and Newton on `atan(x)` from 1.5 overshoots until the derivative can no longer be evaluated.
- At a double root such as `(x - 1)^2` the convergence is only linear, so many more iterations are needed than the usual order suggests.
- Secant on `x^10 - 1` from `[0, 1.3]` stalls in the flat region near 0.18 where `f = -1`, and reports a breakdown instead of a root.

## Fixed-point iteration

Fixed-point iteration solves `x = g(x)` by repeating `x_next = g(x)` from a starting value. The result includes the list of iterates, the symbolic derivative `g'`, its value at the start and at the final iterate, and an Aitken extrapolation of the limit with its error estimate (available once there are three iterates).

- **Convergence** is expected when `|g'| < 1` near the fixed point. A slope near 0 gives fast convergence (the Newton form `(x + 5/x)/2` for the square root of 5), a slope near 1 gives slow linear convergence.
- **Interval check:** for an interval `[a, b]`, 201 sample points are used to test whether `g` maps the interval into itself and to estimate `lambda`, the largest `|g'|`. If `lambda < 1` and the interval maps into itself, `g` is a contraction, and the error after `n` steps is at most `lambda^n / (1 - lambda)` times the first step.

Limitations:

- A slope with `|g'| > 1` makes the iteration diverge. It is stopped as `BREAKDOWN` once a value is not finite or exceeds `1e100`, or if `g` is undefined at an iterate.
- A slope of exactly -1, as for `5/x`, makes the iterates oscillate between two values until the iteration limit.
- The interval check is a sampled estimate, not a proof.

## Root scan

A root scan looks for every root of `f` in `[a, b]`. It evaluates `f` at equally spaced sample points (1000 intervals by default, at most 1000000) and refines each sign change between neighbouring samples with bisection. For `sin(1/x)` on `[0.05, 1]` it finds the six roots `1/(k*pi)`, `k = 1, ..., 6`.

The scan result lists:

- the roots found, in increasing order, including samples where `f` is exactly 0;
- suspected discontinuities: sign changes where bisection ends in `BREAKDOWN`, such as the pole of `1/(x - 1.05)`, which are not reported as roots;
- the number of samples where `f` is undefined, which are skipped (for `ln(x)` on `[-1, 3]`, every sample at or below 0);
- possible double roots: samples where `|f|` drops to at most a quarter of both neighbours without a sign change, as near 1.01 for `(x - 1.01)^2` on `[0, 3]`.

Limitations:

- Two roots closer together than the sample spacing can cancel each other's sign change and be missed; more samples help.
- A possible double root is only a hint for a closer look, for example with Newton's method; the scan does not refine it.

## Run the tests

```bash
./mvnw clean test
```

## License

No license has been chosen yet. All rights reserved.
