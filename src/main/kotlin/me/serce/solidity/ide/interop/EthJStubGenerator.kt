package me.serce.solidity.ide.interop

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import me.serce.solidity.ide.settings.SoliditySettings
import org.web3j.protocol.core.methods.response.AbiDefinition
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
      contracts.forEach {
        writeClass(output, it.contract.name!!, convert(it))
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

  private fun contractStubTemplate(className: String, functions: List<AbiDefinition>): String =
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

  private fun funcStubTemplate(function: AbiDefinition): String {
    val params = stringifyParams(function)
    val paramRefs = paramRefs(function)
    val methodName = function.name
    val firstOutput = function.outputs.firstOrNull()
    val hasReturn = firstOutput != null
    var returnType = if (hasReturn) EthJTypeConverter.convert(firstOutput!!.type, lax = true) else "void"
    if (returnType.contains("BigInteger[]")) {
      // EthJ always returns an object array if an int array defined for output. Bug?
      returnType = returnType.replaceBefore("[", "Object")
    }
    return """
  public $returnType ${methodName(methodName)}($params) {
		${if (hasReturn) "return ($returnType) " else ""}contract.callFunction("$methodName"$paramRefs)${if (hasReturn) ".getReturnValue()" else ""};
	}
"""
  }

  private fun methodName(name: String?) =
    if (objNames.contains(name)) "_$name" else name

  private fun paramRefs(function: AbiDefinition?): String {
    val parameters = function?.inputs ?: return ""
    return if (parameters.isNotEmpty()) {
      ", " + parameters.joinToString(", ") { "(Object)${it.name}" }
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
    val constructor = contract.abis.find { it.isConstructor() }
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

  private fun stringifyParams(function: AbiDefinition?): String {
    var paramCounter = 0
    return function?.inputs?.joinToString(", ") {
      "${EthJTypeConverter.convert(it.type)} ${it.name ?: "param${paramCounter++}"}"
    } ?: ""
  }

  private fun convert(contract: CompiledContractDefinition): String {
    return contractStubTemplate(contract.contract.name!!, contract.abis.filter { !it.isConstructor() })
  }

  private fun generateRepo(contracts: List<CompiledContractDefinition>): String {
    return contractRepoTemplate(contracts)
  }

  private fun writeClass(stubsDir: String, className: String, content: String) {
    Files.write(Paths.get(stubsDir, "$className.java"), content.toByteArray())
  }

}
