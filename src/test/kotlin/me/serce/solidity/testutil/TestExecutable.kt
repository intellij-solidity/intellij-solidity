package me.serce.solidity.testutil

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File
import java.nio.file.Files
import java.nio.file.Files.deleteIfExists
import java.nio.file.Path

class TestExecutable private constructor(
  private val dir: Path,
  private val script: File,
  private val argsLog: File,
  private val stdinLog: File,
) : Disposable {
  val path: String get() = script.absolutePath

  fun readCapturedArgs(): List<String> =
    if (argsLog.exists()) argsLog.readLines().filter { it.isNotEmpty() } else emptyList()

  fun readCapturedStdin(): String = if (stdinLog.exists()) stdinLog.readText() else ""

  override fun dispose() {
    runCatching { script.delete() }
    runCatching { argsLog.delete() }
    runCatching { stdinLog.delete() }
    runCatching { deleteIfExists(dir) }
  }

  sealed interface Workdir {
    data class UnderDir(val path: Path) : Workdir
    data class FixedDir(val path: Path) : Workdir
  }

  class Builder(
    private val name: String,
    private val workdir: Workdir,
    val registerWith: Disposable,
  ) {
    private var stdoutContent: String? = null
    private var stderrContent: String? = null
    private var echoStdin: Boolean = true
    private var exitCode: Int = 0

    fun echoStdinToStdout(value: Boolean) = apply { this.echoStdin = value }
    fun stderr(text: String?) = apply { this.stderrContent = text }
    fun exitCode(code: Int) = apply { this.exitCode = code }

    fun build(): TestExecutable {
      val dir = when (val spec = workdir) {
        is Workdir.FixedDir -> {
          Files.createDirectories(spec.path)
          spec.path
        }

        is Workdir.UnderDir -> {
          runCatching { Files.createDirectories(spec.path) }
          Files.createTempDirectory(spec.path, "${name}-stub")
        }
      }
      val script = dir.resolve(name).toFile().apply {
        deleteOnExit();
      }
      val argsLog = dir.resolve("args.log").toFile().apply { deleteOnExit() }
      val stdinLog = dir.resolve("stdin.log").toFile().apply { deleteOnExit() }

      script.writeText(buildString {
        appendLine("#!/usr/bin/env bash")
        appendLine("set -euo pipefail")
        // record args, one per line
        appendLine("printf '%s\\n' \"\$@\" > '${argsLog.absolutePath}'")
        // consume stdin and record it (always). If echo enabled, send to stdout.
        appendLine("if [ ! -t 0 ]; then")
        appendLine("  if [ ${if (echoStdin) "1" else "0"} -eq 1 ]; then")
        appendLine("    tee '${stdinLog.absolutePath}' >/dev/stdout")
        appendLine("  else")
        appendLine("    cat - > '${stdinLog.absolutePath}'")
        appendLine("  fi")
        appendLine("fi")
        // if not echoing, write the configured stdout text
        val stdout = stdoutContent
        if (!echoStdin && (stdout != null)) {
          // Use printf %b to respect backslash escapes/newlines in tests if needed
          appendLine("printf %b \"${escapeForSh(stdout)}\"")
        }
        // write stderr if any
        val stderr = stderrContent
        if (stderr != null) {
          appendLine("printf %b \"${escapeForSh(stderr)}\" 1>&2")
        }
        // exit code
        appendLine("exit $exitCode")
      })
      script.setExecutable(true)

      LocalFileSystem.getInstance().refreshAndFindFileByIoFile(dir.toFile())?.refresh(true, true)

      val exec = TestExecutable(dir, script, argsLog, stdinLog)
      Disposer.register(registerWith, exec)
      return exec
    }

    private fun escapeForSh(s: String): String =
      s.replace("\\", "\\\\") //
        .replace("$", "\\$") //
        .replace("\"", "\\\"") //
        .replace("\n", "\\n") //
  }
}
