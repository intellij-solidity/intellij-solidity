package me.serce.solidity.lang.core.resolve

import com.intellij.psi.PsiElement

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

    fun testResolveStructFromInterfaceInFunctionCall() = testResolveBetweenFiles(
        InlineFile(
            code = """
         pragma solidity ^0.8.26;
         
         interface InterfaceTest {
          struct Prop {
                 //x
              uint256 prop1;
              uint256 prop2;
          }
         }
    """,
            name = "a.sol"
        ),
        InlineFile("""
        pragma solidity ^0.8.26;
        
        import {InterfaceTest} from "./a.sol";

        contract C {
            function f(InterfaceTest.Prop memory a) public {
                prop.prop1;
            }
            
            function g() public {
                f(InterfaceTest.Prop({
                                //^
                    prop1: 1,
                    prop2: 2
                }));
            }
       }
  """)
    )

    fun testResolveInterfaceInStructFunctionCall() = testResolveBetweenFiles(
        InlineFile(
            code = """
         pragma solidity ^0.8.26;
         
         interface InterfaceTest {
                 //x
          struct Prop {
              uint256 prop1;
              uint256 prop2;
          }
         }
    """,
            name = "a.sol"
        ),
        InlineFile("""
        pragma solidity ^0.8.26;
        
        import {InterfaceTest} from "./a.sol";

        contract C {
            function f(InterfaceTest.Prop memory a) public {
                prop.prop1;
            }
            
            function g() public {
                f(InterfaceTest.Prop({
                        //^
                    prop1: 1,
                    prop2: 2
                }));
            }
       }
  """)
    )

    fun testResolveStructMemberFromInterfaceInFunctionCall() = checkTestResolvePsiElementBetweenFiles(
        InlineFile(
            code = """
         pragma solidity ^0.8.26;
         
         interface InterfaceTest {
          struct Prop {
              uint256 prop1;
                     //x
              uint256 prop2;
          }
         }
    """, name = "a.sol"
        ), InlineFile(
            """
        pragma solidity ^0.8.26;
        
        contract C {
            function f(InterfaceTest.Prop memory a) public {
                prop.prop1;
            }
            
            function g() public {
                f(InterfaceTest.Prop({
                    prop1: 1,
                    //^
                    prop2: 2
                }));
            }
       }
    """
        )
    )

    fun checkTestResolvePsiElementBetweenFiles(file1: InlineFile, file2: InlineFile) {
        myFixture.openFileInEditor(file2.psiFile.virtualFile)
        val (refElement, _) = findElementAndDataInEditor<PsiElement>("^")
        val resolved = checkNotNull(refElement.reference?.resolve()) {
            "failed to resolve ${refElement.text}"
        }
        myFixture.openFileInEditor(file1.psiFile.virtualFile)
        val (resElement, _) = findElementAndDataInEditor<PsiElement>("x")
        assertEquals(resElement, resolved)
    }
}
