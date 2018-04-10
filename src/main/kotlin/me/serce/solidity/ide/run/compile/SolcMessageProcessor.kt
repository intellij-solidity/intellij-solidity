package me.serce.solidity.ide.run.compile

import com.intellij.notification.NotificationGroup
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompilerMessageCategory
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.vfs.VirtualFileManager

object SolcMessageProcessor {

  private val defaultLevel = CompilerMessageCategory.INFORMATION

  private val levels = mapOf(
    "Error:" to CompilerMessageCategory.ERROR,
    "Warning:" to CompilerMessageCategory.WARNING
  )

  private val linkPattern = "(.+\\.sol):(\\d+):(\\d+):".toRegex()

  private const val spanningLines = "Spanning multiple lines."

  private const val notificationGroupId = "Solidity Compiler"

  private val lineSeparator = System.getProperty("line.separator")

  private data class Message(
    val level: CompilerMessageCategory = defaultLevel,
    val url: String? = null,
    val lineNum: Int = -1,
    val columnNum: Int = -1,
    val content: MutableList<String> = mutableListOf()
  )

  fun process(solcResult: SolcResult, context: CompileContext) {
    parseMessages(solcResult).forEach {
      context.addMessage(it.level, it.content.joinToString("\n"), it.url, it.lineNum, it.columnNum)
    }
  }

  private fun parseMessages(solcResult: SolcResult): List<Message> {
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
            "file://${mGroups[1]}",
            mGroups[2].toIntOrNull() ?: -1,
            mGroups[3].toIntOrNull() ?: -1
          )
          result.add(curMessage)
        }
        result.last().content.add(line)
      }
    if (!solcResult.success && result.none { it.level == CompilerMessageCategory.ERROR }) {
      if (result.size == 1) {
        result[0] = result[0].copy(level = CompilerMessageCategory.ERROR)
      } else {
        result.add(Message(CompilerMessageCategory.ERROR).copy(content = mutableListOf("Solc returned error code: ${solcResult.exitCode}")))
      }
    }
    return result
  }

  fun showNotification(result: SolcResult, project: Project) {
    val messages = parseMessages(result)
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
        .filter { it.level == CompilerMessageCategory.ERROR }
        .joinToString("\n") {
          return@joinToString if (it.url != null)
            "<a href='${it.url}?${it.lineNum},${it.columnNum}'>${it.content.first()}</a>\n${it.content.drop(1).joinToString("\n")}"
          else it.content.joinToString("\n")
        }
    }
    val notification = (if (result.success) NotificationGroup.logOnlyGroup(notificationGroupId) else NotificationGroup.balloonGroup(notificationGroupId)).createNotification(
      title, message,
      messageType.toNotificationType()
    ) { n, hlu ->
      val url = hlu.url ?: return@createNotification
      val split = url.toString().split("?")
      var urlPart = split[0]
      if (!urlPart.contains("://")) urlPart = urlPart.replaceFirst(":/", ":///")
      val file = VirtualFileManager.getInstance().findFileByUrl(urlPart) ?: return@createNotification
      val open = OpenFileDescriptor(project, file,
        Math.max(0, split[1].split(",")[0].toInt() - 1),
        Math.max(0, split[1].split(",")[1].toInt() - 1))
      open.navigate(true)
    }
      .setImportant(false)
    notification.notify(project)
  }
}


