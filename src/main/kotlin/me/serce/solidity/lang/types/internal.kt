package me.serce.solidity.lang.types

import com.intellij.openapi.project.Project
import me.serce.solidity.lang.psi.SolPsiFactory
import org.intellij.lang.annotations.Language

class SolInternalTypeFactory(project: Project) {
  private val psiFactory: SolPsiFactory = SolPsiFactory(project)

  companion object {
    fun of(project: Project): SolInternalTypeFactory {
      return project.getService(SolInternalTypeFactory::class.java)
    }
    const val varargsId = "varargs"
  }

  private val registry: Map<String, SolType> by lazy {
    listOf(
      msgType,
      txType,
      abiType,
      blockType
    ).associateBy { it.toString() }
  }

  fun byName(name: String): SolType? = registry[name]

  val stringType: SolContract by lazy {
     contract("""
       contract ${internalise("String")} {
 							/**
                @return contents of the arguments without padding. 
 							*/
 							function concat(string $varargsId) returns (string memory);
         }
     """)
   }

  val msgType: SolType by lazy {
    contract("""
      contract ${internalise("Msg")} {
          /**
          * complete calldata
          */
          bytes data;
          /**
          * the available gas remaining for a current transaction
          */
          uint gas;
          /**
          * sender of the message (current call)
          */
          address sender;
          /**
          * number of wei sent with the message
          */
          uint value;
          /**
          * first four bytes of the calldata (i.e. function identifier)
          */
          bytes4 sig;
      }
    """)
  }

  val txType: SolType by lazy {
    contract("""
      contract ${internalise("Tx")} {
          /**
          * gas price of the transaction
          */
          uint gasprice;
          /**
          * sender of the transaction (full call chain)
          */
          address origin;
      }
    """)
  }

  val addressType: SolContract by lazy {
    contract("""
        contract ${internalise("Address")} {
            /**
            * send given amount of Wei to Address, reverts on failure, forwards 2300 gas stipend, not adjustable
            */
            function transfer(uint value);
            /**
            * send given amount of Wei to Address, returns false on failure, forwards 2300 gas stipend, not adjustable
            */
            function send(uint value) returns (bool);
            
            /**
            * balance of the Address in Wei
            */
            uint256 balance;
            
            /**
            * code at the Address (can be empty)
            */
            bytes memory code;
            
            /**
            * the codehash of the Address
            */
            bytes32 codehash;
            
            /**
            * issue low-level CALL with the given payload, returns success condition and return data, forwards all available gas, adjustable
            */
            function call(bytes memory) returns (bool, bytes memory)
            
            /**
            * issue low-level DELEGATECALL with the given payload, returns success condition and return data, forwards all available gas, adjustable
            */
            
            function delegatecall(bytes memory) returns (bool, bytes memory)
            /**
            * issue low-level STATICCALL with the given payload, returns success condition and return data, forwards all available gas, adjustable
            */
            function staticcall(bytes memory) returns (bool, bytes memory)
            
            }
    """)
  }

  val abiType: SolContract by lazy {
      contract("""
        contract ${internalise("Abi")} {
            /**
            * ABI-decodes the given data, while the types are given in parentheses as second argument. Example: (uint a, uint[2] memory b, bytes memory c) = abi.decode(data, (uint, uint[2], bytes))
            * @custom:no_validation
            */
            function decode(bytes memory encodedData) returns (data); 
            /**
            * ABI-encodes the given arguments
            * @custom:no_validation
            */        
            function encode(data) returns (bytes memory);
            /**
            * Performs packed encoding of the given arguments. Note that packed encoding can be ambiguous! 
            * @custom:no_validation
            */        
            function encodePacked(data) returns (bytes memory); 
            /**
            * ABI-encodes the given arguments starting from the second and prepends the given four-byte selector
            * @custom:no_validation
            */        
            function encodeWithSelector(bytes4 selector) returns (bytes memory);
            /**
            * Equivalent to abi.encodeWithSelector(bytes4(keccak256(bytes(signature))), ...)
            * @custom:no_validation
            */        
            function encodeWithSignature(string memory signature) returns (bytes memory);
            /**
            * ABI-encodes a call to functionPointer with the arguments found in the tuple. Performs a full type-check, ensuring the types match the function signature. Result equals abi.encodeWithSelector(functionPointer.selector, (...))                   
            * @custom:no_validation
            */        
            function encodeCall(functionPointer) returns (bytes memory);
            }
      """)
    }


