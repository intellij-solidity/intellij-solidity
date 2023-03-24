package me.serce.solidity.lang.types

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import me.serce.solidity.lang.psi.SolPsiFactory
import org.intellij.lang.annotations.Language

class SolInternalTypeFactory(project: Project) {
  private val psiFactory: SolPsiFactory = SolPsiFactory(project)

  companion object {
    fun of(project: Project): SolInternalTypeFactory {
      return ServiceManager.getService(project, SolInternalTypeFactory::class.java)
    }
  }

  private val registry: Map<String, SolType> by lazy {
    listOf(
      msgType,
      txType,
      blockType
    ).associateBy { it.toString() }
  }

  fun byName(name: String): SolType? = registry[name]

  val msgType: SolType by lazy {
    struct("""
      struct ${internalise("Msg")} {
          bytes data;
          uint gas;
          address sender;
          uint value;
      }
    """)
  }

  val txType: SolType by lazy {
    struct("""
      struct ${internalise("Tx")} {
          uint gasprice;
          address origin;
      }
    """)
  }

  val addressType: SolContract by lazy {
    contract("""
            contract ${internalise("Address")} {
                function transfer(uint value);
                
                function send(uint value) returns (bool);
            }
          """)
  }


  val arrayType: SolContract by lazy {
    contract("""
      contract ${internalise("Array")} {
          uint256 length;
          
          function push();
          function push(uint value);
          function pop();
      }
    """)
  }

  val blockType: SolType by lazy {
    contract("""
        contract ${internalise("Block")}{
             address coinbase;
             uint difficulty;
             uint gasLimit;
             uint number;
             uint timestamp;
             
             function blockhash(uint blockNumber) returns (bytes32);

        }      
    """)
  }

  val globalType: SolContract by lazy {
    contract("""
      contract Global {
          $blockType block;
          $msgType msg;
          $txType tx;
          uint now;

          function assert(bool condition) private {}
          function require(bool condition) private {}
          function require(bool condition, string message) private {}
          function revert() private {}
          function revert(string) {}
          function keccak256() returns (bytes32) private {}
          function sha3() returns (bytes32) private {}
          function sha256() returns (bytes32) private {}
          function ripemd160() returns (bytes20) private {}
          function ecrecover(bytes32 hash, uint8 v, bytes32 r, bytes32 s) returns (address) private {}
          function addmod(uint x, uint y, uint k) returns (uint) private {}
          function mulmod(uint x, uint y, uint k) returns (uint) private returns (uint) {}
          function selfdestruct(address recipient) private {};
      }
    """)
  }

  private fun contract(@Language("Solidity") contractBody: String) =
    SolContract(psiFactory.createContract(contractBody), true)

  private fun struct(@Language("Solidity") contractBody: String) =
    SolStruct(psiFactory.createStruct(contractBody), true)

}
