package me.serce.solidity.lang.benchmark

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.core.CoreProjectEnvironment
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFileFactory
import me.serce.solidity.lang.SolidityFileType
import me.serce.solidity.lang.SolidityLanguage
import me.serce.solidity.lang.core.SolidityParserDefinition
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
open class SolidityParserBenchmark {
  private lateinit var ijEnv: IjBenchEnvironment
  private lateinit var psiFileFactory: PsiFileFactory
  private lateinit var sources: List<SolSourceFile>

  @Param("openzeppelin")
  var source: String = "openzeppelin"

  @Setup(Level.Trial)
  fun setUp() {
    ijEnv = IjBenchEnvironment()
    psiFileFactory = PsiFileFactory.getInstance(ijEnv.project)
    sources = loadSources(source)
  }

  @TearDown(Level.Trial)
  fun tearDown() {
    ijEnv.close()
  }

  @Benchmark
  fun parseAllContractsInTheProject(): Int {
    var parsedFiles = 0
    for (sourceFile in sources) {
      val file = psiFileFactory.createFileFromText(
        sourceFile.relativePath,
        SolidityFileType,
        sourceFile.contents,
      )
      if (file.node != null) {
        parsedFiles++
      }
    }
    return parsedFiles
  }

  private fun loadSources(source: String): List<SolSourceFile> {
    if (source == "openzeppelin") {
      val root = ProjectFixtureSupport.prepareOpenZeppelinFixture()
      return ProjectFixtureSupport.loadSoliditySources(root.resolve("contracts"))
    }
    throw IllegalArgumentException("Unsupported benchmark source: $source")
  }

  private class IjBenchEnvironment : AutoCloseable {
    private val disposable: Disposable = Disposer.newDisposable("solidity-benchmark")
    private val applicationEnvironment = CoreApplicationEnvironment(disposable)
    private val projectEnvironment = CoreProjectEnvironment(disposable, applicationEnvironment)

    val project: Project = projectEnvironment.project

    init {
      LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        SolidityLanguage,
        SolidityParserDefinition(),
      )
    }

    override fun close() {
      Disposer.dispose(disposable)
    }
  }
}
