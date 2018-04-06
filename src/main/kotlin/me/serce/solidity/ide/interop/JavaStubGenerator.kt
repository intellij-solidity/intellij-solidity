package me.serce.solidity.ide.interop

import me.serce.solidity.lang.psi.SolContractDefinition

object JavaStubGenerator {
  class ContractStubTemplate(className: String, methodName: String) {
    val code =
      "package stubs;\n\n" +
        "import org.ethereum.util.blockchain.SolidityContract;\n\n" +
        "public class $className {\n" +
        "\tprivate final SolidityContract contract;\n" +
        "\t$className(SolidityContract contract) {\n" +
        "\t\tthis.contract = contract; \n" +
        "\t}\n" +
        "\tpublic Object $methodName() {\n" +
        "\t\treturn contract.callFunction(\"$methodName\").getReturnValue();\n" +
        "\t}\n" +
        "}"
  }

  fun convert(contract: SolContractDefinition): String {
    return ContractStubTemplate(contract.name!!, contract.functionDefinitionList.first()!!.name!!).code
  }

  fun generateRepo(contracts: List<SolContractDefinition> ) : String {
    return ""
  }
}
