package me.serce.solidity.ide.run.compile

import com.intellij.notification.NotificationGroup
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompilerMessageCategory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType

object SolcMessageProcessor {

  private const val unsorted = "unsorted"

  private val levels = mapOf(
    "Error:" to CompilerMessageCategory.ERROR,
    "Warning:" to CompilerMessageCategory.WARNING,
    unsorted to CompilerMessageCategory.INFORMATION
  )

  private val linkPattern = "(.+\\.sol):(\\d+):(\\d+):".toRegex()

  private const val spanningLines = "Spanning multiple lines."

  private val lineSeparator = System.getProperty("line.separator")

  fun process(messages: String, context: CompileContext) {
    var curLevel = unsorted
    var curPattern: MatchResult? = null
    messagesStream(messages)
      .forEach { line ->
        val link = linkPattern.find(line)
        if (link != null) {
          val levelStartInd = link.range.endInclusive + 1
          val levelEndInd = line.indexOf(":", levelStartInd) + 1
          curLevel = line.substring(levelStartInd, levelEndInd).trim()
          curPattern = link
        }
        if (curPattern != null) {
          val mGroups = curPattern!!.groupValues
          context.addMessage(levels[if (link != null) curLevel else unsorted], line, "file://${mGroups[1]}", mGroups[2].toInt(), mGroups[3].toInt())
        } else {
          context.addMessage(levels[curLevel], line, null, -1, -1)
        }
      }
  }

  private fun messagesStream(messages: String) =
    messages.split(lineSeparator)
      .filterNot { it.isBlank() || it == spanningLines }

  fun showNotification(result: SolcResult, project: Project) {
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
      message = messagesStream(result.messages).filter { it.contains("Error:") }.joinToString("\n")
    }
    val notification = NotificationGroup.balloonGroup("Solidity Compiler").createNotification(
      title, message,
      messageType.toNotificationType(),
      null
    ).setImportant(false)
    notification.notify(project)
  }
}


