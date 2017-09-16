pragma solidity ^0.4.11;


import './Ownable.sol';


/**
 * @title Claimable
 * @dev Extension for the Ownable contract, where the ownership needs to be claimed.
 * This allows the new owner to accept the transfer.
 */
contract Claimable is <CONTRACT_REFERENCE>Ownable</CONTRACT_REFERENCE> {
    <KEYWORD>address</KEYWORD> public pendingOwner;

    modifier onlyPendingOwner() {
        require(msg.sender == pendingOwner);
        _;
    }

    function transferOwnership(<KEYWORD>address</KEYWORD> newOwner) onlyOwner {
        pendingOwner = newOwner;
    }

    function claimOwnership() onlyPendingOwner {
        owner = pendingOwner;
        pendingOwner = 0x0;
    }

    function reclaimToken(<CONTRACT_REFERENCE>ERC20Basic</CONTRACT_REFERENCE> token) external onlyPendingOwner {
        <KEYWORD>uint256</KEYWORD> balance = token.balanceOf(this);
        token.transfer(pendingOwner, balance);
    }
}
