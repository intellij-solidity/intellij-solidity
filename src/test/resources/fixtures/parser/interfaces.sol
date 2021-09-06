interface Token {
    function transfer(address recipient, uint amount) external;

    receive() external payable;
}
