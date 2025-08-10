package me.serce.solidity.ide.inspections.fixes

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiDocumentManager
import java.io.File
import me.serce.solidity.utils.SolTestBase
import org.junit.Assert.assertEquals

class ImportFileActionTest : SolTestBase() {

  fun testExecuteAddsImport() {
    val nodeModule = myFixture.addFileToProject("node_modules/lib/Lib.sol", "contract Lib {}")
    val installed = myFixture.addFileToProject(
      "installed_contracts/github.com/owner/repo/contracts/C.sol",
      "contract C {}",
    )
    val remapIo = File(project.basePath!!, "remappings.txt").apply {
      parentFile.mkdirs()
      writeText("@oz=lib/openzeppelin")
    }
    LocalFileSystem.getInstance().refreshAndFindFileByIoFile(remapIo)
    val remapped = myFixture.addFileToProject(
      "lib/openzeppelin/token/ERC20.sol",
      "contract ERC20 {}",
    )
    val sibling = myFixture.addFileToProject("contracts/Util.sol", "contract Util {}")

    var idx = 0
    fun check(expected: String, to: com.intellij.psi.PsiFile) {
      val sourceFile = myFixture.addFileToProject("contracts/Main${++idx}.sol", "contract Main {}")
      myFixture.configureFromExistingVirtualFile(sourceFile.virtualFile)
      val source = myFixture.file
      val action = ImportFileAction(myFixture.editor, source, setOf(to))
      action.execute()
      PsiDocumentManager.getInstance(project).commitAllDocuments()
      val expectedText = "import \"$expected\";contract Main {}"
      assertEquals(expectedText, source.text.trim())
    }

    check("lib/Lib.sol", nodeModule)
    check("github.com/owner/repo/C.sol", installed)
    check("@oz/token/ERC20.sol", remapped)
    check("./Util.sol", sibling)
  }

}
