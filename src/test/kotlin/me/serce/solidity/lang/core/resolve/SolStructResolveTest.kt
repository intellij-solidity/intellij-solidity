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

  fun testResolveImportedStruct() = testResolveBetweenFiles(
    InlineFile(
      code = """
        struct Proposal {
                //x
            uint256 id;
        }
      """,
      name = "Abc.sol"
    ),

    InlineFile(
      """
        import "./Abc.sol";
        contract B { 
            function doit(uint256[] storage array) {
                Proposal prop = Proposal(1);
                                   //^
            }
        }
    """
    )

  )

  fun testResolveImportedStruct2() = testResolveBetweenFiles(
    InlineFile(
      code = """
        struct Proposal {
                //x
            uint256 id;
        }
      """,
      name = "Abc.sol"
    ),
    InlineFile(
      """
          pragma solidity ^0.8.26;    
            
          import {Proposal} from "./Abc.sol";
                    //^
          contract B { 
            function doit(uint256[] storage array) {
                Proposal prop = Proposal(1);
            }
        }
    """
    )
  )

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

  fun testResolveStructFromAlias4() = testResolveBetweenFiles(
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

  fun testResolveStructFromInterface() = testResolveBetweenFiles(
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         interface InterfaceTest {
          struct Prop {
              uint prop1;
                  //x
              uint prop2;
          }
         }
    """,
      name = "a.sol"
    ),
    InlineFile("""
        pragma solidity ^0.8.26;
        
        import {InterfaceTest} from "./a.sol";

        contract C {
            InterfaceTest.Prop prop = InterfaceTest.Prop(0, 1);
            function f() public {
                prop.prop1;
                      //^
            }
       }
  """)
  )
}
