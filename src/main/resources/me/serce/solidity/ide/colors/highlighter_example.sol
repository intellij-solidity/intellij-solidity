<KEYWORD>pragma</KEYWORD> solidity ^0.4.11;

<KEYWORD>import</KEYWORD> './Ownable.sol';

<TYPE>uint8</TYPE> <KEYWORD>constant</KEYWORD> <CONSTANT_NAME>MASK</CONSTANT_NAME> = 0x01;

/**
 * @title Claimable
 * @dev Extension for the Ownable contract, where the ownership needs to be claimed.
 * This allows the new owner to accept the transfer.
 */
<KEYWORD>contract</KEYWORD> <CONTRACT_NAME>Claimable</CONTRACT_NAME> <KEYWORD>is</KEYWORD> <CONTRACT_REFERENCE>Ownable</CONTRACT_REFERENCE> {

    <KEYWORD>error</KEYWORD> <ERROR_NAME>InvalidAddress</ERROR_NAME>(<TYPE>address</TYPE> addr);

    <KEYWORD>event</KEYWORD> <EVENT_NAME>TokenTransfer</EVENT_NAME>(<TYPE>uint256</TYPE> tokenId, <TYPE>address</TYPE> recipient);

    <TYPE>bytes</TYPE> <KEYWORD>public</KEYWORD> <KEYWORD>constant</KEYWORD> <CONSTANT_STATE_VARIABLE_NAME>DATA</CONSTANT_STATE_VARIABLE_NAME> = <KEYWORD>hex</KEYWORD><STRING>"48656C6C6F2C20576F726C6421"</STRING>;
    <TYPE>address</TYPE> <KEYWORD>public</KEYWORD> <STATE_VARIABLE_NAME>pendingOwner</STATE_VARIABLE_NAME>;

    <KEYWORD>struct</KEYWORD> <STRUCT_NAME>Struct</STRUCT_NAME> {
        <TYPE>bool</TYPE> flag;
        <TYPE>uint</TYPE> count;
    }

    <KEYWORD>modifier</KEYWORD> <FUNCTION_DECLARATION>onlyPendingOwner</FUNCTION_DECLARATION>() {
        require(msg.sender == pendingOwner);
        _;
    }

    <KEYWORD>function</KEYWORD> <FUNCTION_DECLARATION>transferOwnership</FUNCTION_DECLARATION>(<TYPE>address</TYPE> newOwner) <FUNCTION_CALL>onlyOwner</FUNCTION_CALL> {
        pendingOwner = newOwner;
    }

    <KEYWORD>function</KEYWORD> <FUNCTION_DECLARATION>claimOwnership</FUNCTION_DECLARATION>() <FUNCTION_CALL>onlyPendingOwner</FUNCTION_CALL> {
        owner = pendingOwner;
        pendingOwner = 0x0;
    }

    <KEYWORD>function</KEYWORD> <FUNCTION_DECLARATION>reclaimToken</FUNCTION_DECLARATION>(<CONTRACT_REFERENCE>ERC20Basic</CONTRACT_REFERENCE> token) <KEYWORD>external</KEYWORD> <FUNCTION_CALL>onlyPendingOwner</FUNCTION_CALL> {
        <TYPE>uint256</TYPE> balance = token.balanceOf(this);
        token.transfer(pendingOwner, balance);
    }
}
