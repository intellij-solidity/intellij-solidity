package me.serce.solidity.lang.types

import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import me.serce.solidity.lang.psi.SolPsiFactory

class SolEmbeddedTypeFactory(project: Project) : AbstractProjectComponent(project) {
  private val psiFactory: SolPsiFactory = SolPsiFactory(project)

  companion object {
    fun of(project: Project): SolEmbeddedTypeFactory {
      return project.getComponent(SolEmbeddedTypeFactory::class.java)
    }
  }

  val messageType: SolType by lazy {
    SolStruct(psiFactory.createStruct("""
      struct msg {
          bytes data;
          uint gas;
          address sender;
          uint value;
      }
    """))
  }

  val txType: SolType by lazy {
    SolStruct(psiFactory.createStruct("""
      struct tx {
          uint gasprice;
          address origin;
      }
    """))
  }


  val blockType: SolType by lazy {
    SolContract(psiFactory.createContract("""
      contract block {
          address coinbase;
          uint difficulty;
          uint gaslimit;
          uint number;
          uint timestamp;

          function blockhash(uint blockNumber) returns (bytes32) {
          }
      }
    """))
  }
}
