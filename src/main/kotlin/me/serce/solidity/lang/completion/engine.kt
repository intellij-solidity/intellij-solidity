package me.serce.solidity.lang.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubIndex
import me.serce.solidity.ide.SolidityIcons
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.lang.psi.SolModifierDefinition
import me.serce.solidity.lang.psi.SolUserDefinedTypeName
import me.serce.solidity.lang.psi.SolVarLiteral
import me.serce.solidity.lang.resolve.SolResolver
import me.serce.solidity.lang.stubs.SolGotoClassIndex
import me.serce.solidity.lang.stubs.SolModifierIndex

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

  fun completeModifier(element: PsiElement): Array<out LookupElement> {
    val project = element.project
    val allModifiers = StubIndex.getInstance().getAllKeys(
      SolModifierIndex.KEY,
      project
    )
    return allModifiers
      .map { LookupElementBuilder.create(it, it).withIcon(SolidityIcons.FUNCTION) }
      .toTypedArray()
  }

  fun completeLiteral(element: SolVarLiteral): Array<out LookupElement> {
    return SolResolver.lexicalDeclarations(element)
      .take(25) // TODO: is it needed? Try to elaborate on that
      .toList()
      .map { LookupElementBuilder.create(it, it.name ?: "").withIcon(SolidityIcons.STATE_VAR) }
      .toTypedArray()
  }
}
