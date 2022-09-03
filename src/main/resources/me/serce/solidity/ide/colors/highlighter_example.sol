<KEYWORD>pragma</KEYWORD> solidity ^0.4.11;

<KEYWORD>import</KEYWORD> './Ownable.sol';

<TYPE>uint8</TYPE> <KEYWORD>constant</KEYWORD> <CONSTANT>MASK</CONSTANT> = 0x01;

<KEYWORD>enum</KEYWORD> <ENUM_NAME>Types</ENUM_NAME> {
    <ENUM_VALUE>Type1</ENUM_VALUE>,
    <ENUM_VALUE>Type2</ENUM_VALUE>
}

<KEYWORD>type</KEYWORD> <USER_DEFINED_VALUE_TYPE>Arg</USER_DEFINED_VALUE_TYPE> <KEYWORD>is</KEYWORD> <TYPE>uint256</TYPE>;

/**
 * @title Claimable
 * @dev Extension for the Ownable contract, where the ownership needs to be claimed.
 * This allows the new owner to accept the transfer.
 */
<KEYWORD>contract</KEYWORD> <CONTRACT_NAME>Claimable</CONTRACT_NAME> <KEYWORD>is</KEYWORD> <CONTRACT_NAME>Ownable</CONTRACT_NAME> {

    <KEYWORD>error</KEYWORD> <ERROR_NAME>InvalidAddress</ERROR_NAME>(<TYPE>address</TYPE> addr);

    <KEYWORD>event</KEYWORD> <EVENT_NAME>TokenTransfer</EVENT_NAME>(<TYPE>uint256</TYPE> tokenId, <TYPE>address</TYPE> recipient);

    <TYPE>bytes</TYPE> <KEYWORD>public</KEYWORD> <KEYWORD>constant</KEYWORD> <CONSTANT>DATA</CONSTANT> = <KEYWORD>hex</KEYWORD><STRING>"48656C6C6F2C20576F726C6421"</STRING>;
    <TYPE>address</TYPE> <KEYWORD>public</KEYWORD> <STATE_VARIABLE>pendingOwner</STATE_VARIABLE>;

    <KEYWORD>struct</KEYWORD> <STRUCT_NAME>Struct</STRUCT_NAME> {
        <TYPE>bool</TYPE> flag;
        <TYPE>uint</TYPE> count;
    }

    <KEYWORD>modifier</KEYWORD> <FUNCTION_DECLARATION>onlyPendingOwner</FUNCTION_DECLARATION>() {
        <KEYWORD>require</KEYWORD>(<GLOBAL>msg</GLOBAL>.sender == pendingOwner);
        _;
    }

    <RECEIVE_FALLBACK_DECLARATION>receive</RECEIVE_FALLBACK_DECLARATION>() <KEYWORD>external</KEYWORD> <KEYWORD>payable</KEYWORD> {}
    <RECEIVE_FALLBACK_DECLARATION>fallback</RECEIVE_FALLBACK_DECLARATION>() <KEYWORD>external</KEYWORD> {}

    <KEYWORD>function</KEYWORD> <FUNCTION_DECLARATION>transferOwnership</FUNCTION_DECLARATION>(<TYPE>address</TYPE> newOwner) <FUNCTION_CALL>onlyOwner</FUNCTION_CALL> {
        pendingOwner = newOwner;
    }

    <KEYWORD>function</KEYWORD> <FUNCTION_DECLARATION>claimOwnership</FUNCTION_DECLARATION>() <FUNCTION_CALL>onlyPendingOwner</FUNCTION_CALL> {
        owner = pendingOwner;
        pendingOwner = 0x0;
    }

    <KEYWORD>function</KEYWORD> <FUNCTION_DECLARATION>reclaimToken</FUNCTION_DECLARATION>(<CONTRACT_NAME>ERC20Basic</CONTRACT_NAME> token) <KEYWORD>external</KEYWORD> <FUNCTION_CALL>onlyPendingOwner</FUNCTION_CALL> {
        <TYPE>uint256</TYPE> balance = token.<FUNCTION_CALL>balanceOf</FUNCTION_CALL>(this);
        token.<FUNCTION_CALL>transfer</FUNCTION_CALL>(pendingOwner, balance);
    }
}
