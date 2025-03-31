// SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.8.26;

import {lib} from "./a/lib.sol";
      //^
import { b } from "./b/b.sol";

library ImportLibNameClash {
    function test() external returns(uint256){
        return lib.functionA() + b.functionB();
    }
}
