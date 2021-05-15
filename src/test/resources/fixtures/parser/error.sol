pragma solidity ^0.8.4;

contract Coin {
    error InsufficientBalance(uint requested, uint available);

    function send(address receiver, uint amount) public {
        revert InsufficientBalance({
            requested: amount,
            available: balances[msg.sender]
        });
    }
}
