package me.serce.solidity.ide.interop

import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.run.SolContractMetadata

object JavaStubGenerator {
  const val packageName = "stubs" // only a single level package is supported currently in generator
  const val repoClassName = "ContractsRepository"


  private fun contractStubTemplate(className: String, functions: List<SolFunctionDefinition>): String {
    return "package $packageName;\n\n" +
        "import org.ethereum.util.blockchain.SolidityContract;\n\n" +
        "public class $className {\n" +
        "\tprivate final SolidityContract contract;\n" +
        "\t$className(SolidityContract contract) {\n" +
        "\t\tthis.contract = contract; \n" +
        "\t}\n" +
      functions.joinToString("\n") { funcStubTemplate(it) } +
      "}"
  }

  private fun funcStubTemplate(function: SolFunctionDefinition): String {
    val params = stringifyParams(function)
    val paramNames = if (function.parameters.isNotEmpty()) {
      ", " + function.parameters.map { it.name }.joinToString(", ")
    } else ""
    val methodName = function.name
    return "" +
      "\tpublic Object $methodName($params) {\n" +
      "\t\treturn contract.callFunction(\"$methodName\"$paramNames).getReturnValue();\n" +
      "\t}\n"
  }

  private fun contractRepoTemplate(contracts: List<CompiledContractDefinition>): String {
    return "package $packageName;\n" +
      "\n" +
      "import org.ethereum.config.SystemProperties;\n" +
      "import org.ethereum.config.blockchain.FrontierConfig;\n" +
      "import org.ethereum.solidity.compiler.CompilationResult;\n" +
      "import org.ethereum.util.blockchain.EasyBlockchain;\n" +
      "import org.ethereum.util.blockchain.StandaloneBlockchain;\n" +
      "\n" +
      "import java.math.BigInteger;\n" +
      "\n" +
      "public class $repoClassName {\n" +
      "    private EasyBlockchain blockchain;\n" +
      "    private $repoClassName(EasyBlockchain blockchain) {\n" +
      "        this.blockchain = blockchain;\n" +
      "    }\n" +
      "    \n" +
      "    private static StandaloneBlockchain defaultBlockchain;\n" +
      "\n" +
      "    public static $repoClassName getInstance(EasyBlockchain blockchain) {\n" +
      "        return new $repoClassName(blockchain);\n" +
      "    }\n" +
      "\n" +
      "    public static synchronized $repoClassName getInstance() {\n" +
      "        if (defaultBlockchain == null) {\n" +
      "            defaultBlockchain = initDefaultBlockchain();    \n" +
      "        }\n" +
      "        return new $repoClassName(defaultBlockchain);\n" +
      "    }\n" +
      "\n" +
      "    private static StandaloneBlockchain initDefaultBlockchain() {\n" +
      "        SystemProperties.getDefault().setBlockchainConfig(new FrontierConfig(new FrontierConfig.FrontierConstants() {\n" +
      "            @Override\n" +
      "            public BigInteger getMINIMUM_DIFFICULTY() {\n" +
      "                return BigInteger.ONE;\n" +
      "            }\n" +
      "        }));\n" +
      "        StandaloneBlockchain blockchain = new StandaloneBlockchain().withAutoblock(true);\n" +
      "        System.out.println(\"Creating first empty block (need some time to generate DAG)...\");\n" +
      "        blockchain.createBlock();\n" +
      "        System.out.println(\"Done.\");\n" +
      "        return blockchain;\n" +
      "    }\n" +
      "\n" +
      "    public EasyBlockchain getBlockchain() {\n" +
      "        return blockchain;\n" +
      "    }\n" +
      "\n" +
      contracts.joinToString("\n") { submitContractTemplate(it) } +
      "}"
  }

  private fun submitContractTemplate(contract: CompiledContractDefinition): String {
    val name = contract.contract.name
    val definition = contract.contract
    val params = stringifyParams(definition.functionDefinitionList.find { it.isConstructor })
    return "    public $name submit$name($params) {\n" +
      "        CompilationResult.ContractMetadata metadata = new CompilationResult.ContractMetadata();\n" +
      "        try {\n" +
      "           metadata.abi = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(\"${contract.metadata.abiFile.absolutePath.replace("\\", "\\\\")}\")));\n" +
      "           metadata.bin = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(\"${contract.metadata.binFile.absolutePath.replace("\\", "\\\\")}\")));\n" +
      "        } catch (Exception e) {\n" +
      "           throw new RuntimeException(e);\n" +
      "        }\n" +
      "        return new $name(blockchain.submitNewContract(metadata));\n" +
      "    }\n"
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
