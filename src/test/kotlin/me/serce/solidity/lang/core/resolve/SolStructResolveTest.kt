package me.serce.solidity.lang.core.resolve

class SolStructResolveTest : SolResolveTestBase() {
  fun testStructResolve() = checkByCode("""
      contract B {
          struct Prop {
                //x
              uint8 prop;
          }

          Prop[] aa;
          //^
      }
  """)

  fun testStructResolveTopLevel() = checkByCode("""
      struct Prop {
            //x
          uint8 prop;
      }
          
      contract B {
          Prop[] aa;
          //^
      }
  """)


  fun testStructResolveFromLibrary() = checkByCode("""
      library Library {
          struct Prop {
                //x
              uint8 prop;
          } 
      }
      
      contract B {
          Library.Prop aa;
                 //^
      }
  """)

  fun testStructResolveOneField() = checkFunctionByCode("""
      contract B {
          struct Prop {
                //x
              uint prop1;
          }

          Prop prop = Prop(0);
                      //^
      }
  """)

  fun testStructResolveTwoFields() = checkFunctionByCode("""
      contract B {
          struct Prop {
                //x
              uint prop1;
              uint prop2;
          }

          Prop prop = Prop(0, 1);
                      //^
      }
  """)

  fun testStructResolveInherited() = checkFunctionByCode("""
      contract A {
          struct Prop {
                //x
              uint prop1;
              uint prop2;
          }
      }
      contract B is A {
          Prop prop = Prop(0, 1);
                      //^
      }
  """)

  fun testResolveImportedStruct() {
    val file1 = InlineFile(
      code = """
        struct Proposal {
                //x
            uint256 id;
        }
      """.trimIndent(),
      name = "Abc.sol"
    )

    val file2 = InlineFile("""
        import "./Abc.sol";
        contract B { 
            function doit(uint256[] storage array) {
                Proposal prop = Proposal(1);
                                   //^
            }
        }
    """)

    testResolveBetweenFiles(file1, file2)
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
    InlineFile("""
        pragma solidity ^0.8.26;
        
        import {Prop as Proposal} from "./a.sol";

        contract C {
            Proposal prop = Proposal(0, 1);
            //^
            function f() public {
                prop.prop1;
            }
       }
  """)
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
    InlineFile("""
        pragma solidity ^0.8.26;
        
        import {Prop as Proposal} from "./a.sol";

        contract C {
            Proposal prop = Proposal(0, 1);
                            //^
            function f() public {
                prop.prop1;
            }
       }
  """)
  )

  fun testResolveStructFromAlias3() = testResolveBetweenFiles(
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
    InlineFile("""
        pragma solidity ^0.8.26;
        
        import {Prop as Proposal} from "./a.sol";
                          //^

        contract C {
            Proposal prop = Proposal(0, 1);
            function f() public {
                prop.prop1;
            }
       }
  """)
  )

  fun testResolveStructFromAlias4() = testResolveBetweenFiles(
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
    InlineFile("""
        pragma solidity ^0.8.26;
        
        import {Prop as Proposal} from "./a.sol";

        contract C {
            Proposal prop = Proposal(0, 1);
            function f() public {
                prop.prop1;
                      //^
            }
       }
  """)
  )
}
