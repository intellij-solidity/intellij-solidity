package me.serce.solidity.ide.hints

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.TestActionEvent
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.utils.SolTestBase
import org.intellij.lang.annotations.Language

class SolGasEstimationTest : SolTestBase() {

  private fun ensureProject(@Language("JSON") json: String) {
    if (myFixture.tempDirFixture.getFile("hardhat.config.js") == null) {
      myFixture.tempDirFixture.createFile("hardhat.config.js")
    }
    val buildInfoDir = myFixture.tempDirFixture.findOrCreateDir("artifacts/build-info")
    val existing = buildInfoDir.findChild("info.json")
    if (existing != null) {
      VfsUtil.saveText(existing, json)
    } else {
      myFixture.tempDirFixture.createFile("artifacts/build-info/info.json", json)
    }
  }

  private fun setup(@Language("JSON")json: String) {
    ensureProject(json)
    val action = ShowGasEstimationAction()
    val dataContext = DataContext { dataId -> if (CommonDataKeys.PROJECT.name == dataId) project else null }
    val event = TestActionEvent.createTestEvent(action, dataContext)
    action.setSelected(event, true)
  }

  fun testExternalStructParam() {
    val json = """
      {
        "output": {
          "contracts": {
            "contracts/A.sol": {
              "A": {
                "evm": {
                  "gasEstimates": {
                    "external": { "foo(struct S memory)": "100" }
                  }
                }
              }
            }
          }
        }
      }
    """.trimIndent()

    setup(json)

    val vf = myFixture.tempDirFixture.createFile(
      "contracts/A.sol",
      """
        struct S { uint a; }
        contract A {
          function foo(S memory s) public {}
        }
      """.trimIndent()
    )
    val file = PsiManager.getInstance(project).findFile(vf)!!

    val func = PsiTreeUtil.findChildrenOfType(file, SolFunctionDefinition::class.java).first { it.name == "foo" }
    val provider = SolGasEstimationInlayProvider()
    assertEquals("\u26FD 100", provider.getHint(func, file))
  }

  fun testInternalStorageLocation() {
    val json = """
      {
        "output": {
          "contracts": {
            "contracts/B.sol": {
              "B": {
                "evm": {
                  "gasEstimates": {
                    "internal": { "bar(uint[] memory)": "77" }
                  }
                }
              }
            }
          }
        }
      }
    """.trimIndent()

    setup(json)

    val vf = myFixture.tempDirFixture.createFile(
      "contracts/B.sol",
      """
        contract B {
          function bar(uint[] memory arr) internal {}
        }
      """.trimIndent()
    )
    val file = PsiManager.getInstance(project).findFile(vf)!!

    val func = PsiTreeUtil.findChildrenOfType(file, SolFunctionDefinition::class.java).first { it.name == "bar" }
    val provider = SolGasEstimationInlayProvider()
    assertEquals("\u26FD 77", provider.getHint(func, file))
  }

  fun testCollapseNodes() {
    val json = """
      {
        "output": {
          "contracts": {
            "contracts/C.sol": {
              "C": {
                "evm": {
                  "gasEstimates": {
                    "external": { "foo(uint256)": "12", "foo(uint8)": "12" }
                  }
                }
              }
            }
          }
        }
      }
    """.trimIndent()

    setup(json)

    val vf = myFixture.tempDirFixture.createFile(
      "contracts/C.sol",
      """
        contract C {
          function foo() public {}
        }
      """.trimIndent()
    )
    val file = PsiManager.getInstance(project).findFile(vf)!!

    val func = PsiTreeUtil.findChildrenOfType(file, SolFunctionDefinition::class.java).first { it.name == "foo" }
    val provider = SolGasEstimationInlayProvider()
    assertEquals("\u26FD 12", provider.getHint(func, file))
  }

  fun testNullGasEstimates() {
    val json = """
      {
        "output": {
          "contracts": {
            "contracts/D.sol": {
              "D": {
                "evm": {
                  "gasEstimates": null
                }
              }
            }
          }
        }
      }
    """.trimIndent()

    setup(json)

    val vf = myFixture.tempDirFixture.createFile(
      "contracts/D.sol",
      """
        contract D {
          function foo() public {}
        }
      """.trimIndent()
    )
    val file = PsiManager.getInstance(project).findFile(vf)!!

    val func = PsiTreeUtil.findChildrenOfType(file, SolFunctionDefinition::class.java).first { it.name == "foo" }
    val provider = SolGasEstimationInlayProvider()
    assertEquals("\u26FD null", provider.getHint(func, file))
  }
}
