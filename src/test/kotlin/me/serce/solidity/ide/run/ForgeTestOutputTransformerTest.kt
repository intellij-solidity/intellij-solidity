package me.serce.solidity.ide.run

import me.serce.solidity.utils.SolTestBase

class ForgeTestOutputTransformerTest : SolTestBase() {
  private val transformer = ForgetTestOutputTransformer()

  private val compilerSuccessLine = "Compiler run successful!"
  private val compilerFailLine = "Compiler run failed:"
  private val suiteStartLine = { contract: String -> "Ran 3 tests for test/$contract.t.sol:$contract" }
  private val suiteEndLine = { duration: String -> "Suite result: ok. 1 passed; 0 failed; 0 skipped; finished in ${duration}ms (868.32µs CPU time)" }
  private val testPassLine = { fn: String -> "[PASS] $fn() (gas: 34345)" }
  private val testFailLine = { fn: String, error: String -> "[FAIL: $error] $fn() (gas: 34345)" }
  private val summaryLine = "Ran 5 test suite in 285.26ms (5.68ms CPU time): 6 tests passed, 2 failed, 0 skipped (8 total tests)"

  private val teamcitySuiteStartLine = { contract: String -> "##teamcity[testSuiteStarted name='$contract']\n" }
  private val teamcitySuiteEndLine = { contract: String, duration: String -> "##teamcity[testSuiteFinished name='$contract' duration='$duration']\n" }
  private val teamcityTestStartLine = { fn: String -> "##teamcity[testStarted name='$fn()' captureStandardOutput='true']\n" }
  private val teamcityTestFailLine = { fn: String, details: String -> "##teamcity[testFailed name='$fn()' message='${transformer.escapeTeamCity(testFailLine(fn, details))}' details='$details']\n" }
  private val teamcityTestFinishLine = { fn: String -> "##teamcity[testFinished name='$fn()']\n" }

  fun testCompilerFailed() {
    val lines = transformer.transformLine(compilerFailLine)
    assert(transformer.isFail)

    lines?.let {
      assertEquals("Expected only one line", it.size, 1)
      assertEquals("Compiler run failed:\n", it[0])
    }

    assertEquals(listOf("subsequent line\n"), transformer.transformLine("subsequent line"))
  }

  fun testAllTestsRan() {
    val lines = transformer.transformLine(summaryLine)
    assert(transformer.allTestsRan)
    assertNull("Expected no transformed output", lines)
    assertNull("Subsequent lines should not be transformed", transformer.transformLine("subsequent line"))
  }

  fun testSuiteStart() {
    val lines = transformer.transformLine(suiteStartLine("Counter"))
    assertEquals("Counter", transformer.suiteName)

    lines?.let {
      assertEquals("Expected only one line", it.size, 1)
      assertEquals(teamcitySuiteStartLine("Counter"), it[0])
    }
  }

  fun testSuiteEnd() {
    transformer.suiteName = "Counter"
    val lines = transformer.transformLine(suiteEndLine("123"))

    lines?.let {
      assertEquals("Expected only one line", it.size, 1)
      assertEquals(teamcitySuiteEndLine("Counter", "123"), it[0])
    }
  }

  fun testTestPassStart() {
    val lines = transformer.transformLine(testPassLine("testIncrementSuccess"))
    assertEquals("testIncrementSuccess()", transformer.activeTest)

    lines?.let {
      assertEquals("Expected 2 lines", it.size, 2)
      assertEquals(teamcityTestStartLine("testIncrementSuccess"), it[0])
      assertEquals("${testPassLine("testIncrementSuccess")}\n", it[1])
    }

    assertEquals(listOf("subsequent line\n"), transformer.transformLine("subsequent line"))
  }

  fun testTestFailStart() {
    val lines = transformer.transformLine(testFailLine("testIncrementFail", "some error"))
    assertEquals("testIncrementFail()", transformer.activeTest)

    lines?.let {
      assertEquals("Expected 3 lines", it.size, 3)
      assertEquals(teamcityTestStartLine("testIncrementFail"), it[0])
      assertEquals(teamcityTestFailLine("testIncrementFail", "some error"), it[1])
      assertEquals("${testFailLine("testIncrementFail", "some error")}\n", it[2])
    }

    assertEquals(listOf("subsequent line\n"), transformer.transformLine("subsequent line"))
  }

  fun testTestFinish() {
    transformer.activeTest = "testIncrementSuccess()"
    val lines = transformer.transformLine("")

    lines?.let {
      assertEquals("Expected 2 lines", it.size, 2)
      assertEquals(teamcityTestFinishLine("testIncrementSuccess"), it[0])
      assertEquals("\n", it[1])
    }
  }
}
