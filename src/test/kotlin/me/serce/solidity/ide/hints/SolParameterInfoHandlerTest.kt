package me.serce.solidity.ide.hints

import com.intellij.psi.PsiElement
import com.intellij.testFramework.utils.parameterInfo.MockCreateParameterInfoContext
import com.intellij.testFramework.utils.parameterInfo.MockParameterInfoUIContext
import com.intellij.testFramework.utils.parameterInfo.MockUpdateParameterInfoContext
import junit.framework.AssertionFailedError
import junit.framework.TestCase
import me.serce.solidity.utils.SolTestBase
import org.intellij.lang.annotations.Language
import java.awt.Color

class SolParameterInfoHandlerTest : SolTestBase() {
  fun testEvent() = checkByText("""
        contract A {
            event SomeEvent(uint value, string s);
        
            function main() {
                emit SomeEvent(/*caret*/);
            }
        }
    """, "uint256 value, string s", 0)

  fun testEventWithoutSemicolon() = checkByText("""
        contract A {
            event SomeEvent(uint value, string s);
        
            function main() {
                emit SomeEvent(/*caret*/)
            }
        }
    """, "uint256 value, string s", 0)

  fun testEvent2ndParam() = checkByText("""
        contract A {
            event SomeEvent(uint value, string s);
        
            function main() {
                emit SomeEvent(10,/*caret*/);
            }
        }
    """, "uint256 value, string s", 1)

  fun testEvent2ndParamWithoutSemicolon() = checkByText("""
        contract A {
            event SomeEvent(uint value, string s);
        
            function main() {
                emit SomeEvent(10,/*caret*/)
            }
        }
    """, "uint256 value, string s", 1)


  fun testError() = checkByText("""
        contract A {
            error AnError(uint value, string s);
        
            function main() {
                revert AnError(/*caret*/);
            }
        }
    """, "uint256 value, string s", 0)

  fun testErrorWithoutSemicolon() = checkByText("""
        contract A {
            error AnError(uint value, string s);
        
            function main() {
                revert AnError(/*caret*/)
            }
        }
    """, "uint256 value, string s", 0)

  fun testError2ndParam() = checkByText("""
        contract A {
            error AnError(uint value, string s);
        
            function main() {
                revert AnError(10,/*caret*/);
            }
        }
    """, "uint256 value, string s", 1)

  fun testError2ndParamWithoutSemicolon() = checkByText("""
        contract A {
            error AnError(uint value, string s);
        
            function main() {
                revert AnError(10,/*caret*/)
            }
        }
    """, "uint256 value, string s", 1)

  fun testStruct() = checkByText("""
      contract B {
          struct Prop {
              uint prop1;
              uint prop2;
          }

          Prop prop = Prop(0/*caret*/);
      }
    """, "uint256 prop1, uint256 prop2", 0)

  fun testStructWithoutSemicolon() = checkByText("""
      contract B {
          struct Prop {
              uint prop1;
              uint prop2;
          }

          Prop prop = Prop(/*caret*/)
      }
    """, "uint256 prop1, uint256 prop2", 0)

  fun testStruct2ndParam() = checkByText("""
      contract B {
          struct Prop {
              uint prop1;
              uint prop2;
          }

          Prop prop = Prop(0, /*caret*/);
      }
    """, "uint256 prop1, uint256 prop2", 1)

  fun testStruct2ndParamWithoutSemicolon() = checkByText("""
      contract B {
          struct Prop {
              uint prop1;
              uint prop2;
          }

          Prop prop = Prop(0, /*caret*/)
      }
    """, "uint256 prop1, uint256 prop2", 1)

  fun testEmptyParameters() = checkByText("""
        contract A {
            function foo() {}

            function main() {
                foo(/*caret*/);
            }
        }
    """, "<no parameters>", 0)

  fun testInt() = checkByText("""
        contract A {
            function foo(uint256 a) {}

            function main() {
                foo(/*caret*/);
            }
        }
    """, "uint256 a", 0)

  fun testIntWithoutSemicolon() = checkByText("""
        contract A {
            function foo(uint256 a) {}

            function main() {
                foo(/*caret*/)
            }
        }
    """, "uint256 a", 0)

