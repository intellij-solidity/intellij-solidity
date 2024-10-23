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

    fun testResolveContractFromBracketWithAlias2() = testResolveBetweenFiles(
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
                         //^
              contract b {
                  function test() public {
                      A.doit();
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

    fun testResolveContractFromPathWithAlias() = checkByCode(
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

    fun testResolveContractFromPathWithAlias3() = testResolveToAnotherFile(
      InlineFile(
        code = """
            pragma solidity ^0.8.26;
            contract a {
              function doit() public {
              }
            }
        """,
        name = "a.sol"
      ).psiFile,
      InlineFile(
        """
            pragma solidity ^0.8.26;
                  
            import "./a.sol" as A;
                              //^
            contract b {
              function test(address x) public {
                  A.a(x).doit();
              }
            }
      """
      ).psiFile
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

    fun testResolveImportedFunctionFromAsteriskWithAlias2() = testResolveToAnotherFile(
      InlineFile(
        code = """
            pragma solidity ^0.8.26;
                  
            library a {
              function doit() internal {
              }
            }
        """,
        name = "a.sol"
      ).psiFile,
      InlineFile(
        """
            pragma solidity ^0.8.26;
                  
            import * as A from "./a.sol";
                      //^
                
            contract b {
              function test() public {
                  A.a.doit();
              }
            }
      """
      ).psiFile
    )

    fun testResolveContractFromAsteriskWithAlias() = checkByCode(
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

    fun testResolveLibraryFromAsteriskWithAliasMultipleLibrary() = testResolveBetweenFiles(
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

    fun testResolveContractFromAsteriskWithAliasMultipleContracts2() {
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

            InlineFile("""
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
}
