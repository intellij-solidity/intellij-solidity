package me.serce.solidity.lang.completion

import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.stubs.StubIndex
import me.serce.solidity.ide.SolidityIcons
import me.serce.solidity.lang.psi.SolUserDefinedTypeName
import me.serce.solidity.lang.stubs.SolGotoClassIndex

object SolCompleter {
  fun completeTypeName(element: SolUserDefinedTypeName): Array<out LookupElement> {
    val project = element.project
    val allTypeNames = StubIndex.getInstance().getAllKeys(
      SolGotoClassIndex.KEY,
      project
    )
    return allTypeNames
      .map { LookupElementBuilder.create(it, it).withIcon(SolidityIcons.CONTRACT) }
      .toTypedArray()
  }
}
