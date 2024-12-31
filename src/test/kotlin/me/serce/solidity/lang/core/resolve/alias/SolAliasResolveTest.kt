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





















  fun testResolveEventWithChainedAliases() {
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         import "./a.sol" as A;
         
    """,
      name = "b.sol"
    )

    testResolveBetweenFiles(
      InlineFile(
        code = """
         pragma solidity ^0.8.26;
         
         event eventA();
                //x
    """,
        name = "a.sol"
      ),
      InlineFile(
        """
        pragma solidity ^0.8.26;
        
        import "./b.sol" as B;

        contract C {
            function f() public {
                emit B.A.eventA();
                        //^
            }
       }
  """
      )
    )
  }

  fun testResolveErrorWithChainedAliases() {
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         import "./a.sol" as A;
         
    """,
      name = "b.sol"
    )

    testResolveBetweenFiles(
      InlineFile(
        code = """
         pragma solidity ^0.8.26;
         
         error errorA();
                //x
    """,
        name = "a.sol"
      ),
      InlineFile(
        """
        pragma solidity ^0.8.26;
        
        import "./b.sol" as B;

        contract C {
            function f() public {
                revert B.A.errorA();
                           //^
            }
       }
  """
      )
    )
  }

  fun testResolveEnumInInterfaceWithChainedAliases() {
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         import "./a.sol" as A;
         
    """,
      name = "b.sol"
    )

    testResolveBetweenFiles(
      InlineFile(
        code = """
         pragma solidity ^0.8.26;
         
        interface InterfaceTest{
            enum enumB { B1, B2 }
                        //x
        }
    """,
        name = "a.sol"
      ),
      InlineFile(
        """
        pragma solidity ^0.8.26;
        
        import "./b.sol" as B;

        contract C {
            function f() public {
                B.A.InterfaceTest.enumB.B1;
                                      //^
            }
       }
  """
      )
    )
  }

  fun testResolveEnumInInterfaceWithChainedAliases2() {
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         import "./a.sol" as A;
         
    """,
      name = "b.sol"
    )

    testResolveBetweenFiles(
      InlineFile(
        code = """
         pragma solidity ^0.8.26;
         
        interface InterfaceTest{
            enum enumB { B1, B2 }
                //x
        }
    """,
        name = "a.sol"
      ),
      InlineFile(
        """
        pragma solidity ^0.8.26;
        
        import "./b.sol" as B;

        contract C {
            function f() public {
                B.A.InterfaceTest.enumB.B1;
                                  //^
            }
       }
  """
      )
    )
  }

  fun testResolveErrorInInterfaceWithChainedAliases() {
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         import "./a.sol" as A;
         
    """,
      name = "b.sol"
    )

    testResolveBetweenFiles(
      InlineFile(
        code = """
         pragma solidity ^0.8.26;
         
        interface InterfaceTest{
            error errorB();
                    //x
        }
    """,
        name = "a.sol"
      ),
      InlineFile(
        """
        pragma solidity ^0.8.26;
        
        import "./b.sol" as B;

        contract C {
            function f() public {
               revert B.A.InterfaceTest.errorB();
                                        //^
            }
       }
  """
      )
    )
  }

  fun testResolveInterfaceWithChainedAliases() {
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         import "./a.sol" as A;
         
    """,
      name = "b.sol"
    )

    testResolveBetweenFiles(
      InlineFile(
        code = """
         pragma solidity ^0.8.26;
         
        interface InterfaceTest{
                    //x
            error errorB();
        }
    """,
        name = "a.sol"
      ),
      InlineFile(
        """
        pragma solidity ^0.8.26;
        
        import "./b.sol" as B;

        contract C {
            function f() public {
               revert B.A.InterfaceTest.errorB();
                                //^
            }
       }
  """
      )
    )
  }

  fun testResolveFunctionInContractWithChainedAliases() {
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         import "./a.sol" as A;
         
    """,
      name = "b.sol"
    )

    testResolveBetweenFiles(
      InlineFile(
        code = """
         pragma solidity ^0.8.26;
         
        
        contract contractA {
            function doit() public {
                    //x
            }
        }
    """, name = "a.sol"
      ), InlineFile(
        """
        pragma solidity ^0.8.26;
        
        import "./b.sol" as B;

        contract C {
            function f() public {
               B.A.contractA(address(0)).doit();
                                        //^
            }
        }
      """
      )
    )
  }

  fun testResolveStructInInterfaceWithChainedAliases() {
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         import "./a.sol" as A;
         
    """,
      name = "b.sol"
    )

    testResolveBetweenFiles(
      InlineFile(
        code = """
         pragma solidity ^0.8.26;
         
         interface InterfaceTest{
           struct structA {
                  //x
              address x;
              uint256 y;
          }
        }
    """,
        name = "a.sol"
      ),
      InlineFile(
        """
        pragma solidity ^0.8.26;
        
        import "./b.sol" as B;

        contract C {
            function f() public {
                 B.A.InterfaceTest.structA;
                                    //^
            }
       }
    """
      )
    )
  }

  fun testResolveStructInInterfaceWithChainedAliases2() {
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         import "./a.sol" as A;
         
    """,
      name = "b.sol"
    )

    testResolveBetweenFiles(
      InlineFile(
        code = """
         pragma solidity ^0.8.26;
         
         interface InterfaceTest{
           struct structA {
              address x;
              uint256 y;
                    //x
          }
        }
    """,
        name = "a.sol"
      ),
      InlineFile(
        """
        pragma solidity ^0.8.26;
        
        import "./b.sol" as B;

        contract C {
            function f() public {
                  B.A.InterfaceTest.structA memory testStruct = B.A.InterfaceTest.structA(address(0),2);
                  testStruct.y;
                           //^
            }
       }
    """
      )
    )
  }
}
