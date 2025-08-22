package me.serce.solidity.ide.inspections.fixes

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.utils.SolTestBase
import org.junit.Assert.assertEquals

class RenameFixTest : SolTestBase() {
  fun testRenameContract() {
    val file = InlineFile("contract A {}").psiFile
    val contract = file.children.filterIsInstance<SolContractDefinition>().first()
    WriteCommandAction.runWriteCommandAction(project) {
      RenameFix(contract, "B").invoke(project, file, contract, contract)
    }
    PsiDocumentManager.getInstance(project).commitAllDocuments()
    assertEquals("contract B {}", file.text.trim())
  }
}
