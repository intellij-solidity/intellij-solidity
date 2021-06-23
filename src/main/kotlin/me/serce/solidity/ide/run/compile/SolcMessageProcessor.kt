package me.serce.solidity.ide.run.compile

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompilerMessageCategory
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.vfs.VirtualFileManager
import me.serce.solidity.ide.settings.SoliditySettingsConfigurable

object SolcMessageProcessor {

  private val defaultLevel = MessageType.INFO

  private val levels = mapOf(
    "Warning:" to MessageType.WARNING,
    "Error:" to MessageType.ERROR
  )

  private val linkPattern = "(.+\\.sol):(\\d+):(\\d+):".toRegex()

  private const val spanningLines = "Spanning multiple lines."

  private const val notificationGroupId = "Solidity Compiler"

  private val lineSeparator = System.getProperty("line.separator")

  private data class Message(
    val level: MessageType = defaultLevel,
    val url: String? = null,
    val lineNum: Int = -1,
    val columnNum: Int = -1,
    val content: MutableList<String> = mutableListOf()
  )

  fun process(solcResult: SolcResult, context: CompileContext) {
    parseMessages(solcResult, context.project).forEach {
      context.addMessage(it.level.toCompilationCategory(), it.content.joinToString("\n"), it.url, it.lineNum, it.columnNum)
    }
  }

  private fun MessageType.toCompilationCategory(): CompilerMessageCategory {
    return when (this) {
      MessageType.ERROR -> CompilerMessageCategory.ERROR
      MessageType.WARNING -> CompilerMessageCategory.WARNING
      MessageType.INFO -> CompilerMessageCategory.INFORMATION
      else -> throw IllegalArgumentException("Unsupported type: $this")
    }
  }

  private fun parseMessages(solcResult: SolcResult, project: Project): List<Message> {
    val result = mutableListOf(Message())
    solcResult.messages.split(lineSeparator)
      .filterNot { it.isBlank() || it == spanningLines }
      .forEach { line ->
        val link = linkPattern.find(line)
        if (link != null) {
          val levelStartInd = link.range.endInclusive + 1
          val levelEndInd = line.indexOf(":", levelStartInd) + 1
          val curLevel = line.substring(levelStartInd, levelEndInd).trim()
          val mGroups = link.groupValues
          val curMessage = Message(
            levels[curLevel] ?: defaultLevel,
            "${project.guessProjectDir()}/${mGroups[1]}",
            mGroups[2].toIntOrNull() ?: -1,
            mGroups[3].toIntOrNull() ?: -1
          )
          result.add(curMessage)
        }
        result.last().content.add(line)
      }
    if (!solcResult.success && result.none { it.level == MessageType.ERROR }) {
      if (result.size == 1) {
        result[0] = result[0].copy(level = MessageType.ERROR)
      } else {
        result.add(Message(MessageType.ERROR).copy(content = mutableListOf("Solc returned error code: ${solcResult.exitCode}")))
      }
    }
    return result
  }

  fun showNotification(result: SolcResult, project: Project) {
    val messages = parseMessages(result, project)
    val title: String
    val message: String
    val messageType: MessageType
    if (result.success) {
      title = "Solidity compilation completed"
      messageType = MessageType.INFO
      message = "successfully"
    } else {
      title = "Solidity compilation failed"
      messageType = MessageType.ERROR
      message = messages
        .filter { it.level == MessageType.ERROR }
        .joinToString("\n") {
          return@joinToString if (it.url != null)
            "<a href='${it.url}?${it.lineNum},${it.columnNum}'>${it.content.first()}</a>\n${it.content.drop(1).joinToString("\n")}"
          else it.content.joinToString("\n")
        }
    }
    val notification = (if (result.success) NotificationGroup.logOnlyGroup(notificationGroupId) else NotificationGroup.balloonGroup(notificationGroupId)).createNotification(
      title, message,
      messageType.toNotificationType()
      , NotificationListener { _, hlu ->
      val url = hlu.url ?: return@NotificationListener

      val (urlPart, filePart) = url.toString().split("?", limit = 2)
      val (lineNum, colNum) = filePart.split(",", limit = 2)
      // VirtualFileManager can't handle urls without '//' schema part
      val urlPartFixed = if (!urlPart.contains("://")) urlPart.replaceFirst(":/", ":///") else urlPart

      val file = VirtualFileManager.getInstance().findFileByUrl(urlPartFixed) ?: return@NotificationListener
      val open = OpenFileDescriptor(project, file,
        Math.max(0, lineNum.toInt() - 1),
        Math.max(0, colNum.toInt() - 1))
      open.navigate(true)
    })
      .setImportant(false)
    notification.notify(project)
  }

  fun showNoEvmMessage(project: Project) {
    NotificationGroup.balloonGroup(notificationGroupId)
      .createNotification("SolcJ compiler is not found", "<a href=\"#\">Please configure EthereumJ bundle</a>", NotificationType.INFORMATION,
        NotificationListener { _, _ ->
          SoliditySettingsConfigurable().getQuickFix(project).run()
        }).notify(project)
  }
}


