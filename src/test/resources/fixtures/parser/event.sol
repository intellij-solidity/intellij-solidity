interface I {
    event ForeignEvent();
}

event E();

contract MyToken {
    event Transfer(address indexed from, address indexed to, uint256 value);

    event TestEventName(address indexed _token, address indexed _token2, uint _amount);

    function f(address from, address to, uint256 value) public {

        emit I.ForeignEvent();
        emit Transfer(from, to, value);

        emit E();
    }
}