  fun testSome() = checkByText("""
        contract A {
            function foo(uint256 a) {}
            
            function foo(string a) {}

            function main() {
                foo(/*caret*/);
            }
        }
    """, listOf("uint256 a", "string a"), 0)

  fun testSomeWithOverridden() = checkByText("""
        contract Base {
            function foo(uint256 a) {} 
        }
    
        contract A is Base {            
            function foo(string a) {}

            function main() {
                foo(/*caret*/);
            }
        }
    """, listOf("uint256 a", "string a"), 0)

  fun testSomeWithOverriddenWithoutSemicolon() = checkByText("""
        contract Base {
            function foo(uint256 a) {} 
        }
    
        contract A is Base {            
            function foo(string a) {}

            function main() {
                foo(/*caret*/)
            }
        }
    """, listOf("uint256 a", "string a"), 0)

  fun testLibrary() = checkByText("""
        library Lib {
            function bar(uint _self, uint256 _param, uint256 _param2) {}
        }

        contract A {
            using Lib for uint;

            function main(uint foo) {
                foo.bar(342/*caret*/);
            }
        }
    """, "uint256 _param, uint256 _param2", 0)

  fun testLibrary2ndParam() = checkByText("""
        library Lib {
            function bar(uint _self, uint256 _param, uint256 _param2) {}
        }

        contract A {
            using Lib for uint;

            function main(uint foo) {
                foo.bar(342, /*caret*/);
            }
        }
    """, "uint256 _param, uint256 _param2", 1)

  fun testLibrary2ndParamWithoutSemicolon() = checkByText("""
        library Lib {
            function bar(uint _self, uint256 _param, uint256 _param2) {}
        }

        contract A {
            using Lib for uint;

            function main(uint foo) {
                foo.bar(342, /*caret*/)
            }
        }
    """, "uint256 _param, uint256 _param2", 1)

  fun testOtherContract() = checkByText("""
        contract Test {
            function foo(uint256 a) {}
        }

        contract A {
            Test test;

            function main() {
                test.foo(/*caret*/);
            }
        }
    """, "uint256 a", 0)

  fun testOtherContractMultiParameters2ndParam() = checkByText("""
        contract Test {
            function foo(uint256 a,uint256 b) {}
        }

        contract A {
            Test test;

            function main() {
                test.foo(1,/*caret*/);
            }
        }
    """, "uint256 a, uint256 b", 1)

  fun testOtherContractWithoutSemicolon() = checkByText("""
        contract Test {
            function foo(uint256 a) {}
        }

        contract A {
            Test test;

            function main() {
                test.foo(/*caret*/)
            }
        }
    """, "uint256 a", 0)

  fun testOtherContractMultiParametersWithoutSemicolon() = checkByText("""
        contract Test {
            function foo(uint256 a,uint256 b) {}
        }

        contract A {
            Test test;

            function main() {
                test.foo(/*caret*/)
            }
        }
    """, "uint256 a, uint256 b", 0)

  fun testOtherContractMultiParametersWithoutSemicolon2ndParam() = checkByText("""
        contract Test {
            function foo(uint256 a,uint256 b) {}
        }

        contract A {
            Test test;

            function main() {
                test.foo(1,/*caret*/)
            }
        }
    """, "uint256 a, uint256 b", 1)

  fun testLibraryEmpty() = checkByText("""
        library Lib {
            function bar(uint _self) {}
        }

        contract A {
            using Lib for uint;

            function main(uint foo) {
                foo.bar(/*caret*/);
            }
        }
    """, "<no parameters>", 0)

  fun testMultipleParameter2() = checkByText("""
        contract A {
            function foo(uint256 a, int b) {}

            function main() {
                foo(1, 2/*caret*/);
            }
        }
    """, "uint256 a, int256 b", 1)

  fun testMultipleParameter3() = checkByText("""
        contract A {
            function foo(string a, int b, address c) {}

            function main() {
                foo("1"/*caret*/, 2, 0x000d);
            }
        }
    """, "string a, int256 b, address c", 0)

  fun testMultipleParameterWithMissingValue() = checkByText(
    """
        contract A {
            function a(
                uint256 x,
                uint256 y,
                uint256 z
            ) public {}

            function b() public {
                a(12, /*caret*/);
            }
        }
    """, "uint256 x, uint256 y, uint256 z", 1
  )

