# Numerical Methods in Java

A command-line application for solving common numerical-methods problems in Java: error analysis and Taylor series, root finding, systems of linear equations, interpolation, numerical differentiation and integration, and ordinary differential equations (including Runge–Kutta methods and systems of ODEs).

## Status

Early development. The project currently contains the build setup and a minimal entry point; no numerical methods are implemented yet.

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

## Run the tests

```bash
./mvnw clean test
```

## License

No license has been chosen yet. All rights reserved.
