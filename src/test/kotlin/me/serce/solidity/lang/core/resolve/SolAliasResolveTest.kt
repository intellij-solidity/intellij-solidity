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

  fun testResolveEnumWithChainedAliases2() {
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

  fun testResolveEnumWithChainedAliases3() {
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

  fun testResolveEnumWithChainedAliases4() {
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

  fun testResolveStructWithChainedAliases2() {
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

  fun testResolveStructWithChainedAliases3() {
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

  fun testResolveTypeWithChainedAliases() {
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
         
         type Decimal18 is uint256;
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
                B.A.Decimal18;
                    //^
            }
       }
  """
      )
    )
  }

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
                  B.A.structA testStruct = B.A.structA(address(0),2);
                  testStruct.y;
                           //^
            }
       }
    """
      )
    )
  }
}
