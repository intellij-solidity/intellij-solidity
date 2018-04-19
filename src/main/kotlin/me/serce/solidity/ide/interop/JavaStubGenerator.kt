package me.serce.solidity.ide.interop

import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.run.SolContractMetadata

object JavaStubGenerator {
  const val packageName = "stubs" // only a single level package is supported currently in generator
  const val repoClassName = "ContractsRepository"

  const val autoGenComment = "// Auto-generated code"


  private fun contractStubTemplate(className: String, functions: List<SolFunctionDefinition>): String =
    """$autoGenComment
package $packageName;

import org.ethereum.util.blockchain.SolidityContract;

public class $className {
	private final SolidityContract contract;
	$className(SolidityContract contract) {
		this.contract = contract;
	}
${functions.joinToString("\n") { funcStubTemplate(it) }}
}"""

  private fun funcStubTemplate(function: SolFunctionDefinition): String {
    val params = stringifyParams(function)
    val paramRefs = paramRefs(function)
    val methodName = function.name
    return """
  public Object $methodName($params) {
		return contract.callFunction("$methodName"$paramRefs).getReturnValue();
	}
"""
  }

  private fun paramRefs(function: SolFunctionDefinition?): String {
    val parameters = function?.parameters ?: return ""
    return if (parameters.isNotEmpty()) {
      ", " + parameters.map { it.name }.joinToString(", ")
    } else ""
  }

  private fun contractRepoTemplate(contracts: List<CompiledContractDefinition>): String =
    """$autoGenComment
package $packageName;

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
           metadata.abi = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("${contract.metadata.abiFile.absolutePath.replace("\\", "\\\\")}")));
           metadata.bin = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("${contract.metadata.binFile.absolutePath.replace("\\", "\\\\")}")));
        } catch (Exception e) {
           throw new RuntimeException(e);
        }
        return new $name(blockchain.submitNewContract(metadata$paramRefs));
    }
"""
  }

  private fun stringifyParams(function: SolFunctionDefinition?) =
    function?.parameters?.joinToString(", ") { "${SolToJavaTypeConverter.convert(it.typeName)} ${it.name}" } ?: ""

  fun convert(contract: SolContractDefinition): String {
    return contractStubTemplate(contract.name!!, contract.functionDefinitionList.filter { !it.isConstructor })
  }

  fun generateRepo(contracts: List<CompiledContractDefinition>): String {
    return contractRepoTemplate(contracts)
  }

  data class CompiledContractDefinition(val metadata: SolContractMetadata, val contract: SolContractDefinition)
}
