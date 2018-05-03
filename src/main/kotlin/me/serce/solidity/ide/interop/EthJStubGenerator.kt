package me.serce.solidity.ide.interop

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import me.serce.solidity.ide.settings.SoliditySettings
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolFunctionDefinition
import java.io.ByteArrayOutputStream
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.zip.GZIPOutputStream

object EthJStubGenerator : Sol2JavaGenerator {

  override fun generate(project: Project, dir: VirtualFile, contracts: List<CompiledContractDefinition>) {
    val output = WriteCommandAction.runWriteCommandAction(project, Computable {
      VfsUtil.createDirectoryIfMissing(dir, SoliditySettings.instance.basePackage.replace(".", "/"))
    }).path
    runReadAction {
      val repo = EthJStubGenerator.generateRepo(contracts)
      writeClass(output, repoClassName, repo)
      contracts.map { it.contract }.forEach {
        writeClass(output, it.name!!, convert(it))
      }
    }
  }

  private const val repoClassName = "ContractsRepository"

  private const val autoGenComment = "// This is a generated file. Not intended for manual editing."

  private fun getPackageName(): String = SoliditySettings.instance.basePackage

  private val objNames = Any::class.java.declaredMethods.asSequence()
    .filter { !Modifier.isPrivate(it.modifiers) }
    .map { it.name }
    .toSet()

  private fun contractStubTemplate(className: String, functions: List<SolFunctionDefinition>): String =
    """$autoGenComment
package ${getPackageName()};

import org.ethereum.util.blockchain.SolidityContract;

public class $className {
	private final SolidityContract contract;
	$className(SolidityContract contract) {
		this.contract = contract;
	}
${functions.filter { it.name != null }.joinToString("\n") { funcStubTemplate(it) }}
}"""

  private fun funcStubTemplate(function: SolFunctionDefinition): String {
    val params = stringifyParams(function)
    val paramRefs = paramRefs(function)
    val methodName = function.name
    return """
  public Object ${methodName(methodName)}($params) {
		return contract.callFunction("$methodName"$paramRefs).getReturnValue();
	}
"""
  }

  private fun methodName(name: String?) =
    if (objNames.contains(name)) "_$name" else name

  private fun paramRefs(function: SolFunctionDefinition?): String {
    val parameters = function?.parameters ?: return ""
    return if (parameters.isNotEmpty()) {
      ", " + parameters.map { it.name }.joinToString(", ")
    } else ""
  }

  private fun contractRepoTemplate(contracts: List<CompiledContractDefinition>): String =
    """$autoGenComment
package ${getPackageName()};

import org.ethereum.config.SystemProperties;
import org.ethereum.config.blockchain.FrontierConfig;
import org.ethereum.solidity.compiler.CompilationResult;
import org.ethereum.util.blockchain.EasyBlockchain;
import org.ethereum.util.blockchain.StandaloneBlockchain;

import java.math.BigInteger;

public class $repoClassName {
  private EasyBlockchain blockchain;
  private $repoClassName(EasyBlockchain blockchain) {
      this.blockchain = blockchain;
  }

  private static StandaloneBlockchain defaultBlockchain;

  public static $repoClassName getInstance(EasyBlockchain blockchain) {
      return new $repoClassName(blockchain);
  }

  public static synchronized $repoClassName getInstance() {
      if (defaultBlockchain == null) {
          defaultBlockchain = initDefaultBlockchain();
      }
      return new $repoClassName(defaultBlockchain);
  }

  private static StandaloneBlockchain initDefaultBlockchain() {
      SystemProperties.getDefault().setBlockchainConfig(new FrontierConfig(new FrontierConfig.FrontierConstants() {
          @Override
          public BigInteger getMINIMUM_DIFFICULTY() {
              return BigInteger.ONE;
          }
      }));
      StandaloneBlockchain blockchain = new StandaloneBlockchain().withAutoblock(true);
      System.out.println("Creating first empty block (need some time to generate DAG)...");
      blockchain.createBlock();
      System.out.println("Done.");
      return blockchain;
  }

  public EasyBlockchain getBlockchain() {
      return blockchain;
  }

  private static String unzip(String data) throws java.io.IOException {
    byte[] bytes = java.util.Base64.getDecoder().decode(data);
    try (java.util.zip.GZIPInputStream inputStream = new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(bytes));
         java.io.ByteArrayOutputStream result = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString("UTF-8");
    }
  }

${contracts.joinToString("\n") { submitContractTemplate(it) }}
}"""

  private fun submitContractTemplate(contract: CompiledContractDefinition): String {
    val name = contract.contract.name
    val definition = contract.contract
    val constructor = definition.functionDefinitionList.find { it.isConstructor }
    val params = stringifyParams(constructor)
    val paramRefs = paramRefs(constructor)
    return """
    public $name submit$name($params) {
        CompilationResult.ContractMetadata metadata = new CompilationResult.ContractMetadata();
        try {
           metadata.abi = unzip("${zip(contract.metadata.abi)}");
           metadata.bin = unzip("${zip(contract.metadata.bin)}");
        } catch (Exception e) {
           throw new RuntimeException(e);
        }
        return new $name(blockchain.submitNewContract(metadata$paramRefs));
    }
"""
  }

  private fun zip(content: String): String {
    val baos = ByteArrayOutputStream()
    GZIPOutputStream(baos).bufferedWriter().use { it.write(content) }
    return Base64.getEncoder().encodeToString(baos.toByteArray())
  }

  private fun stringifyParams(function: SolFunctionDefinition?): String {
    var paramCounter = 0
    return function?.parameters?.joinToString(", ") {
      "${EthJTypeConverter.convert(it.typeName)} ${it.name ?: "param${paramCounter++}"}"
    } ?: ""
  }

  private fun convert(contract: SolContractDefinition): String {
    return contractStubTemplate(contract.name!!, contract.functionDefinitionList.filter { !it.isConstructor })
  }

  private fun generateRepo(contracts: List<CompiledContractDefinition>): String {
    return contractRepoTemplate(contracts)
  }

  private fun writeClass(stubsDir: String, className: String, content: String) {
    Files.write(Paths.get(stubsDir, "$className.java"), content.toByteArray())
  }

}
