package me.serce.solidity.lang.core.resolve

class SolAliasResolveTest : SolResolveTestBase() {
    fun testResolveImportedFunctionFromBracketWithAlias() = testResolveBetweenFiles(
        InlineFile(
            code = """
          pragma solidity ^0.8.26;
                
          library a {
            function doit() internal {
                     //x
            }
          }
      """,
            name = "a.sol"
        ),
        InlineFile(
            """
          pragma solidity ^0.8.26;      
                
          import {a as A} from "./a.sol";
              
          contract b {
            function test() public {
                A.doit();
                //^
            }
          }
                      
    """
        )
    )

    fun testResolveContractFromBracketWithAlias() = testResolveBetweenFiles(
        InlineFile(
            code = """
            pragma solidity ^0.8.26;
                
            library a {
                  //x
                function doit() internal {
                }
            }
      """,
            name = "a.sol"
        ),
        InlineFile(
            """
            pragma solidity ^0.8.26;
                
            import {a as A} from "./a.sol";
                     
            contract b {
                function test() public {
                    A.doit();
                  //^
                }
            }
    """
        )
    )

    fun testResolveImportedFunctionFromPathWithAlias() = testResolveBetweenFiles(
        InlineFile(
            code = """
          pragma solidity ^0.8.26;
                
          contract a {
            function doit() public {
                     //x
            }
          }
      """,
            name = "a.sol"
        ),
        InlineFile(
            """
          pragma solidity ^0.8.26;
                
          import "./a.sol" as A;
              
          contract b {
            function test(address x) public {
                A.a(x).doit();
                     //^
            }
          }
                      
    """
        )
    )

    fun testResolveContractFromPathWithAlias() = testResolveBetweenFiles(
        InlineFile(
            code = """
          pragma solidity ^0.8.26;
                
          contract a {
                 //x
            function doit() public {
            }
          }
      """,
            name = "a.sol"
        ),
        InlineFile(
            """
          pragma solidity ^0.8.26;
                
          import "./a.sol" as A;
                     
          contract b {
            function test(address x) public {
                A.a(x).doit();
              //^
            }
          }
    """
        )
    )

    fun testResolveContractFromPathWithAlias2() = testResolveBetweenFiles(
        InlineFile(
            code = """
          pragma solidity ^0.8.26;
                
          contract a {
                 //x
            function doit() public {
            }
          }
      """,
            name = "a.sol"
        ),
        InlineFile(
            """
          pragma solidity ^0.8.26;
                
          import "./a.sol" as A;
                     
          contract b {
            function test(address x) public {
                A.a(x).doit();
                //^
            }
          }
    """
        )
    )

    fun testResolveImportedFunctionFromAsteriskWithAlias() = testResolveBetweenFiles(
        InlineFile(
            code = """
          pragma solidity ^0.8.26;
                
          library a {
            function doit() internal {
                     //x
            }
          }
      """,
            name = "a.sol"
        ),
        InlineFile(
            """
          pragma solidity ^0.8.26;
                
          import * as A from "./a.sol";
              
          contract b {
            function test() public {
                A.a.doit();
                  //^
            }
          }
    """
        )
    )

    fun testResolveContractFromAsteriskWithAlias() = testResolveBetweenFiles(
        InlineFile(
            code = """
          pragma solidity ^0.8.26;
                
          library a {
                //x
            function doit() internal {
            }
          }
      """,
            name = "a.sol"
        ),
        InlineFile(
            """
          pragma solidity ^0.8.26;
                
          import * as A from "./a.sol";
              
          contract b {
            function test() public {
                A.a.doit();
              //^
            }
          }
    """
        )
    )

    fun testResolveContractFromAsteriskWithAlias2() = testResolveBetweenFiles(
        InlineFile(
            code = """
          pragma solidity ^0.8.26;
                
          library a {
                //x
            function doit() internal {
            }
          }
      """,
            name = "a.sol"
        ),
        InlineFile(
            """
          pragma solidity ^0.8.26;
                
          import * as A from "./a.sol";
              
          contract b {
            function test() public {
                A.a.doit();
                //^
            }
          }
    """
        )
    )

    fun testResolveContractFromAsteriskWithAliasMultipleContracts() = testResolveBetweenFiles(
        InlineFile(
            code = """
            pragma solidity ^0.8.26;
                
            contract a {
                function doit() public {
                }
            }
            
            contract ab {
                function doit() public {
                       //x
                }
            }
      """,
            name = "a.sol"
        ),
        InlineFile(
            """
          pragma solidity ^0.8.26;
                
          import * as A from "./a.sol";
              
          contract b {
            function test(address x) public {
                A.ab(x).doit();
                       //^
            }
          }
    """
        )
    )
}
