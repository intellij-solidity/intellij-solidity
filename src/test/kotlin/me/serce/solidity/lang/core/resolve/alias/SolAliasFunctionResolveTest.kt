package me.serce.solidity.lang.core.resolve.alias

import me.serce.solidity.lang.core.resolve.SolResolveTestBase

class SolAliasFunctionResolveTest : SolResolveTestBase() {

  fun testResolveImportedFunctionFromLibraryAlias() = testResolveBetweenFiles(
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

  fun testResolveImportedFunctionFromPathAlias() = testResolveBetweenFiles(
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

  fun testResolveImportedFunctionFromLibraryPathAlias() = testResolveBetweenFiles(
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

  fun testResolveFunctionFromFileAliasWithMultipleContracts() = testResolveBetweenFiles(
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

  fun testResolveFunctionFromFileAliasWithMultipleLibraries() = testResolveBetweenFiles(
    InlineFile(
      code = """
            pragma solidity ^0.8.26;
                
            library a {
                function doit() internal {
                }
            }
            
            library ab {
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
            function test(address x) public {
                A.ab.doit();
                    //^
            }
          }
    """
    )
  )

  fun testResolveFunctionFromChainedImportAlias() {
    InlineFile(
      code = """
        pragma solidity ^0.8.26;
        
        import "./z.sol";
            
        contract a {
            function doit() public {
            }
        }
        """, name = "a.sol"
    )

    testResolveBetweenFiles(
      InlineFile(
        code = """
          pragma solidity ^0.8.26;
              
          contract z {
              function doit2() public {
                      //x
              }
          }
          """, name = "z.sol"
      ),

      InlineFile(
        """
          pragma solidity ^0.8.26;
              
          import * as A from "./a.sol";
            
          contract b {
              function test(address x) public {
                  A.z(x).doit2();
                         //^
              }
          }
          """
      )
    )
  }

  fun testResolveFunctionFromChainedFileAlias() {
    InlineFile(
      code = """
        pragma solidity ^0.8.26;
        
        import "./z.sol" as Z;
            
        contract a {
            function doit() public {
            }
        }
        """, name = "a.sol"
    )

    testResolveBetweenFiles(
      InlineFile(
        code = """
          pragma solidity ^0.8.26;
              
          contract z {
              function doit2() public {
                      //x
              }
          }
          """, name = "z.sol"
      ),

      InlineFile(
        """
          pragma solidity ^0.8.26;
              
          import * as A from "./a.sol";
            
          contract b {
              function test(address x) public {
                  A.Z.z(x).doit2();
                          //^
              }
          }
          """
      )
    )
  }
}
