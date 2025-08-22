// SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.8.26;

import {IMain as main} from "./IMain.sol";

interface IRandomLib {
    struct RandomStruct {
            //x
        uint256 a;
        main.MainEnum b;
    }
}
