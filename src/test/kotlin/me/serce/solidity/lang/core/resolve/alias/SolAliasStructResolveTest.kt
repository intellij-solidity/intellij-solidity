package me.serce.solidity.lang.core.resolve.alias

import me.serce.solidity.lang.core.resolve.SolResolveTestBase

class SolAliasStructResolveTest : SolResolveTestBase() {
  fun testResolveStructWithChainedAliases() {
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
         
         struct structA {
                //x
            address x;
            uint256 y;
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
                 B.A.structA;
                    //^
            }
       }
  """
      )
    )
  }

  fun testResolveStructAsFunctionWithChainedAliases() {
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
         
         struct structA {
                //x
            address x;
            uint256 y;
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
                 B.A.structA testStruct = B.A.structA(address(0),2);
                                                //^
            }
       }
  """
      )
    )
  }

  fun testResolveStructMemberWithChainedAliases() {
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
         
         struct structA {
            address x;
            uint256 y;
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
                 B.A.structA testStruct = B.A.structA(address(0),2);
                 testStruct.y;
                          //^
            }
       }
  """
      )
    )
  }

  fun testResolveStructFromAlias() = testResolveBetweenFiles(
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
          struct Prop {
                //x
              uint prop1;
              uint prop2;
          }
    """,
      name = "a.sol"
    ),
    InlineFile(
      """
        pragma solidity ^0.8.26;
        
        import {Prop as Proposal} from "./a.sol";

        contract C {
            Proposal prop = Proposal(0, 1);
            //^
            function f() public {
                prop.prop1;
            }
       }
  """
    )
  )

  fun testResolveStructFromAlias2() = testResolveBetweenFiles(
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
          struct Prop {
                //x
              uint prop1;
              uint prop2;
          }
    """,
      name = "a.sol"
    ),
    InlineFile(
      """
        pragma solidity ^0.8.26;
        
        import {Prop as Proposal} from "./a.sol";

        contract C {
            Proposal prop = Proposal(0, 1);
                            //^
            function f() public {
                prop.prop1;
            }
       }
  """
    )
  )

  fun testResolveStructMemberFromAlias() = testResolveBetweenFiles(
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
          struct Prop {
              uint prop1;
                  //x
              uint prop2;
          }
    """,
      name = "a.sol"
    ),
    InlineFile(
      """
        pragma solidity ^0.8.26;
        
        import {Prop as Proposal} from "./a.sol";

        contract C {
            Proposal prop = Proposal(0, 1);
            function f() public {
                prop.prop1;
                      //^
            }
       }
  """
    )
  )

  fun testResolveStructInInterfaceWithChainedFileAlias() {
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

  fun testResolveStructMemberInInterfaceWithChainedFileAlias() {
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
