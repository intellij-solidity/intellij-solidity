package me.serce.solidity.lang.benchmark

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.core.CoreProjectEnvironment
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import me.serce.solidity.lang.SolidityFileType
import me.serce.solidity.lang.SolidityLanguage
import me.serce.solidity.lang.core.SolidityParserDefinition
import me.serce.solidity.lang.psi.SolVarLiteral
import me.serce.solidity.lang.resolve.SolResolver
import me.serce.solidity.lang.types.SolInternalTypeFactory
import org.openjdk.jmh.annotations.*
import java.nio.file.Path
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 1)
@Measurement(iterations = 3)
open class SolidityResolveBenchmark {
  private lateinit var ijEnv: IjBenchEnvironment
  private lateinit var rootVarLiteral: SolVarLiteral
  private lateinit var baseDir: Path

  @Param("200")
  var fileCount: Int = 200

  @Param("4")
  var importFanout: Int = 4

  @Param("200")
  var statementCount: Int = 200

  @Setup(Level.Trial)
  fun setUpTrial() {
    ijEnv = IjBenchEnvironment()
  }

  @Setup(Level.Iteration)
  fun setUpIteration() {
    baseDir = ProjectFixtureSupport.generateSyntheticProject(
      fileCount = fileCount,
      importFanout = importFanout,
      statementCount = statementCount,
      includeRootImport = false,
      includeInheritance = false,
    )
    val contractsDir = baseDir.resolve("contracts")

    val rootPath = contractsDir.resolve("File0.sol")
    val vFile = VfsUtil.findFileByIoFile(rootPath.toFile(), true)
      ?: error("Failed to find root file: $rootPath")
    val rootPsiFile = PsiManager.getInstance(ijEnv.project).findFile(vFile)
      ?: error("Failed to create PSI for: $vFile")
    rootVarLiteral = PsiTreeUtil.findChildrenOfType(rootPsiFile, SolVarLiteral::class.java).firstOrNull()
      ?: error("Expected a var literal in root file")
  }

  @TearDown(Level.Iteration)
  fun tearDownIteration() {
    baseDir.toFile().deleteRecursively()
  }

  @TearDown(Level.Trial)
  fun tearDownTrial() {
    ijEnv.close()
  }

  @Benchmark
  fun lexicalDeclarationsOnSyntheticProject(): Int {
    return SolResolver.resolveVarLiteral(rootVarLiteral).count()
  }

  private class IjBenchEnvironment : AutoCloseable {
    private val disposable: Disposable = Disposer.newDisposable("solidity-lexical-benchmark")
    private val applicationEnvironment = CoreApplicationEnvironment(disposable)
    private val projectEnvironment = CoreProjectEnvironment(disposable, applicationEnvironment)

    val project: Project = projectEnvironment.project

    init {
      applicationEnvironment.registerFileType(SolidityFileType, SolidityFileType.defaultExtension)
      LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        SolidityLanguage,
        SolidityParserDefinition(),
      )
      (project as MockProject).registerService(
        SolInternalTypeFactory::class.java,
        SolInternalTypeFactory(project),
      )
    }

    override fun close() {
      Disposer.dispose(disposable)
    }
  }
}
