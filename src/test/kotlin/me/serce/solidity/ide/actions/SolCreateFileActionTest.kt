package me.serce.solidity.ide.actions

import com.intellij.ide.IdeView
import com.intellij.ide.actions.TestDialogBuilder
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.psi.PsiDirectory
import com.intellij.psi.impl.file.PsiDirectoryFactory
import com.intellij.testFramework.MapDataContext
import me.serce.solidity.utils.SolLightPlatformCodeInsightFixtureTestCase
import org.intellij.lang.annotations.Language

class SolCreateFileActionTest: SolLightPlatformCodeInsightFixtureTestCase() {

  fun testCreateContract() {
    val directoryFactory = PsiDirectoryFactory.getInstance(project)
    val dir = directoryFactory.createDirectory(myFixture.tempDirFixture.findOrCreateDir("foo"))
    val ctx = MapDataContext(mapOf(
      LangDataKeys.IDE_VIEW to TestIdeView(dir),
      CommonDataKeys.PROJECT to project,
      TestDialogBuilder.TestAnswers.KEY to TestDialogBuilder.TestAnswers("myContract", SMART_CONTRACT_TEMPLATE)
    ))
    val event = AnActionEvent.createFromDataContext("", null, ctx)
    ActionManager.getInstance().getAction("solidity.file.create")!!.actionPerformed(event)

    val file = dir.findFile("myContract.sol")!!
    @Language("Solidity")
    val content = """
      pragma solidity ^0.8.0;
      
      contract myContract {
          function myContract(){
      
          }
      }
      
      """.trimIndent()
    assertEquals(content, file.text)
  }

  private class TestIdeView(private val dir: PsiDirectory) : IdeView {
    override fun getDirectories() = arrayOf(dir)
    override fun getOrChooseDirectory() = dir
  }
}
