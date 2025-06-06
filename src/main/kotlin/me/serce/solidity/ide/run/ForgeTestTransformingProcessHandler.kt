package me.serce.solidity.ide.run

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.util.Key

class ForgeTestTransformingProcessHandler(cmdLine: GeneralCommandLine) : OSProcessHandler(cmdLine) {
  private val suiteStartRegex = Regex("^Ran \\d+ tests? for (.*\\.t\\.sol):(.*)$")
  private val suiteEndRegex =
    Regex("^Suite result: (\\w+)\\. (\\d+) passed; (\\d+) failed; (\\d+) skipped; finished in (\\d+\\.?\\d+)(.+) \\(.*\\)$")
  private val testRegex = Regex("^\\[(PASS|FAIL)(: (.+))?\\] (.*\\(\\)) \\(.* (\\d+)\\)$")
  private val summaryRegex =
    Regex("^Ran (\\d+) test suites in (\\d+\\.?\\d+)(.+) \\(.+\\): (\\d+) tests passed, (\\d+) failed, (\\d+) skipped \\(\\d+ total tests\\)$")

  private var suiteName = ""
  private var activeTest = ""
  private var allTestsRan = false

  override fun notifyTextAvailable(text: String, outputType: Key<*>) {
    val line = text.trimEnd('\n', '\r')
      .removeSuffix("\n")
      .removeSuffix("\r")

    if (allTestsRan) return
    if (summaryRegex.matches(line)) {
      allTestsRan = true
      return
    }

    suiteStartRegex.matchEntire(line)?.let { m ->
      val (fileName, contractName) = m.destructured
      suiteName = contractName
      super.notifyTextAvailable("##teamcity[testSuiteStarted name='$suiteName']\n", ProcessOutputTypes.STDOUT)
      return
    }

    suiteEndRegex.matchEntire(line)?.let { m ->
      val (_, passed, failed, skipped, durationStr, unit) = m.destructured

      var duration = durationStr.toDouble()
      when (unit) {
        "µs" -> duration /= 1_000
        "s" -> duration *= 1_000
        "m" -> duration *= 60_000
        "h" -> duration *= 3_600_000
      }

      super.notifyTextAvailable(
        "##teamcity[testSuiteFinished name='$suiteName' duration='$duration']\n",
        ProcessOutputTypes.STDOUT
      )
      return
    }

    testRegex.matchEntire(line)?.let { m ->
      val (passFail, _, error, testName, gas) = m.destructured
      activeTest = testName
      super.notifyTextAvailable(
        "##teamcity[testStarted name='$testName' captureStandardOutput='true']\n",
        ProcessOutputTypes.STDOUT
      )
      if (passFail == "FAIL") {
        super.notifyTextAvailable(
          "##teamcity[testFailed name='$testName' message='${escapeTeamCity(line)}' details='${escapeTeamCity(error)}']\n",
          ProcessOutputTypes.STDOUT
        )
      }
    }

    if (activeTest.isNotEmpty() && line.isEmpty()) {
      super.notifyTextAvailable("##teamcity[testFinished name='$activeTest']\n", ProcessOutputTypes.STDOUT)
      super.notifyTextAvailable("\n", ProcessOutputTypes.STDOUT)
      activeTest = ""
      return
    }
    if (activeTest.isNotEmpty()) {
      super.notifyTextAvailable("$line\n", ProcessOutputTypes.STDOUT)
    }
  }

  /**
   * TeamCity messages need single quotes and certain characters escaped.
   * At minimum, we should escape newline, pipe, bracket, etc. Here’s a quick placeholder:
   */
  private fun escapeTeamCity(s: String): String {
    return s
      .replace("|", "||")
      .replace("'", "|'")
      .replace("\n", "|n")
      .replace("\r", "|r")
      .replace("[", "|[")
      .replace("]", "|]")
  }
}
