package me.serce.solidity.ide

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.diagnostic.SubmittedReportInfo.SubmissionStatus.FAILED
import com.intellij.openapi.diagnostic.SubmittedReportInfo.SubmissionStatus.NEW_ISSUE
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.Consumer
import io.sentry.DefaultSentryClientFactory
import io.sentry.Sentry
import io.sentry.connection.ConnectionException
import io.sentry.event.Event
import io.sentry.event.EventBuilder
import io.sentry.event.interfaces.ExceptionInterface
import java.awt.Component

class SentryReportSubmitter : ErrorReportSubmitter() {

  init {
    // IntelliJ Solidity N
    val dsn = "https://4cf4758bb74b408a82c9c4f200b63837:45584ba1888b405f80c886e163031be0@sentry.io/301677" +
      "?${DefaultSentryClientFactory.UNCAUGHT_HANDLER_ENABLED_OPTION}=false"
    Sentry.init(dsn)
  }

  private val pluginVersion = PluginManager.getPlugin(PluginId.getId("me.serce.solidity"))?.version ?: "unknown"

  override fun getReportActionText() = "Submit error to IntelliJ Solidity maintainers"

  override fun submit(
    events: Array<out IdeaLoggingEvent>,
    additionalInfo: String?,
    parentComponent: Component,
    consumer: Consumer<SubmittedReportInfo>
  ): Boolean {
    val ijEvent = events.firstOrNull()
    if (ijEvent == null) {
      return true
    }
    if (pluginVersion.endsWith("-SNAPSHOT")) {
      // do not report errors from dev-versions. If someone uses a dev version, he will
      // be able to report the issue and all related info as a github issue or even fix it.
      consumer.consume(SubmittedReportInfo(null, "Error submission is disable in dev version", FAILED))
      return false
    }
    val error = ijEvent.throwable
    val eventBuilder = EventBuilder()
      .withMessage(ijEvent.message)
      .withLevel(Event.Level.ERROR)
      .withTag("build", ApplicationInfo.getInstance().build.asString())
      .withTag("plugin_version", pluginVersion)
      .withTag("os", SystemInfo.OS_NAME)
      .withTag("os_version", SystemInfo.OS_VERSION)
      .withTag("os_arch", SystemInfo.OS_ARCH)
      .withTag("java_version", SystemInfo.JAVA_VERSION)
      .withTag("java_runtime_version", SystemInfo.JAVA_RUNTIME_VERSION)
    if (error != null) {
      eventBuilder.withSentryInterface(ExceptionInterface(error))
    }
    if (additionalInfo != null) {
      eventBuilder.withExtra("additional_info", additionalInfo)
    }
    if (events.size > 1) {
      eventBuilder.withExtra("extra_events", events.drop(1).joinToString("\n") { it.toString() })
    }
    return try {
      Sentry.capture(eventBuilder)
      consumer.consume(SubmittedReportInfo(null, "Error has been successfully reported", NEW_ISSUE))
      true
    } catch (e: ConnectionException) {
      // sentry is temporarily unavailable
      consumer.consume(SubmittedReportInfo(null, "Error submission has failed", FAILED))
      false
    }
  }
}
