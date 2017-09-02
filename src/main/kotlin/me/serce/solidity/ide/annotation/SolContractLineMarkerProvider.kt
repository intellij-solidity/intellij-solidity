package me.serce.solidity.ide.annotation

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import me.serce.solidity.ide.navigation.findAllImplementations
import me.serce.solidity.ide.navigation.findImplementations
import me.serce.solidity.lang.psi.SolContractDefinition

class SolContractLineMarkerProvider : LineMarkerProvider {
  override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

  override fun collectSlowLineMarkers(elements: MutableList<PsiElement>, result: MutableCollection<LineMarkerInfo<PsiElement>>) {
    for (el in elements) {
      if (el !is SolContractDefinition) {
        continue
      }
      val identifier = el.identifier
      if (identifier == null) {
        return
      }
      val targets = el.findImplementations()
      if (targets.findFirst() == null) {
        continue
      }

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
