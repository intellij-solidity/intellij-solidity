// https://github.com/intellij-solidity/intellij-solidity/issues/227
function sum(uint[] memory _arr) pure returns (uint s) {
    for (uint i = 0; i < _arr.length; i++) {
        s += _arr[i];
    }
}

contract MyToken {
    string public standard = 'Token 0.1';
    string public name;
    string public symbol;
    uint8 public decimals;
    uint256 public totalSupply;

    function MyToken(
        uint256 initialSupply,
        string tokenName,
        uint8 decimalUnits,
        string tokenSymbol
        ) {
        balanceOf[msg.sender] = initialSupply;
        totalSupply = initialSupply;
        name = tokenName;
        symbol = tokenSymbol;
        decimals = decimalUnits;
    }

    function () {
        throw;
    }

    function hello() internal constant returns (string memory str) {
        return "hel";
    }

    receive () external payable { }

    fallback () external { }

    receive () external payable virtual {
        emit PaymentReceived(_msgSender(), msg.value);
    }

    function totalSupply() public view override(IERC20, IERC777) returns (uint256) {
        return _totalSupply;
    }
}