  val arrayType: SolContract by lazy {
    contract("""
      contract ${internalise("Array")} {
            /**
            * yields the fixed length of the byte array. The length of memory arrays is fixed (but dynamic, i.e. it can depend on runtime parameters) once they are created.
            */
          uint256 length;
          
            /**
            * Dynamic storage arrays and <code>bytes</code> (not <code>string</code>) have a member function called <code>push()</code> that you can use to append a zero-initialised element at the end of the array. It returns a reference to the element, so that it can be used like <code>x.push().t = 2</code> or <code>x.push() = b</code>.
            */
          function push() returns (Type);
            /**
            * Dynamic storage arrays and <code>bytes</code> (not <code>string</code>) have a member function called <code>push(x)</code> that you can use to append a given element at the end of the array. The function returns nothing.
            */
          function push(Type value);
            /**
            * Dynamic storage arrays and <code>bytes</code> (not <code>string</code>) have a member function called <code>pop()</code> that you can use to remove an element from the end of the array. This also implicitly calls delete on the removed element. The function returns nothing.            
            */
          function pop() returns (Type);
      }
    """)
  }

  val blockType: SolType by lazy {
    contract("""
        contract ${internalise("Block")}{
            /**
            * current block’s base fee (<a href="https://eips.ethereum.org/EIPS/EIP-3198">EIP-3198</a> and <a href="https://eips.ethereum.org/EIPS/EIP-1559">EIP-1559</a>)
            */
             uint basefee;       
            /**
            * current block’s blob base fee (<a href="https://eips.ethereum.org/EIPS/EIP-7516">EIP-7516</a> and <a href="https://eips.ethereum.org/EIPS/EIP-4844">EIP-4844</a>)
            */
             uint blobbasefee;        
            /**
            * current chain id
            */
             uint chainid;
            /**
            * current block miner’s address
            */
             address coinbase;
            /**
            * current block difficulty
            */
             uint difficulty;
            /**
            * current block gaslimit
            */
             uint gaslimit;
            /**
            * random number provided by the beacon chain (EVM >= Paris) (see <a href="https://eips.ethereum.org/EIPS/EIP-4399">EIP-4399</a> )
            */
             uint prevrandao;
            /**
            * current block number
            */
             uint number;
            /**
            * current block timestamp as seconds since unix epoch
            */
             uint timestamp;
             
             /**
             * hash of the given block when blocknumber is one of the 256 most recent blocks; otherwise returns zero
             */
             function blockhash(uint blockNumber) returns (bytes32);
             
             /**
             * versioned hash of the index-th blob associated with the current transaction. A versioned hash consists of a single byte representing the version (currently 0x01), followed by the last 31 bytes of the SHA256 hash of the KZG commitment (<a href="https://eips.ethereum.org/EIPS/EIP-4844">EIP-4844</a>).
             */
             function blobhash(uint blockNumber) returns (bytes32);

        }      
    """)
  }

  val metaType: SolContract by lazy {
    contract("""
      /**
      */
      contract ${internalise("MetaType")} {
							/**
                The name of the contract
							*/
							string name;
							/**
                the creation bytecode of the contract. This can be used in inline assembly to build custom creation routines, especially by using the <code>create2</code> opcode. This property can not be accessed in the contract itself or any derived contract. It causes the bytecode to be included in the bytecode of the call site and thus circular references like that are not possible.
							*/
							bytes memory creationCode;
							/**
                the runtime bytecode of the contract. This is the code that is usually deployed by the constructor of <code>C</code>. If <code>C</code> has a constructor that uses inline assembly, this might be different from the actually deployed bytecode. Also note that libraries modify their runtime bytecode at time of deployment to guard against regular calls. The same restrictions as with <code>.creationCode</code> also apply for this property.
							*/
							bytes memory runtimeCode;

							/**
                 the <a href="https://eips.ethereum.org/EIPS/eip-165">EIP-165</a> interface identifier of the given interface <code>I</code>. This identifier is defined as the <code>XOR</code> of all function selectors defined within the interface itself - excluding all inherited functions.
							*/
							bytes4 interfaceId;

							/**
                the smallest value representable by type <code>T</code>.
							*/
							T min;
							/**
                the largest value representable by type <code>T</code>.
							*/
							T max;
      }
    """)
  }

