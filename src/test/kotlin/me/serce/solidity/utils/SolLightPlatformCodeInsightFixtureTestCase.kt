package me.serce.solidity.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.containers.ContainerUtil

abstract class SolLightPlatformCodeInsightFixtureTestCase : BasePlatformTestCase() {
  private var myBackedUpPlatformPrefix: String? = null
  private val myIsSmallIde: Boolean = false

  protected fun setUpProjectSdk() {
    ApplicationManager.getApplication().runWriteAction {
      val sdk = projectDescriptor.sdk ?: return@runWriteAction
      ProjectJdkTable.getInstance().addJdk(sdk)
      ProjectRootManager.getInstance(myFixture.project).projectSdk = sdk
    }
  }

  protected fun launchIntention(name: String) {
    val availableIntentions = myFixture.filterAvailableIntentions(name)
    val action = ContainerUtil.getFirstItem(availableIntentions)
    assertNotNull(action)
    myFixture.launchAction(action!!)
  }

  @JvmOverloads protected fun assertNoIntentionsAvailable(name: String, message: String? = null) {
    val availableIntentions = myFixture.filterAvailableIntentions(name)
    val action = ContainerUtil.getFirstItem(availableIntentions)
    assertNull(message, action)
  }

  protected fun <T : PsiElement> getElementAtCaret(clazz: Class<T>): T {
    val offset = myFixture.editor.caretModel.offset
    val focused = myFixture.file.findElementAt(offset)
    return PsiTreeUtil.getParentOfType(focused, clazz)!!
  }
}
