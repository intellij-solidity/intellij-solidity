## Setting up

The project can be imported into IntelliJ IDEA. There are two default run configurations:

* `lexer parser` Regenerates the lexer and the parser
* `Run IntelliJ` Recompiles the plugin ands starts an IDE with the plugin, can be used for debugging.

## Grammar

To work on grammar, the [Grammar-Kit](https://github.com/JetBrains/Grammar-Kit) plugin needs to be installed. The grammar for the parser is defined in [solidity.bnf](src/main/grammars/solidity.bnf), and the corresponding lexer is defined in [_SolidityLexer.flex](src/main/grammars/_SolidityLexer.flex).

When working on grammar, it's convenient to use the `Live Preview` and `Structure view`, see the [usage instructions](https://github.com/JetBrains/Grammar-Kit#general-usage-instructions) for the Grammar-Kit.

It's important to remember that running the tests from IDE doesn't regenerate the grammar, so every time the lexer or parser are changed, `lexer parser` run configuration needs to be run.

## Formatting

The implementation of the formatter aims to follow the [official style guide](https://github.com/ethereum/solidity/blob/develop/docs/style-guide.rst) by default, but also to be configurable to step away from the conventions when needed.

The formatting rules are defined in `src/main/kotlin/me/serce/solidity/ide/formatting`, with the corresponding tests in `src/test/kotlin/me/serce/solidity/ide/formatting`. The [JetBrains documentation](https://www.jetbrains.org/intellij/sdk/docs/tutorials/custom_language_support/formatter.html) can be used as a reference.

## Exceptions

User-reported errors are collected by Sentry. If you're a contributor and would like to have an account, please contact @SerCeMan.

## Releasing

The plugin is released once a month, typically at the end of the second week. If there is a need for an out-of-schedule release, please contact @SerCeMan.
