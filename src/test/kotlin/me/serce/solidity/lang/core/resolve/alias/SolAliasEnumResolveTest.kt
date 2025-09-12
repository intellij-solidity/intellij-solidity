package me.serce.solidity.lang.core.resolve.alias

import me.serce.solidity.lang.core.resolve.SolResolveTestBase

class SolAliasEnumResolveTest : SolResolveTestBase() {
  fun testResolveEnumWithChainedAliases() {
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         import {enumLambda as A} from "./a.sol";
         
         
    """,
      name = "b.sol"
    )

    testResolveBetweenFiles(
      InlineFile(
        code = """
         pragma solidity ^0.8.26;
         
         enum enumLambda { A1, A2 }
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
                B.A.A2;
                //^
            }
       }
  """
      )
    )
  }

  fun testResolveEnumMemberWithChainedAliases() {
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         import {enumLambda as A} from "./a.sol";
         
         
    """,
      name = "b.sol"
    )

    testResolveBetweenFiles(
      InlineFile(
        code = """
         pragma solidity ^0.8.26;
         
         enum enumLambda { A1, A2 }
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
                B.A.A2;
                  //^
            }
       }
  """
      )
    )
  }

  fun testResolveEnumWithChainedFileAlias() {
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
         
         enum enumLambda { A1, A2 }
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
                B.A.enumLambda.A2;
                    //^
            }
       }
  """
      )
    )
  }

  fun testResolveEnumMemberWithChainedFileAlias() {
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
         
         enum enumLambda { A1, A2 }
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
                B.A.enumLambda.A2;
                             //^
            }
       }
  """
      )
    )
  }

  fun testResolveEnumFromAlias() = testResolveBetweenFiles(
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
          enum B { A1, A2 }
             //x
    """,
      name = "a.sol"
    ),
    InlineFile("""
        pragma solidity ^0.8.26;
        
        import {B as enumB} from "./a.sol";

        contract C {
            function f() public {
                enumB.A2;
                //^
            }
       }
  """)
  )

  fun testResolveEnumMemberFromAlias() = testResolveBetweenFiles(
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
          enum B { A1, A2 }
                      //x
    """,
      name = "a.sol"
    ),
    InlineFile("""
        pragma solidity ^0.8.26;
        
        import {B as enumB} from "./a.sol";

        contract C {
            function f() public {
                enumB.A2;
                    //^
            }
       }
  """)
  )

  fun testResolveEnumFromAliasInInterface() = testResolveBetweenFiles(
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         interface InterfaceI {
            enum B { A1, A2 }
               //x
         }
    """,
      name = "a.sol"
    ),
    InlineFile("""
        pragma solidity ^0.8.26;
        
        import {InterfaceI as Types} from "./a.sol";

        contract C {
            function f() public {
                Types.B.A2;
                    //^
            }
       }
  """)
  )

  fun testResolveEnumMemberFromAliasInInterface() = testResolveBetweenFiles(
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         interface InterfaceI {
            enum B { A1, A2 }
                       //x
         }
    """,
      name = "a.sol"
    ),
    InlineFile("""
        pragma solidity ^0.8.26;
        
        import {InterfaceI as Types} from "./a.sol";

        contract C {
            function f() public {
                Types.B.A2;
                       //^
            }
       }
  """)
  )

  fun testResolveEnumFromChainedAliasInInterface() {
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         import {InterfaceI as Types} from "./a.sol";
    """,
      name = "b.sol"
    )
    testResolveBetweenFiles(
      InlineFile(
        code = """
         pragma solidity ^0.8.26;
         
         interface InterfaceI {
            enum enumLambda { A1, A2 }
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
            B.Types.enumLambda.A2;
                       //^
          }
        }
      """
      )
    )
  }

  fun testResolveEnumMemberFromChainedAliasInInterface() {
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         import {InterfaceI as Types} from "./a.sol";
    """,
      name = "b.sol"
    )
    testResolveBetweenFiles(
      InlineFile(
        code = """
         pragma solidity ^0.8.26;
         
         interface InterfaceI {
            enum enumLambda { A1, A2 }
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
            B.Types.enumLambda.A2;
                              //^
          }
        }
      """
      )
    )
  }

  fun testResolveEnumAsParameterWithChainedAlias()  {
      InlineFile(
        code = """
         pragma solidity ^0.8.26;
         
         import {ContractA as A} from "./a.sol";
         
         
    """,
        name = "b.sol"
      )
    testResolveBetweenFiles(
      InlineFile(
        code = """
         pragma solidity ^0.8.26;
         
         contract ContractA {
            enum enumLambda { A1, A2 }
               //x
         }
    """,
        name = "a.sol"
      ),
      InlineFile("""
        pragma solidity ^0.8.26;
        
        import "./b.sol" as B;
        
        contract C {
            function f(B.A.enumLambda test) public {
                            //^
            }
        }
  """)
    )
  }

  fun testResolveEnumInInterfaceWithChainedFileAlias() {
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

  fun testResolveEnumMemberInInterfaceWithChainedFileAlias() {
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

    fun testResolveEnumFromAliasWithInheritance() {
        InlineFile(
            code = """
        pragma solidity ^0.8.0;
    
        import {Types as Constants} from "./types.sol";
        
        contract Parent {
            function foo() public pure returns (uint256) {
                return 42;
            }
        }
            """, name = "parent.sol"
        )

        testResolveBetweenFiles(
            InlineFile(
                code = """
            pragma solidity ^0.8.0;

            interface Types {
                enum enumB { B1, B2 }
                    //x
            }
            """, name = "types.sol"
            ), InlineFile(
                code = """
            pragma solidity ^0.8.0;

            import "./parent.sol";
            
             contract Child is Parent {
                function foo2() public {
                    Constants.enumB.B1;
                             //^
                }
            }
            """, name = "child.sol"
            )
        )
    }

    fun testResolveEnumMemberFromAliasWithInheritance() {
        InlineFile(
            code = """
        pragma solidity ^0.8.0;
    
        import {Types as Constants} from "./types.sol";
        
        contract Parent {
            function foo() public pure returns (uint256) {
                return 42;
            }
        }
    """, name = "parent.sol"
        )

        testResolveBetweenFiles(
            InlineFile(
                code = """
            pragma solidity ^0.8.0;

            interface Types {
                enum enumB { B1, B2 }
                            //x
            }
            """, name = "types.sol"
            ), InlineFile(
                code = """
            pragma solidity ^0.8.0;

            import "./parent.sol";
            
             contract Child is Parent {
                function foo2() public {
                    Constants.enumB.B1;
                                  //^
                }
            }
            """, name = "child.sol"
            )
        )
    }
}
