package me.serce.solidity.ide.refactoring

import me.serce.solidity.utils.SolTestBase
import org.intellij.lang.annotations.Language

class RenameTest : SolTestBase() {
  fun testContractName() = doTest("Newname", """
        contract Abcd {
            function Abcd() {}
        }

        contract BBB {
            /*caret*/Abcd field;

            function doSomething(Abcd aaa) returns(Abcd) {
               Abcd aaaa = aaa;
               return aaaa;
            }
        }
    """, """
        contract Newname {
            function Newname() {}
        }

        contract BBB {
            Newname field;

            function doSomething(Newname aaa) returns(Newname) {
               Newname aaaa = aaa;
               return aaaa;
            }
        }
    """)

  fun testField() = doTest("myNewname", """
        contract BBB {
            Abcd /*caret*/myField;

            function doSomething(Abcd fff) returns(Abcd) {
               Abcd aaaa = myField;

               myField++;
               return myField;
            }
        }
    """, """
        contract BBB {
            Abcd myNewname;

            function doSomething(Abcd fff) returns(Abcd) {
               Abcd aaaa = myNewname;

               myNewname++;
               return myNewname;
            }
        }
    """)

  fun testLocal() = doTest("newname", """
        contract BBB {
            function doSomething() {
               BBB /*caret*/aaaa = myField;
               aaaa++;
               return aaaa;
            }
        }
    """, """
        contract BBB {
            function doSomething() {
               BBB newname = myField;
               newname++;
               return newname;
            }
        }
    """)

  fun testConstructor() = doTest("Newname", """
        contract BBB {
            function /*caret*/BBB() {
            }
        }
    """, """
        contract Newname {
            function Newname() {
            }
        }
    """)

  fun testFunctionModifier() = doTest("Newname", """
        contract BBB {
            modifier /*caret*/oldModifier {
            }

            function doSomething() public oldModifier {
            }
        }
    """, """
        contract BBB {
            modifier Newname {
            }

            function doSomething() public Newname {
            }
        }
    """)

  fun testFileRename() {
    val labFile = myFixture.configureByFile("imports/Lab.sol")
    val importingFile = myFixture.configureByFile("imports/nested/ImportingFile.sol")

    myFixture.renameElement(labFile, "AssetGatewayToken.sol")
    myFixture.openFileInEditor(importingFile.virtualFile)
    myFixture.checkResultByFile("imports/nested/ImportingFile_after.sol")
  }

  fun testLibraryRename() = doTest("lib2", """
        library /*caret*/lib {
            struct dog {
                uint256 a;
                uint256 b;
            }

            function f() internal returns (dog memory) {
                dog memory r;
                return r;
            }
        }

        contract c {
            function f() internal returns (lib.dog memory) {
                return lib.f();
            }
        }
    """, """
        library lib2 {
            struct dog {
                uint256 a;
                uint256 b;
            }

            function f() internal returns (dog memory) {
                dog memory r;
                return r;
            }
        }

        contract c {
            function f() internal returns (lib2.dog memory) {
                return lib2.f();
            }
        }
    """)

  fun testStructInALibraryScopeRename() = doTest(
    "cat", """
        library lib {
            struct dog/*caret*/ {
                uint256 a;
                uint256 b;
            }

            function f() internal returns (dog memory) {
                dog memory r;
                return r;
            }
        }

        contract c {
            function f() internal returns (lib.dog memory) {
                return lib.f();
            }
        }
    """, """
        library lib {
            struct cat {
                uint256 a;
                uint256 b;
            }

            function f() internal returns (cat memory) {
                cat memory r;
                return r;
            }
        }

        contract c {
            function f() internal returns (lib.cat memory) {
                return lib.f();
            }
        }
    """
  )


  private fun doTest(
    newName: String,
    @Language("Solidity") before: String,
    @Language("Solidity") after: String
  ) {
    InlineFile(before).withCaret()
    myFixture.renameElementAtCaret(newName)
    myFixture.checkResult(after)
  }

  override fun getTestDataPath() = "src/test/resources/fixtures/refactoring/rename/"
}
