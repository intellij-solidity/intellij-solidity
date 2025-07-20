package me.serce.solidity.ide.run

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.util.Key

class ForgeTestTransformingProcessHandler(cmdLine: GeneralCommandLine) : OSProcessHandler(cmdLine) {
  val transformer = ForgetTestOutputTransformer()

  override fun notifyTextAvailable(text: String, outputType: Key<*>) {
    val lines = transformer.transformLine(text)
    lines?.forEach {
      super.notifyTextAvailable(it, outputType)
    }
  }
}

open class ForgetTestOutputTransformer {
  public val compilerFailRegex = Regex("^Compiler run failed:$")
  public val suiteStartRegex = Regex("^Ran \\d+ tests? for (.*\\.t\\.sol):(.*)$")
  public val suiteEndRegex =
    Regex("^Suite result: (\\w+)\\. (\\d+) passed; (\\d+) failed; (\\d+) skipped; finished in (\\d+\\.?\\d+)(.+) \\(.*\\)$")
  public val testRegex = Regex("^\\[(PASS|FAIL)(: (.+))?\\] (.*\\(\\)) \\(.* (\\d+)\\)$")
  public val summaryRegex =
    Regex("^Ran (\\d+) test suites? in (\\d+\\.?\\d+)(.+) \\(.+\\): (\\d+) tests? passed, (\\d+) failed, (\\d+) skipped \\(\\d+ total tests\\)$")

  public var isFail = false
  public var suiteName = ""
  public var activeTest = ""
  public var allTestsRan = false

  fun transformLine(text: String): List<String>? {
    val line = text.trimEnd('\n', '\r')
      .removeSuffix("\n")
      .removeSuffix("\r")

    if (isFail) {
      return listOf("$line\n")
    }
    if (compilerFailRegex.matches(line)) {
      isFail = true
      return listOf("$line\n")
    }

    if (allTestsRan) return null
    if (summaryRegex.matches(line)) {
      allTestsRan = true
      return null
    }

    suiteStartRegex.matchEntire(line)?.let { m ->
      val (fileName, contractName) = m.destructured
      suiteName = contractName
      return listOf("##teamcity[testSuiteStarted name='$suiteName']\n")
    }

    suiteEndRegex.matchEntire(line)?.let { m ->
      val (_, passed, failed, skipped, durationStr, unit) = m.destructured

      val duration = durationStr.toDouble()
      val durationFmt = when (unit) {
        "µs" -> "%.0f".format(duration / 1_000)
        "ms" -> "%.0f".format(duration)
        "s" -> "%.0f".format(duration * 1_000)
        "m" -> "%.0f".format(duration * 60_000)
        "h" -> "%.0f".format(duration * 3_600_000)
        else -> duration.toString()
      }

      return listOf("##teamcity[testSuiteFinished name='$suiteName' duration='$durationFmt']\n")
    }

    val testLines = mutableListOf<String>()

    testRegex.matchEntire(line)?.let { m ->
      val (passFail, _, error, testName, gas) = m.destructured
      activeTest = testName
      testLines.add("##teamcity[testStarted name='$testName' captureStandardOutput='true']\n")
      if (passFail == "FAIL") {
        testLines.add(
          "##teamcity[testFailed name='$testName' message='${escapeTeamCity(line)}' details='${escapeTeamCity(error)}']\n"
        )
      }
    }

    if (activeTest.isNotEmpty() && line.isEmpty()) {
      testLines.add("##teamcity[testFinished name='$activeTest']\n")
      testLines.add("\n")
      activeTest = ""
    }
    if (activeTest.isNotEmpty()) {
      testLines.add("$line\n")
    }

    return testLines
  }

  /**
   * TeamCity messages need single quotes and certain characters escaped.
   * At minimum, we should escape newline, pipe, bracket, etc. Here’s a quick placeholder:
   */
  public fun escapeTeamCity(s: String): String {
    return s
      .replace("|", "||")
      .replace("'", "|'")
      .replace("\n", "|n")
      .replace("\r", "|r")
      .replace("[", "|[")
      .replace("]", "|]")
  }
}
