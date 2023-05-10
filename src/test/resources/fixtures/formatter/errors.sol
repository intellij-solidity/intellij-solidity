contract Coin {
 // Errors allow you to provide information about
 // why an operation failed. They are returned
 // to the caller of the function.
error InsufficientBalance(uint requested, uint available);

function send(address receiver, uint amount) public {
if (amount > balances[msg.sender]) {
revert InsufficientBalance({
requested: amount,
available: balances[msg.sender]
});
}
}
}
