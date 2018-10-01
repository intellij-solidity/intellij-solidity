package me.serce.solidity.lang.completion

import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import me.serce.solidity.ide.SolidityIcons
import me.serce.solidity.ide.inspections.fixes.ImportFileAction
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolMemberAccessExpression
import me.serce.solidity.lang.psi.SolNamedElement
import me.serce.solidity.lang.resolve.SolResolver
import me.serce.solidity.lang.stubs.SolEventIndex
import me.serce.solidity.lang.stubs.SolGotoClassIndex
import me.serce.solidity.lang.stubs.SolModifierIndex
import me.serce.solidity.lang.types.SolContract
import me.serce.solidity.lang.types.SolEnum
import me.serce.solidity.lang.types.SolStruct
import me.serce.solidity.lang.types.type
import javax.swing.Icon

val TYPED_COMPLETION_PRIORITY = 15.0

object SolCompleter {

  fun completeEventName(element: PsiElement): Array<out LookupElementBuilder> {
    val project = element.project
    val allTypeNames = StubIndex.getInstance().getAllKeys(
      SolEventIndex.KEY,
      project
    )
    return allTypeNames
      .map {
        LookupElementBuilder
          .create(it, it)
          .withIcon(SolidityIcons.EVENT)
      }
      .toTypedArray()
  }

  fun completeTypeName(element: PsiElement): Array<out LookupElement> {
    val project = element.project
    val allTypeNames = StubIndex.getInstance().getAllKeys(
      SolGotoClassIndex.KEY,
      project
    )
    return allTypeNames
      .flatMap {
        StubIndex.getElements(SolGotoClassIndex.KEY, it, project, GlobalSearchScope.projectScope(project), SolNamedElement::class.java)
      }
      .filterIsInstance<SolContractDefinition>()
      .map { ContractLookupElement(it) }
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

  fun completeLiteral(element: PsiElement): Array<out LookupElement> {
    val declarations = SolResolver.lexicalDeclarations(element).take(25).toList()
    return declarations.createVarLookups()
  }

  fun completeMemberAccess(element: SolMemberAccessExpression): Array<out LookupElement> {
    val exprType = element.expression.type
    return when (exprType) {
      is SolStruct -> exprType.ref.variableDeclarationList.createVarLookups()
      is SolContract -> {
        val ref = exprType.ref
        (ref.collectSupers.flatMap { SolResolver.resolveTypeName(it) } + ref)
          .filterIsInstance<SolContractDefinition>()
          .flatMap { it.stateVariableDeclarationList }
          .createVarLookups()
      }
      is SolEnum -> {
        exprType.ref.enumValueList.createVarLookups(SolidityIcons.ENUM)
      }
      else -> emptyArray()
    }
  }

  private fun Collection<SolNamedElement>.createVarLookups(): Array<LookupElement> = createVarLookups(SolidityIcons.STATE_VAR)

  private fun Collection<SolNamedElement>.createVarLookups(icon: Icon): Array<LookupElement> {
    return map {
      LookupElementBuilder.create(it, it.name ?: "")
              .withIcon(icon)
    }.toTypedArray().map {
      PrioritizedLookupElement.withPriority(it, TYPED_COMPLETION_PRIORITY)
    }.toTypedArray()
  }
}

class ContractLookupElement(val contract: SolContractDefinition) : LookupElement() {
  override fun getLookupString(): String = contract.name!!

  override fun renderElement(presentation: LookupElementPresentation) {
    presentation.icon = SolidityIcons.CONTRACT
    presentation.itemText = contract.name
    presentation.typeText = "from ${contract.containingFile.name}"
  }

  override fun handleInsert(context: InsertionContext?) {
    if (!ImportFileAction.isImportedAlready(context!!.file, contract.containingFile)) {
      ImportFileAction.addImport(contract.project, context.file, contract.containingFile)
    }
  }
}
