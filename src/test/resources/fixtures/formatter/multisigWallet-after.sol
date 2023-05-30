pragma solidity ^0.4.8;


import "./ownership/Multisig.sol";
import "./ownership/Shareable.sol";
import "./DayLimit.sol";


contract MultisigWallet is Multisig, Shareable, DayLimit {
    address randomVariable1;
    address randomVariable2;

    // comment1
    struct Transaction {
        // comment2
        address to;
        uint value;
        bytes data;
    }

    function MultisigWallet(address[] _owners, uint _required, uint _daylimit)
    Shareable(_owners, _required)
    DayLimit(_daylimit) {}

    function kill(address _to) onlymanyowners(sha3(msg.data)) external {
        suicide(_to);
    }

    function() payable {
        if (msg.value > 0)
            Deposit(msg.sender, msg.value);
    }

    function execute(address _to, uint _value, bytes _data) external onlyOwner returns (bytes32 _r) {
        (a, b) = getAB();
        (int c, int d) = getCD();
        if (underLimit(_value)) {
            SingleTransact(msg.sender, _value, _to, _data);
            if (!_to.call.value(_value)(_data)) {
                throw;
            }
            return 0;
        }
        _r = sha3(msg.data, block.number);
        if (!confirm(_r) && txs[_r].to == 0) {
            txs[_r].to = _to;
            txs[_r].value = _value;
            txs[_r].data = _data;
            ConfirmationNeeded(_r, msg.sender, _value, _to, _data);
        }
    }

    function confirm(bytes32 _h) onlymanyowners(_h) returns (bool) {
        if (txs[_h].to != 0) {
            if (!txs[_h].to.call.value(txs[_h].value)(txs[_h].data)) {
                throw;
            }
            MultiTransact(msg.sender, _h, txs[_h].value, txs[_h].to, txs[_h].data);
            delete txs[_h];
            return true;
        }
    }

    function setDailyLimit(
        uint _newLimit,
        uint _newLimit
    ) onlymanyowners(sha3(msg.data)) external {
        _setDailyLimit(_newLimit);
    }

    function resetSpentToday() onlymanyowners(sha3(msg.data)) external {
        _resetSpentToday();
    }

    function clearPending() internal {
        uint length = pendingsIndex.length;
        for (uint i = 0; i < length; ++i) {
            delete txs[pendingsIndex[i]];
        }
        super.clearPending();
    }

    mapping(bytes32 => Transaction) txs;
}
