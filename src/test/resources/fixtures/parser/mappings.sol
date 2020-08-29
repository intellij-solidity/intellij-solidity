interface IElement {}

contract MyToken {
    mapping (address => uint256) public balanceOf;
    mapping (address => mapping (address => uint256)) public allowance;
    mapping (IElement => address) public testVar;
}
