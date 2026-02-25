package me.serce.solidity.ide.inspections.fixes

import com.intellij.psi.PsiDocumentManager
import me.serce.solidity.utils.SolTestBase
import org.junit.Assert.assertEquals

class ImportFileActionTest : SolTestBase() {

  fun testExecuteAddsImport() {
    val nodeModule = myFixture.addFileToProject("node_modules/lib/Lib.sol", "contract Lib {}")
    val installed = myFixture.addFileToProject(
      "installed_contracts/github.com/owner/repo/contracts/C.sol",
      "contract C {}",
    )
    myFixture.addFileToProject("remappings.txt", "@oz=lib/openzeppelin")
    val remapped = myFixture.addFileToProject(
      "lib/openzeppelin/token/ERC20.sol",
      "contract ERC20 {}",
    )
    val sibling = myFixture.addFileToProject("contracts/Util.sol", "contract Util {}")

    check("lib/Lib.sol", nodeModule)
    check("github.com/owner/repo/C.sol", installed)
    check("@oz/token/ERC20.sol", remapped)
    check("./Util.sol", sibling)
  }

  fun testExecuteAddsImportUsingFoundryTomlRemapping() {
    myFixture.addFileToProject(
      "foundry.toml",
      """
      [profile.default]
      remappings = ["@oz/=lib/openzeppelin/"]
      """.trimIndent()
    )
    val remapped = myFixture.addFileToProject(
      "lib/openzeppelin/token/ERC20.sol",
      "contract ERC20 {}",
    )

    val sourceFile = myFixture.addFileToProject("contracts/MainToml.sol", "contract Main {}")
    myFixture.configureFromExistingVirtualFile(sourceFile.virtualFile)
    val source = myFixture.file
    val action = ImportFileAction(myFixture.editor, source, setOf(remapped))
    action.execute()
    PsiDocumentManager.getInstance(project).commitAllDocuments()

    assertEquals("import \"@oz/token/ERC20.sol\";contract Main {}", source.text.trim())
  }

  fun testBuildImportPathWithCustomRemappingTarget() {
    myFixture.addFileToProject(
      "remappings.txt",
      "@deps/=dependencies/@deps/"
    )
    val customDep = myFixture.addFileToProject(
      "dependencies/@deps/token/ERC20.sol",
      "contract ERC20 {}"
    )
    check("@deps/token/ERC20.sol", customDep)
  }

  private var idx = 0
  private fun check(expected: String, to: com.intellij.psi.PsiFile) {
    val sourceFile = myFixture.addFileToProject("contracts/Main${++idx}.sol", "contract Main {}")
    myFixture.configureFromExistingVirtualFile(sourceFile.virtualFile)
    val source = myFixture.file
    val action = ImportFileAction(myFixture.editor, source, setOf(to))
    action.execute()
    PsiDocumentManager.getInstance(project).commitAllDocuments()
    val expectedText = "import \"$expected\";contract Main {}"
    assertEquals(expectedText, source.text.trim())
  }

}
