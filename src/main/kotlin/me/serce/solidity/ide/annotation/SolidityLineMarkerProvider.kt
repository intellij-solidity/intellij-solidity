package me.serce.solidity.ide.annotation

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.ide.util.PsiElementListCellRenderer
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import me.serce.solidity.ide.navigation.findAllImplementations
import me.serce.solidity.ide.navigation.findImplementations
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.lang.resolve.function.SolFunctionResolver

class SolidityLineMarkerProvider : LineMarkerProvider {
  override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

  override fun collectSlowLineMarkers(elements: List<PsiElement>, result: MutableCollection<LineMarkerInfo<PsiElement>>) {
    for (el in elements) {
      when (el) {
        is SolContractDefinition -> {
          val identifier = el.identifier
          if (identifier != null) {
            val targets = el.findImplementations()
            if (targets.findFirst() != null) {
              val info = NavigationGutterIconBuilder
                .create(AllIcons.Gutter.OverridenMethod)
                .setTargets(el.findAllImplementations())
                .setPopupTitle("Go to implementation")
                .setTooltipText("Has implementations")
                .createLineMarkerInfo(identifier)
              result.add(info)
            }
          }
        }
        is SolFunctionDefinition -> {
          val identifier = el.identifier
          if (identifier != null) {
            val overriden = SolFunctionResolver.collectOverriden(el)
            if (!overriden.isEmpty()) {
              val info = NavigationGutterIconBuilder
                .create(AllIcons.Gutter.OverridingMethod)
                .setTargets(overriden)
                .setPopupTitle("Go to overriden functions")
                .setTooltipText("Overrides function")
                .setCellRenderer(FunctionCellRenderer(el.containingFile))
                .createLineMarkerInfo(identifier)
              result.add(info)
            }
            val overrides = SolFunctionResolver.collectOverrides(el)
            if (!overrides.isEmpty()) {
              val info = NavigationGutterIconBuilder
                .create(AllIcons.Gutter.OverridenMethod)
                .setTargets(overrides)
                .setPopupTitle("Is overriden")
                .setTooltipText("Is overriden in subcontracts")
                .setCellRenderer(FunctionCellRenderer(el.containingFile))
                .createLineMarkerInfo(identifier)
              result.add(info)
            }
          }
        }
      }
    }
  }
}

class FunctionCellRenderer(private val file: PsiFile) : PsiElementListCellRenderer<SolFunctionDefinition>() {
  override fun getContainerText(element: SolFunctionDefinition?, name: String?): String? =
    if (element?.containingFile != file) {
      element?.containingFile?.name
    } else {
      null
    }

  override fun getIconFlags(): Int = Iconable.ICON_FLAG_VISIBILITY

  override fun getElementText(element: SolFunctionDefinition?): String = element?.contract?.name!!
}
