package me.serce.solidity.ide.actions

import com.intellij.ide.IdeView
import com.intellij.ide.actions.TestDialogBuilder
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.psi.PsiDirectory
import com.intellij.psi.impl.file.PsiDirectoryFactory
import me.serce.solidity.utils.SolLightPlatformCodeInsightFixtureTestCase
import org.intellij.lang.annotations.Language

class SolCreateFileActionTest: SolLightPlatformCodeInsightFixtureTestCase() {
  fun testCreateContract() {
    val directoryFactory = PsiDirectoryFactory.getInstance(project)
    val dir = directoryFactory.createDirectory(myFixture.tempDirFixture.findOrCreateDir("foo"))
    val ctx = SimpleDataContext.builder()
      .add(LangDataKeys.IDE_VIEW, TestIdeView(dir))
      .add(CommonDataKeys.PROJECT, project)
      .add(TestDialogBuilder.TestAnswers.KEY, TestDialogBuilder.TestAnswers("myContract", SMART_CONTRACT_TEMPLATE))
      .build();

    val event = AnActionEvent.createFromDataContext("", null, ctx)
    ActionManager.getInstance().getAction("solidity.file.create")!!.actionPerformed(event)

    val file = dir.findFile("myContract.sol")!!
    @Language("Solidity")
    val content = """
      // SPDX-License-Identifier: UNLICENSED
      pragma solidity ^0.8.10;
      
      contract myContract {
          constructor(){
      
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
