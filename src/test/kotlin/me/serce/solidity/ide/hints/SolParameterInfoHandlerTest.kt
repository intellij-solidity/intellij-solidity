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
  fun testEmptyParameters() = checkByText("""
        contract A {
            function foo() {}

            function main() {
                foo(/*caret*/);
            }
        }
    """, "<no arguments>", 0)

  fun testInt() = checkByText("""
        contract A {
            function foo(uint256 a) {}

            function main() {
                foo(1/*caret*/);
            }
        }
    """, "uint256 a", 0)

  fun testSome() = checkByText("""
        contract A {
            function foo(uint256 a) {}
            
            function foo(string a) {}

            function main() {
                foo(1/*caret*/);
            }
        }
    """, listOf("uint256 a", "string a"), 0)

  fun testLibrary() = checkByText("""
        library Lib {
            function bar(uint _self, uint _param) {}
        }

        contract A {
            using Lib for uint;

            function main(uint foo) {
                foo.bar(342/*caret*/);
            }
        }
    """, "uint _param", 0)

  fun testOtherContract() = checkByText("""
        contract Test {
            function foo(uint256 a) {}
        }

        contract A {
            Test test;

            function main() {
                test.foo(1/*caret*/);
            }
        }
    """, "uint256 a", 0)

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
    """, "<no arguments>", 0)

  fun testMultipleParameter2() = checkByText("""
        contract A {
            function foo(uint256 a, int b) {}

            function main() {
                foo(1, 2/*caret*/);
            }
        }
    """, "uint256 a, int b", 1)

  fun testMultipleParameter3() = checkByText("""
        contract A {
            function foo(string a, int b, address c) {}

            function main() {
                foo("1"/*caret*/, 2, 0x000d);
            }
        }
    """, "string a, int b, address c", 0)

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
    handler.updateParameterInfo(element, updateContext)
    TestCase.assertEquals(index, updateContext.currentParameter)
  }
}
