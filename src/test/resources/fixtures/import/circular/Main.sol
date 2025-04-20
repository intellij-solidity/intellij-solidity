// SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.8.26;
import {IMain} from "./IMain.sol";
import {IRandomLib} from "./IRandomLib.sol";

contract Main  {
    IMain.MainStruct public mainStorage;

    constructor(){
        mainStorage = IMain.MainStruct(1,IRandomLib.RandomStruct(2,IMain.MainEnum.A));
                                                            //^
    }
}
