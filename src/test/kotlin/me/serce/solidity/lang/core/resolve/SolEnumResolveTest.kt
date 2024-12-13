package me.serce.solidity.lang.core.resolve

class SolEnumResolveTest : SolResolveTestBase() {
  fun testEnumResolve() = checkByCode("""
        contract A {
            enum B { A1, A2 }
                        //x

            function f() public {
                B.A2;
                //^
            }
        }
  """)

  fun testEnumResolveInFile() = checkByCode("""
      enum B { A1, A2 }
         //x

      contract A {
          function f() public {
              B.A2;
            //^
          }
      }
  """)

  fun testEnumItself() = checkByCode("""
        contract A {
            enum B { A1, A2 }
               //x

            function f() public {
                B.A2;
              //^
            }
        }
  """)

  fun testEnumItself2() = checkByCode("""
        contract A {
            enum B { A1, A2 }
               //x

            function f(B test) public {
                     //^
            }
        }
  """)

  fun testEnumParent() = checkByCode("""
        contract A {
            enum B { A1, A2 }
                    //x
        }

        contract C is A {
            function f() public {
                B.A1;
                //^
            }
        }
  """)

  fun testEnumParent2() = checkByCode("""
        contract A {
            enum B { A1, A2 }
               //x
        }

        contract C is A {
            function f(B test) public {
                     //^
            }
        }
  """)

  fun testEnumFromOtherContract() = checkByCode("""
        contract A {
            enum B { A1, A2 }
               //x
        }

        contract C {
            function f(A.B test) public {
                       //^
            }
        }
  """)

  fun testEnumValueFromOtherContract() = checkByCode("""
        contract A {
            enum B { A1, A2 }
                   //x
        }

        contract C {
            function f() public {
                A.B test = A.B.A1;
                              //^
            }
        }
  """)

  fun testResolveImportedEnum() = testResolveBetweenFiles(
    InlineFile(
      code = """
          pragma solidity ^0.8.26;
          
           enum B { A1, A2 }
              //x
      """,
      name = "a.sol"
    ),
    InlineFile(
      """
          pragma solidity ^0.8.26;      
                
          import {B} from "./a.sol";
                //^
          contract b {
              function f() {
                  B.A2;
              }
          }
    """
    )
  )

  fun testResolveImportedEnum2() = testResolveBetweenFiles(
    InlineFile(
      code = """
          pragma solidity ^0.8.26;
          
           enum B { A1, A2 }
              //x
      """,
      name = "a.sol"
    ),
    InlineFile(
      """
          pragma solidity ^0.8.26;      
                
          import {B} from "./a.sol";
                
          contract b {
              function f() {
                  B.A2;
                //^
              }
          }
    """
    )
  )

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

  fun testResolveEnumFromAlias2() = testResolveBetweenFiles(
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

  fun testResolveEnumFromAliasInInterface2() = testResolveBetweenFiles(
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         interface InterfaceI {
                     //x
            enum B { A1, A2 }
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

  fun testResolveEnumFromAliasInInterface3() = testResolveBetweenFiles(
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

  fun testResolveEnumFromAliasInInterface4() = testResolveBetweenFiles(
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
}
