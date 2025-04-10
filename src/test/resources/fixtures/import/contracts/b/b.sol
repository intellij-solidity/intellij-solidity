// SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.8.26;

import {lib} from "./lib.sol";

library b{
    function functionB() external returns(uint256) {
        return 10 + lib.functionLib();
    }
}
