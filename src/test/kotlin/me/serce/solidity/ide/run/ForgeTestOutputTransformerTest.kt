package me.serce.solidity.ide.run

import me.serce.solidity.utils.SolTestBase

class ForgeTestOutputTransformerTest : SolTestBase() {
  private val transformer = ForgetTestOutputTransformer()

  private val compilerSuccessLine = "Compiler run successful!"
  private val compilerFailLine = "Compiler run failed:"
  private val suiteStartLine = { contract: String -> "Ran 3 tests for test/$contract.t.sol:$contract" }
  private val suiteEndLine = { duration: String -> "Suite result: ok. 1 passed; 0 failed; 0 skipped; finished in ${duration} (868.32µs CPU time)" }
  private val testPassLine = { fn: String -> "[PASS] $fn() (gas: 34345)" }
  private val testFailLine = { fn: String, error: String -> "[FAIL: $error] $fn() (gas: 34345)" }
  private val summaryLine = "Ran 5 test suite in 285.26ms (5.68ms CPU time): 6 tests passed, 2 failed, 0 skipped (8 total tests)"

  private val teamcitySuiteStartLine = { contract: String -> "##teamcity[testSuiteStarted name='$contract']\n" }
  private val teamcitySuiteEndLine = { contract: String, duration: String -> "##teamcity[testSuiteFinished name='$contract' duration='$duration']\n" }
  private val teamcityTestStartLine = { fn: String -> "##teamcity[testStarted name='$fn()' captureStandardOutput='true']\n" }
  private val teamcityTestFailLine = { fn: String, details: String -> "##teamcity[testFailed name='$fn()' message='${transformer.escapeTeamCity(testFailLine(fn, details))}' details='$details']\n" }
  private val teamcityTestFinishLine = { fn: String -> "##teamcity[testFinished name='$fn()']\n" }

  fun testCompilerFailed() {
    transformer.transformLine(compilerFailLine)?.let {
      assert(transformer.isFail)
      assertEquals("Expected only one line", it.size, 1)
      assertEquals("Compiler run failed:\n", it[0])
    }

    assertEquals(listOf("subsequent line\n"), transformer.transformLine("subsequent line"))
  }

  fun testAllTestsRan() {
    transformer.transformLine(summaryLine)?.let {
      assert(transformer.allTestsRan)
      assertNull("Expected no transformed output", it)
      assertNull("Subsequent lines should not be transformed", transformer.transformLine("subsequent line"))
    }
  }

  fun testSuiteStart() {
    transformer.transformLine(suiteStartLine("Counter"))?.let {
      assertEquals("Counter", transformer.suiteName)
      assertEquals("Expected only one line", it.size, 1)
      assertEquals(teamcitySuiteStartLine("Counter"), it[0])
    }
  }

  fun testSuiteEnd() {
    transformer.suiteName = "Counter"

    transformer.transformLine(suiteEndLine("123µs"))?.let {
      assertEquals("Expected only one line", it.size, 1)
      assertEquals(teamcitySuiteEndLine("Counter", "0"), it[0])
    }

    transformer.transformLine(suiteEndLine("123ms"))?.let {
      assertEquals("Expected only one line", it.size, 1)
      assertEquals(teamcitySuiteEndLine("Counter", "123"), it[0])
    }

    transformer.transformLine(suiteEndLine("123s"))?.let {
      assertEquals("Expected only one line", it.size, 1)
      assertEquals(teamcitySuiteEndLine("Counter", "123000"), it[0])
    }
  }

  fun testTestPassStart() {
    transformer.transformLine(testPassLine("testIncrementSuccess"))?.let {
      assertEquals("testIncrementSuccess()", transformer.activeTest)
      assertEquals("Expected 2 lines", it.size, 2)
      assertEquals(teamcityTestStartLine("testIncrementSuccess"), it[0])
      assertEquals("${testPassLine("testIncrementSuccess")}\n", it[1])
    }

    assertEquals(listOf("subsequent line\n"), transformer.transformLine("subsequent line"))
  }

  fun testTestFailStart() {
    transformer.transformLine(testFailLine("testIncrementFail", "some error"))?.let {
      assertEquals("testIncrementFail()", transformer.activeTest)
      assertEquals("Expected 3 lines", it.size, 3)
      assertEquals(teamcityTestStartLine("testIncrementFail"), it[0])
      assertEquals(teamcityTestFailLine("testIncrementFail", "some error"), it[1])
      assertEquals("${testFailLine("testIncrementFail", "some error")}\n", it[2])
    }

    assertEquals(listOf("subsequent line\n"), transformer.transformLine("subsequent line"))
  }

  fun testTestFinish() {
    transformer.activeTest = "testIncrementSuccess()"

    transformer.transformLine("")?.let {
      assertEquals("Expected 2 lines", it.size, 2)
      assertEquals(teamcityTestFinishLine("testIncrementSuccess"), it[0])
      assertEquals("\n", it[1])
    }
  }
}
