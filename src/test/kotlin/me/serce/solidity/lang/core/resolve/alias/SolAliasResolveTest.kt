package me.serce.solidity.lang.core.resolve.alias

import me.serce.solidity.lang.core.resolve.SolResolveTestBase

class SolAliasResolveTest : SolResolveTestBase() {

  fun testResolveFileAlias() = checkByCode(
    """
          pragma solidity ^0.8.26;
                
          import "./a.sol" as A;
                            //x
          contract b {
            function test(address x) public {
                A.a(x).doit();
              //^
            }
          }
    """
  )

  fun testResolveFileAlias2() = checkByCode(
    """
          pragma solidity ^0.8.26;
                
          import * as A from "./a.sol";
                    //x
          contract b {
            function test() public {
                A.a.doit();
              //^
            }
          }
    """
  )
}
