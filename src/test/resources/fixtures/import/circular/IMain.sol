// SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.8.26;

import {IRandomLib} from "./IRandomLib.sol";

interface IMain {
    enum MainEnum {
        A,
        B,
        C
    }
    struct MainStruct {
        uint256 a;
        IRandomLib.RandomStruct b;
    }
}
