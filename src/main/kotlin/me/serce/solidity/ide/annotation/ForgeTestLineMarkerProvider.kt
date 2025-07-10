package me.serce.solidity.ide.annotation

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import me.serce.solidity.ide.run.ForgeTestRunConfigurationProducer
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.lang.psi.Visibility
import javax.swing.Icon

class ForgeTestLineMarkerProvider : LineMarkerProvider {
  private val testIcon: Icon = AllIcons.RunConfigurations.TestState.Run

  override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

  override fun collectSlowLineMarkers(
      elements: MutableList<out PsiElement>,
      result: MutableCollection<in LineMarkerInfo<*>>
  ) {
    if (elements.isEmpty()) return

    val file = elements[0].containingFile ?: return
    if (!file.name.endsWith(".t.sol")) return

    for (element in elements) {
      when (element) {
        is SolContractDefinition -> {
          element.identifier?.let { identifier ->
            result.add(
              createTestLineMarker(
                identifier,
                "Run Forge Tests in ${element.name}",
                element.name ?: ""
              )
            )
          }
        }

        is SolFunctionDefinition -> {
          if (element.isTestFunction()) {
            element.identifier?.let { identifier ->
              result.add(
                createTestLineMarker(
                  identifier,
                  "Run Forge Test ${element.name}",
                  "${element.contract?.name}.${element.name}"
                )
              )
            }
          }
        }
      }
    }
  }

  private fun createTestLineMarker(
      element: PsiElement,
      tooltip: String,
      testName: String
  ): LineMarkerInfo<PsiElement> {
    val producer = ForgeTestRunConfigurationProducer()
    return object : LineMarkerInfo<PsiElement>(
      element,
      element.textRange,
      testIcon,
      { tooltip },
      { _, el -> producer.runTest(el.project, testName) },
        GutterIconRenderer.Alignment.LEFT,
      { testName }
    ) {}
  }

  private fun SolFunctionDefinition.isTestFunction(): Boolean {
    // Check if function name starts with 'test' and is public/external
    return name?.startsWith("test") == true &&
      (visibility == null || visibility == Visibility.PUBLIC || visibility == Visibility.EXTERNAL)
  }
}