  fun testMultipleParameterWithMissingValue2() = checkByText(
    """
        contract A {
            function a(
                uint256 x,
                uint256 y,
                uint256 z
            ) public {}

            function b() public {
                a(12/*caret*/);
            }
        }
    """, "uint256 x, uint256 y, uint256 z", 0
  )

  fun testMultipleParameterWithMissingValue3() = checkByText(
    """
        contract A {
            function a(
                uint256 x,
                uint256 y,
                uint256 z
            ) public {}

            function b() public {
                a(12, 10,/*caret*/)
            }
        }
    """, "uint256 x, uint256 y, uint256 z", 2
  )

  fun testOtherContractInAnotherFileMultiParameters2ndParam() {
    InlineFile(
      code = """
        contract Test {
            function foo (uint256 a, uint256 b) {}
        }
    """, name = "Test.sol"
    )
    checkByText(
      """
        import "./Test.sol";
        contract A {
            Test test;

            function main() {
                test.foo(1,/*caret*/);
            }
        }
    """, "uint256 a, uint256 b", 1
    )
  }

  fun testOtherContractInAnotherFileMultiParameters2ndParamWithoutSemicolon() {
    InlineFile(
      code = """
        contract Test {
            function foo (uint256 a, uint256 b) {}
        }
    """, name = "Test.sol"
    )
    checkByText(
      """
        import "./Test.sol";
        contract A {
            Test test;

            function main() {
                test.foo(1,/*caret*/)
            }
        }
    """, "uint256 a, uint256 b", 1
    )
  }

  fun testOtherContractInAnotherFileWithAliasMultiParameters2ndParam() {
    InlineFile(
      code = """
        pragma solidity ^0.8.10;
        contract ContractTest {
            function foo (uint256 a, uint256 b) {}
        }
    """, name = "contractTest.sol"
    )
    checkByText(
      """
        pragma solidity ^0.8.10;

        import "./contractTest.sol" as Test;
        contract A {
            Test.ContractTest test = new Test.ContractTest();
        
            function main() public  {
                test.foo(1,/*caret*/);
            }
        }
    """, "uint256 a, uint256 b", 1
    )
  }

  fun testOtherContractInAnotherFileWithAliasMultiParameters2ndParamWithoutSemicolon() {
    InlineFile(
      code = """
        pragma solidity ^0.8.10;
        contract ContractTest {
            function foo (uint256 a, uint256 b) {}
        }
    """, name = "contractTest.sol"
    )
    checkByText(
      """
        pragma solidity ^0.8.10;

        import "./contractTest.sol" as Test;
        contract A {
            Test.ContractTest test = new Test.ContractTest();
        
            function main() public  {
                test.foo(1,/*caret*/)
            }
        }
    """, "uint256 a, uint256 b", 1
    )
  }

  private fun checkByText(@Language("Solidity") code: String, hint: String, index: Int) {
    checkByText(code, listOf(hint), index)
  }

  private fun checkByText(@Language("Solidity") code: String, hints: List<String>, index: Int) {
    myFixture.configureByText("main.sol", code.replace("/*caret*/", "<caret>"))
    val handler = SolParameterInfoHandler()
    val createContext = MockCreateParameterInfoContext(myFixture.editor, myFixture.file)

    val el = handler.findElementForParameterInfo(createContext)
    el ?: throw AssertionFailedError("Hint not found")
    handler.showParameterInfo(el, createContext)
    val items = createContext.itemsToShow ?: throw AssertionFailedError("Parameters are not shown")
    TestCase.assertEquals(hints.size, items.size)
    for (hint in hints.withIndex()) {
      val context = object : MockParameterInfoUIContext<PsiElement>(el) {
        override fun getDefaultParameterColor(): Color = Color.GRAY
      }
      handler.updateUI(items[hint.index] as SolArgumentsDescription, context)
      TestCase.assertEquals(hint.value, context.text)
    }

    val updateContext = MockUpdateParameterInfoContext(myFixture.editor, myFixture.file)
    val element = handler.findElementForUpdatingParameterInfo(updateContext) ?: throw AssertionFailedError("Parameter not found")
    updateContext.parameterOwner = element
    handler.updateParameterInfo(element, updateContext)
    TestCase.assertEquals(index, updateContext.currentParameter)
  }
}
