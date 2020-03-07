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
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.resolve.SolResolver
import me.serce.solidity.lang.stubs.SolEventIndex
import me.serce.solidity.lang.stubs.SolGotoClassIndex
import me.serce.solidity.lang.stubs.SolModifierIndex
import me.serce.solidity.lang.types.*
import javax.swing.Icon

const val TYPED_COMPLETION_PRIORITY = 15.0

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

  fun completeModifier(element: SolModifierInvocationElement): Array<out LookupElement> {
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
    val expr = element.expression
    val contextType = when {
      expr is SolPrimaryExpression && expr.varLiteral?.name == "super" -> ContextType.SUPER
      else -> ContextType.EXTERNAL
    }
    return element.expression.getMembers()
      .mapNotNull {
        when (it.getPossibleUsage(contextType)) {
          Usage.CALLABLE -> (it as SolCallableElement).toFunctionLookup()
          Usage.VARIABLE -> it.getName()?.let { name ->
            PrioritizedLookupElement.withPriority(
              LookupElementBuilder.create(name).withIcon(SolidityIcons.STATE_VAR),
              TYPED_COMPLETION_PRIORITY
            )
          }
          else -> null
        }
      }
      .distinctBy { it.lookupString }
      .toTypedArray()
  }

  private fun Collection<SolNamedElement>.createVarLookups(): Array<LookupElement> = createVarLookups(SolidityIcons.STATE_VAR)

  private fun Collection<SolNamedElement>.createVarLookups(icon: Icon): Array<LookupElement> = map {
    PrioritizedLookupElement.withPriority(
      LookupElementBuilder.create(it.name ?: "").withIcon(icon),
      TYPED_COMPLETION_PRIORITY
    )
  }.toTypedArray()
}

class ContractLookupElement(val contract: SolContractDefinition) : LookupElement() {
  override fun getLookupString(): String = contract.name!!

  override fun renderElement(presentation: LookupElementPresentation) {
    presentation.icon = SolidityIcons.CONTRACT
    presentation.itemText = contract.name
    presentation.typeText = "from ${contract.containingFile.name}"
  }

  override fun handleInsert(context: InsertionContext) {
    if (!ImportFileAction.isImportedAlready(context.file, contract.containingFile)) {
      ImportFileAction.addImport(contract.project, context.file, contract.containingFile)
    }
  }
}
