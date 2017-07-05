package me.serce.solidity.lang.types

import com.intellij.openapi.project.Project
import me.serce.solidity.lang.psi.SolPsiFactory

class SolEmbeddedTypeFactory(val project: Project) {
  private val psiFactory: SolPsiFactory = SolPsiFactory(project)

  fun solMessageType(): SolType {
    return SolStruct(psiFactory.createStruct("""
      struct msg {
          bytes data;
          uint gas;
          address sender;
          uint value;
      }
    """))
  }

  fun solTxType(): SolType {
    return SolStruct(psiFactory.createStruct("""
      struct tx {
          uint gasprice;
          address origin;
      }
    """))
  }


  fun solBlockType(): SolType {
    return SolContract(psiFactory.createContract("""
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
