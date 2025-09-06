package me.serce.solidity.ide.formatting

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessAdapter
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.formatting.FormattingContext
import com.intellij.formatting.service.AsyncDocumentFormattingService
import com.intellij.formatting.service.AsyncFormattingRequest
import com.intellij.formatting.service.FormattingService.Feature
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import me.serce.solidity.settings.FormatterType
import me.serce.solidity.settings.SoliditySettings
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ExecutionException

class SolidityExternalFormatter : AsyncDocumentFormattingService() {

  override fun createFormattingTask(request: AsyncFormattingRequest): FormattingTask? {
    val formattingContext: FormattingContext = request.context
    val project = formattingContext.project
    val settings = SoliditySettings.getInstance(project)
    val userHome = System.getProperty("user.home")

    if (settings.formatterType == FormatterType.INTELLIJ_SOLIDITY) {
      return null
    }

    val psiFile: PsiFile = request.context.containingFile
    val foundryExePath = settings.executablePath.ifBlank { "$userHome/.foundry/bin/forge" }

    return try {
      val cmd = GeneralCommandLine()
        .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
        .withWorkDirectory(project.basePath)
        .withExePath(foundryExePath)
        .withParameters(buildList {
          add("fmt")
          add("-")
          add("--raw")

          if (settings.configPath.isNotBlank()) {
            add("--root")
            add(settings.configPath)
          }
        })
        .withCharset(StandardCharsets.UTF_8)

      val handler = OSProcessHandler(cmd)
      handler.processInput.use { outputStream ->
        outputStream.write(psiFile.text.toByteArray())
        outputStream.flush()
      }
      object : FormattingTask {
        override fun run() {
          val adapter = object : CapturingProcessAdapter() {
            override fun processTerminated(event: ProcessEvent) {
              val exitCode = event.exitCode
              if (exitCode == 0) {
                request.onTextReady(output.stdout)
              } else {
                val onlyErrors = removeWarning(output.stderr).ifBlank { "Formatting failed with exit code $exitCode" }
                request.onError("Solidity", onlyErrors)
              }
            }
          }
          handler.addProcessListener(adapter)
          handler.startNotify()
        }

        override fun cancel(): Boolean {
          handler.destroyProcess()
          return true
        }

        override fun isRunUnderProgress(): Boolean = true
      }
    } catch (e: ExecutionException) {
      request.onError("Solidity", e.message ?: "Unknown error")
      null
    }
  }

  private fun isWarning(line: String): Boolean {
    val l = line.trim().lowercase()
    return l.startsWith("warning:")
  }

  private fun removeWarning(stderr: String): String {
    val lines = stderr.lineSequence().toList()
    val errors = lines.filter { !isWarning(it) }
    return when {
      errors.isNotEmpty() -> errors.joinToString("\n")
      else -> stderr
    }
  }


  override fun getNotificationGroupId(): String {
    return "Solidity"
  }

  override fun getName(): String {
    return "Solidity"
  }

  override fun getFeatures(): Set<Feature?> {
    return EnumSet.noneOf(Feature::class.java)
  }

  override fun canFormat(p0: PsiFile): Boolean {
    val project: Project = p0.project
    val settings = SoliditySettings.getInstance(project)
    return when (settings.formatterType) {
      FormatterType.FOUNDRY -> true
      FormatterType.DISABLED -> false
      FormatterType.INTELLIJ_SOLIDITY -> false
    }
  }
}
