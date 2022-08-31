// https://docs.soliditylang.org/en/v0.8.15/units-and-global-variables.html

abstract contract Block {
    /**
    Current block’s base fee (EIP-3198 and EIP-1559)
    */
    uint basefee;

    /**
    Current chain id
    */
    uint chainid;

    /**
    Current block miner’s address
    */
    address payable coinbase;

    /**
    Current block difficulty
    */
    uint difficulty;

    /**
    Current block gaslimit
    */
    uint gaslimit;

    /**
    Current block number
    */
    uint number;

    /**
    Current block timestamp as seconds since unix epoch
    */
    uint timestamp;
}

abstract contract Msg {
    /**
    Complete calldata
    */
    bytes data;

    /**
    Sender of the message (current call)
    */
    address sender;

    /**
    First four bytes of the calldata (i.e. function identifier)
    */
    bytes4 sig;

    /**
    Number of wei sent with the message
    */
    uint value;
}

abstract contract Tx {
    /**
    Gas price of the transaction
    */
    uint gasprice;

    /**
    Sender of the transaction (full call chain)
    */
    address origin;
}

abstract contract Abi {
    /**
    ABI-decodes the given data, while the types are given in parentheses as second argument.
    Example: (uint a, uint[2] memory b, bytes memory c) = abi.decode(data, (uint, uint[2], bytes))
    */
    function decode(bytes memory encodedData /* , ... */) public virtual /* returns ( ... ) */;

    /**
    ABI-encodes the given arguments
    */
    function encode(/* ... */) public virtual returns (bytes memory);

    /**
    Performs packed encoding of the given arguments.
    Note that packed encoding can be ambiguous!
    */
    function encodePacked(/* ... */) public virtual returns (bytes memory);

    /**
    ABI-encodes the given arguments starting from the second and prepends the given four-byte selector
    */
    function encodeWithSelector(bytes4 selector /* , ... */) public virtual returns (bytes memory);

    /**
    Equivalent to abi.encodeWithSelector(bytes4(keccak256(bytes(signature))), ...)
    */
    function encodeWithSignature(string memory signature /* , ... */) public virtual returns (bytes memory);

    /**
    ABI-encodes a call to functionPointer with the arguments found in the tuple.
    Performs a full type-check, ensuring the types match the function signature.
    Result equals abi.encodeWithSelector(functionPointer.selector, (...))
    */
    function encodeCall(/* function functionPointer, ... */) public virtual returns (bytes memory);
}

abstract contract Bytes {
    /**
    Concatenates variable number of bytes and bytes1, …, bytes32 arguments to one byte array
    */
    function concat(/* ... */) public virtual returns (bytes memory);
}

abstract contract String {
    /**
    Concatenates variable number of string arguments to one string array
    */
    function concat(/* ... */) public virtual returns (string memory);
}

abstract contract Array {
    uint length;

    function push(/* ... */) public virtual;
    function pop() public virtual /* returns (...) */;
}

abstract contract Address {
    /**
    Balance of the Address in Wei
    */
    uint256 balance;

    /**
    Code at the Address (can be empty)
    */
    bytes code;

    /**
    The codehash of the Address
    */
    bytes32 codehash;

    /**
    Send given amount of Wei to Address, reverts on failure, forwards 2300 gas stipend, not adjustable
    */
    function transfer(uint256 amount) public virtual;

    /**
    Send given amount of Wei to Address, returns false on failure, forwards 2300 gas stipend, not adjustable
    */
    function send(uint256 amount) public virtual returns (bool);

    /**
    Issue low-level CALL with the given payload, returns success condition and return data, forwards all available gas, adjustable
    */
    function call(bytes memory) public virtual returns (bool, bytes memory);

    /**
    Issue low-level DELEGATECALL with the given payload, returns success condition and return data, forwards all available gas, adjustable
    */
    function delegatecall(bytes memory) public virtual returns (bool, bytes memory);

    /**
    Issue low-level STATICCALL with the given payload, returns success condition and return data, forwards all available gas, adjustable
    */
    function staticcall(bytes memory) public virtual returns (bool, bytes memory);
}

abstract contract Globals {
    Block block;
    Msg msg;
    Tx tx;
    Abi abi;

    /**
    Hash of the given block when blocknumber is one of the 256 most recent blocks; otherwise returns zero
    */
    function blockhash(uint blockNumber) public virtual returns (bytes32);

    /**
    Remaining gas
    */
    function gasleft() public virtual returns (uint256);

    /**
    Causes a Panic error and thus state change reversion if the condition is not met - to be used for internal errors.
    */
    function assert(bool condition) public virtual;

    /**
    Reverts if the condition is not met - to be used for errors in inputs or external components.
    */
    function require(bool condition) public virtual;

    /**
    Reverts if the condition is not met - to be used for errors in inputs or external components.
    Also provides an error message.
    */
    function require(bool condition, string memory message) public virtual;

    /**
    Abort execution and revert state changes
    */
    function revert() public virtual;

    /**
    Abort execution and revert state changes, providing an explanatory string
    */
    function revert(string memory reason) public virtual;

    /**
    Compute (x + y) % k where the addition is performed with arbitrary precision and does not wrap around at 2**256.
    Assert that k != 0 starting from version 0.5.0.
    */
    function addmod(uint x, uint y, uint k) public virtual returns (uint);

    /**
    Compute (x * y) % k where the multiplication is performed with arbitrary precision and does not wrap around at 2**256.
    Assert that k != 0 starting from version 0.5.0.
    */
    function mulmod(uint x, uint y, uint k) public virtual returns (uint);

    /**
    Compute the Keccak-256 hash of the input
    */
    function keccak256(bytes memory) public virtual returns (bytes32);

    /**
    Compute the SHA-256 hash of the input
    */
    function sha256(bytes memory) public virtual returns (bytes32);

    /**
    Compute RIPEMD-160 hash of the input
    */
    function ripemd160(bytes memory) public virtual returns (bytes20);

    /**
    Recover the address associated with the public key from elliptic curve signature or return zero on error.
    The function parameters correspond to ECDSA values of the signature:
        * r = first 32 bytes of signature
        * s = second 32 bytes of signature
        * v = final 1 byte of signature

    ecrecover returns an address, and not an address payable.
    See address payable (https://docs.soliditylang.org/en/v0.8.15/types.html#address) for conversion, in case you need to transfer funds to the recovered address.
    */
    function ecrecover(bytes32 hash, uint8 v, bytes32 r, bytes32 s) public virtual returns (address);

    /**
    Destroy the current contract, sending its funds to the given Address and end execution. Note that selfdestruct has some peculiarities inherited from the EVM:
      * the receiving contract's receive function is not executed.
      * the contract is only really destroyed at the end of the transaction and revert s might "undo" the destruction.
    */
    function selfdestruct(address recipient) public virtual;
}
