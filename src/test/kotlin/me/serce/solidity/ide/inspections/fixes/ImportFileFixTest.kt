package me.serce.solidity.ide.inspections.fixes

import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import me.serce.solidity.lang.psi.SolReferenceElement
import me.serce.solidity.utils.SolTestBase
import org.junit.Assert.assertEquals

class ImportFileFixTest : SolTestBase() {
  fun testInvokeAddsImport() {
    myFixture.addFileToProject("B.sol", "contract B {}")
    val main = myFixture.addFileToProject("Main.sol", "contract Main is B {}")
    val ref = PsiTreeUtil.findChildrenOfType(main, SolReferenceElement::class.java).first { it.text == "B" }
    ImportFileFix(ref).invoke(project, main, ref, ref)
    PsiDocumentManager.getInstance(project).commitAllDocuments()
    val expected = "import \"./B.sol\";contract Main is B {}"
    assertEquals(expected, main.text.trim())
  }
}
