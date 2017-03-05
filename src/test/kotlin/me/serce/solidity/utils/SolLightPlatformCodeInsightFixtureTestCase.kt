package me.serce.solidity.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import com.intellij.util.ObjectUtils
import com.intellij.util.PlatformUtils
import com.intellij.util.containers.ContainerUtil

abstract class SolLightPlatformCodeInsightFixtureTestCase : LightPlatformCodeInsightFixtureTestCase {
  private var myBackedUpPlatformPrefix: String? = null
  private val myIsSmallIde: Boolean

  protected constructor(isSmallIde: Boolean) {
    myIsSmallIde = isSmallIde
  }

  protected constructor() {
    myIsSmallIde = false
  }

  override fun setUp() {
    if (myIsSmallIde) {
      myBackedUpPlatformPrefix = PlatformUtils.getPlatformPrefix()
      System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, PlatformUtils.PYCHARM_PREFIX)
    }
    super.setUp()
  }

  override fun tearDown() {
    if (myIsSmallIde) {
      System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, myBackedUpPlatformPrefix!!)
    }
    super.tearDown()
  }

  protected fun setUpProjectSdk() {
    ApplicationManager.getApplication().runWriteAction {
      val sdk = projectDescriptor.sdk
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
    return ObjectUtils.assertNotNull(PsiTreeUtil.getParentOfType(focused, clazz))
  }
}
