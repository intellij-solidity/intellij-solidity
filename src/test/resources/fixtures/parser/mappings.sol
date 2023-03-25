interface IElement {}

contract MyToken {
    mapping (address => uint256) public balanceOf;
    mapping (address => mapping (address => uint256)) public allowance;
    mapping (IElement => address) public testVar;
    mapping(address user => uint balance) public balances;
    mapping(address owner => mapping(address spender => uint value)) public allowance;
    mapping(bytes32 => address sender) public commits;
    mapping(address owner => mapping(address spender => bytes32[] notes)) names;
}
