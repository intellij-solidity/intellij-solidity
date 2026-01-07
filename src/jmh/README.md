# JMH Benchmarks for IntelliJ Solidity

This directory contains benchmarks for the IntelliJ Solidity plugin using [JMH](https://openjdk.org/projects/code-tools/jmh/).

The benchmarks exist here not to get absolute numbers, but rather to provide a way to measure any performance impact of the changes made to the plugin.

## Running a benchmark

To run all benchmarks, use the following Gradle command from the project root:

```bash
./gradlew jmh
```

To run a specific benchmark:

```bash
./gradlew jmh -Pjmh.includes='SolidityParserBenchmark'
```

## Project fixtures

As it's often hard to get realistic input data for benchmarks, `ProjectFixtureSupport` provides a set of fixtures that are derived from real-world Solidity projects.