  val functionType: SolContract by lazy {
    contract("""
      /**
      */
      contract ${internalise("FunctionType")} {
							/**
                 the address of the contract of the function.
							*/
							address __address;
							/**
                the ABI function selector
							*/
							bytes4 selector;
      }
    """)
  }


  val globalType: SolContract by lazy {
    contract("""
      contract Global {
          $blockType block;
          $msgType msg;
          $abiType abi;
          $txType tx;

          /**
          * causes a Panic error and thus state change reversion if the condition is not met - to be used for internal errors.
          */
          function assert(bool condition);
          /**
          * reverts if the condition is not met - to be used for errors in inputs or external components.
          */
          function require(bool condition);
          /**
          * reverts if the condition is not met - to be used for errors in inputs or external components. Also provides an error message.
          */
          function require(bool condition, string message);
          /**
          * abort execution and revert state changes
          */
          function revert();
          /**
          * abort execution and revert state changes, providing an explanatory string
          */
          function revert(string memory reason);
          /**
          * compute the Keccak-256 hash of the input
          */
          function keccak256(bytes memory input) returns (bytes32);
          /**
          * compute the SHA-256 hash of the input
          */
          function sha3(bytes memory input) returns (bytes32);
          /**
          * compute the SHA-256 hash of the input
          */
          function sha256(bytes memory input) returns (bytes32);
          /**
          * compute RIPEMD-160 hash of the input
          */
          function ripemd160(bytes memory input) returns (bytes20);
          /**
          * recover the address associated with the public key from elliptic curve signature or return zero on error. The function parameters correspond to ECDSA values of the signature:
          * 
          * r = first 32 bytes of signature
          * 
          * s = second 32 bytes of signature
          * 
          * v = final 1 byte of signature
          * 
          * ecrecover returns an address, and not an address payable. See address payable for conversion, in case you need to transfer funds to the recovered address.
          * 
          * For further details, read example usage.
          */
          function ecrecover(bytes32 hash, uint8 v, bytes32 r, bytes32 s) returns (address);
          /**
          * compute <code>(x + y) % k</code> where the addition is performed with arbitrary precision and does not wrap around at <code>2**256</code>. Assert that <code>k != 0</code> starting from version 0.5.0.
          */
          function addmod(uint x, uint y, uint k) returns (uint);
          /**
          * compute <code>(x * y) % k</code> where the multiplication is performed with arbitrary precision and does not wrap around at <code>2**256</code>. Assert that <code>k != 0</code> starting from version 0.5.0.
          */
          function mulmod(uint x, uint y, uint k) returns (uint);
          /**
          * Destroy the current contract, sending its funds to the given Address and end execution. Note that selfdestruct has some peculiarities inherited from the EVM:
          * 
          * <ul>
          * <li>the receiving contract’s receive function is not executed.</li>
          * <li>the contract is only really destroyed at the end of the transaction and revert s might “undo” the destruction.</li>
          * </ul>
          * 
          * Furthermore, all functions of the current contract are callable directly including the current function.
          */
          function selfdestruct(address recipient);
          
          
          /**
          * Returns the amount of gas remaining the current transaction.
          */
          function gasleft() returns (uint64);
          
          /**
          * Returns hash of the given block when blocknumber is one of the 256 most recent blocks; otherwise returns zero
          */
          function blockhash(uint blockNumber) returns (bytes32);
      }
    """)
  }

  private fun contract(@Language("Solidity") contractBody: String) =
    SolContract(psiFactory.createContract(contractBody), true)
}
