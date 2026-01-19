package me.serce.solidity.lang.benchmark

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Comparator

/**
 * Support for preparing realistic solidity projects for benchmarks.
 */
object ProjectFixtureSupport {
  data class ExternalRepo(
    val name: String,
    val url: String,
    val ref: String,
  )

  private val registeredRepos: MutableMap<String, ExternalRepo> = linkedMapOf(
    "openzeppelin" to ExternalRepo(
      name = "openzeppelin-contracts",
      url = "https://github.com/OpenZeppelin/openzeppelin-contracts.git",
      ref = "v5.5.0",
    ),
  )

  fun prepareExternalRepo(key: String): Path {
    val repo = registeredRepos[key]
      ?: throw IllegalArgumentException("Unknown external repo key: $key")
    val checkoutName = "${repo.name}-${repo.ref}"
    return prepareGitFixture(checkoutName, repo.url, repo.ref)
  }

  fun prepareOpenZeppelinFixture(): Path = prepareExternalRepo("openzeppelin")

  fun generateSyntheticProject(
    fileCount: Int,
    importFanout: Int,
    statementCount: Int,
    includeRootImport: Boolean,
    includeInheritance: Boolean,
  ): Path {
    val baseDir = Files.createTempDirectory("solidity-synthetic-bench")
    val contractsDir = baseDir.resolve("contracts")
    Files.createDirectories(contractsDir)

    for (i in 0 until fileCount) {
      val imports = mutableListOf<Int>()
      if (includeRootImport && i != 0) {
        imports.add(0)
      }
      if (i == 0) {
        imports.addAll((1 until minOf(fileCount, importFanout + 1)))
      } else {
        imports.addAll((1..importFanout).mapNotNull { offset ->
          val next = i + offset
          if (next < fileCount) next else null
        })
      }
      val importLines = imports.distinct().joinToString("\n") { "import \"contracts/File$it.sol\";" }
      val statements = buildString {
        repeat(statementCount) { idx ->
          append("x = x + $idx;")
          append('\n')
        }
      }
      val inheritance = if (includeInheritance && i != 0) "is C0 " else ""
      val content = """
        pragma solidity ^0.8.0;
        $importLines
        contract C$i $inheritance{
          uint256 x;
          function f() public {
            $statements
          }
        }
      """.trimIndent()
      Files.writeString(contractsDir.resolve("File$i.sol"), content)
    }

    return baseDir
  }

  fun loadSoliditySources(contractsRoot: Path): List<SolSourceFile> {
    if (!Files.isDirectory(contractsRoot)) {
      throw IOException("Contracts directory does not exist: $contractsRoot")
    }
    val sources = Files.walk(contractsRoot).use { paths ->
      paths
        .filter { Files.isRegularFile(it) && it.toString().endsWith(".sol") }
        .map { path ->
          val content = Files.readString(path)
          val relativePath = contractsRoot.relativize(path).toString()
          SolSourceFile(relativePath, content)
        }
        .toList()
    }
    if (sources.isEmpty()) {
      throw IOException("No Solidity sources found under: $contractsRoot")
    }
    return sources
  }

  private fun prepareGitFixture(name: String, url: String, ref: String): Path {
    val baseDir = Paths.get(System.getProperty("java.io.tmpdir"), "intellij-solidity-fixtures")
    Files.createDirectories(baseDir)
    val checkoutDir = baseDir.resolve(name)
    if (Files.isDirectory(checkoutDir.resolve(".git"))) {
      return checkoutDir
    }
    if (Files.exists(checkoutDir)) {
      deleteDir(checkoutDir)
    }
    runCommand(baseDir, "git", "clone", "--depth", "1", "--branch", ref, url, checkoutDir.toString())
    return checkoutDir
  }

  private fun runCommand(workDir: Path, vararg command: String) {
    val builder = ProcessBuilder(*command)
    builder.directory(workDir.toFile())
    builder.redirectErrorStream(true)
    val process = builder.start()
    val output = StringBuilder()
    BufferedReader(InputStreamReader(process.inputStream, StandardCharsets.UTF_8)).use { reader ->
      reader.forEachLine { line ->
        output.append(line).append(System.lineSeparator())
      }
    }
    val exitCode = process.waitFor()
    if (exitCode != 0) {
      throw IOException(
        "Command failed ($exitCode): ${command.joinToString(" ")}\n$output"
      )
    }
  }

  private fun deleteDir(path: Path) {
    Files.walk(path)
      .sorted(Comparator.reverseOrder())
      .forEach { entry ->
        Files.delete(entry)
      }
  }
}

data class SolSourceFile(
  val relativePath: String,
  val contents: String,
)
