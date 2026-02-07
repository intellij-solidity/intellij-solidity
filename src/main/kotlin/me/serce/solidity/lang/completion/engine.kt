package me.serce.solidity.lang.completion

import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import me.serce.solidity.ide.SolidityIcons
import me.serce.solidity.ide.inspections.fixes.ImportFileAction
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.resolve.SolResolver
import me.serce.solidity.lang.stubs.SolErrorIndex
import me.serce.solidity.lang.stubs.SolEventIndex
import me.serce.solidity.lang.stubs.SolGotoClassIndex
import me.serce.solidity.lang.stubs.SolModifierIndex
import me.serce.solidity.lang.types.ContextType
import me.serce.solidity.lang.types.Usage
import me.serce.solidity.lang.types.getMembers
import me.serce.solidity.lang.types.type

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

  fun completeErrorName(element: PsiElement): Array<out LookupElementBuilder> {
    val project = element.project
    val allTypeNames = StubIndex.getInstance().getAllKeys(
      SolErrorIndex.KEY,
      project
    )
    return allTypeNames
      .map {
        LookupElementBuilder
          .create(it, it)
          .withIcon(SolidityIcons.ERROR)
      }
      .toTypedArray()
  }

  fun completeTypeName(
    element: PsiElement,
    prefix: String = "",
    invocationCount: Int = 1
  ): Array<out LookupElement> {
    if (!shouldCompleteGlobalTypes(prefix, invocationCount)) {
      return emptyArray()
    }
    // Use CamelHump matching ("FB" -> "FooBar") to match the behaviour of IJ idea
    val matcher = if (prefix.isBlank()) null else CamelHumpMatcher(prefix)
    val project = element.project
    return SolContractNamesCache.getInstance(project)
      .allNames()
      .asSequence()
      .filter { matcher == null || matcher.prefixMatches(it) }
      .map { ContractLookupElement(project, it) }
      .toList()
      .toTypedArray()
  }

  fun completeModifier(element: SolModifierInvocationElement): Array<out LookupElement> {
    val project = element.project
    val allModifiers = StubIndex.getInstance().getAllKeys(
      SolModifierIndex.KEY,
      project
    )
    return allModifiers
      .map { LookupElementBuilder.create(it, it).withIcon(SolidityIcons.MODIFIER) }
      .toTypedArray()
  }

  fun completeLiteral(
    element: PsiElement,
    prefix: String = "",
    invocationCount: Int = 1
  ): Sequence<LookupElement> {
    val lexicalDeclarations = SolResolver.lexicalDeclarations(element).mapNotNull {
      when (it) {
        is SolFunctionDefinition -> it.toFunctionLookup()
        is SolStructDefinition -> it.toStructLookup()
        else -> it.toVarLookup()
      }
    }.associateBy { it.lookupString }
    val keys = lexicalDeclarations.keys
    return lexicalDeclarations.values.asSequence() +
      completeTypeName(element, prefix, invocationCount).asSequence().filterNot { keys.contains(it.lookupString) }
  }

  private fun shouldCompleteGlobalTypes(prefix: String, invocationCount: Int): Boolean {
    val minGlobalTypePrefixLength = 2
    // Keep autopopup cheap by showing project-wide types only on explicit completion,
    // or after a small typed prefix.
    return invocationCount >= 1 || prefix.length >= minGlobalTypePrefixLength
  }

  fun completeMemberAccess(element: SolMemberAccessExpression): Array<out LookupElement> {
    val expr = element.expression
    val contextType = when {
      expr is SolPrimaryExpression && expr.varLiteral?.name == "super" -> ContextType.SUPER
      expr.type.isBuiltin -> ContextType.BUILTIN
      else -> ContextType.EXTERNAL
    }

    return element.getMembers()
      .mapNotNull {
        when (it.getPossibleUsage(contextType)) {
          Usage.CALLABLE -> {
            // could also be a builtin, me.serce.solidity.lang.types.BuiltinCallable
            (it as? SolCallable ?: it.resolveElement() as? SolCallableElement)?.toFunctionLookup()
          }
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


}

class ContractLookupElement(
  private val project: Project,
  private val contractName: String,
  private val sourceFileName: String? = null
) : LookupElement() {
  constructor(contract: SolContractDefinition) : this(
    contract.project,
    contract.name ?: "",
    contract.containingFile.name
  )

  override fun getLookupString(): String = contractName

  override fun renderElement(presentation: LookupElementPresentation) {
    presentation.icon = SolidityIcons.CONTRACT
    presentation.itemText = contractName
    sourceFileName?.let {
      presentation.typeText = "from $it"
    }
  }

  override fun handleInsert(context: InsertionContext) {
    // Stub index access is not safe/reliable during dumb mode:
    // https://plugins.jetbrains.com/docs/intellij/indexing-and-psi-stubs.html#dumb-mode
    if (DumbService.isDumb(project)) {
      return
    }
    val contract = runReadAction {
      val candidates = StubIndex.getElements(
        SolGotoClassIndex.KEY,
        contractName,
        project,
        GlobalSearchScope.projectScope(project),
        SolNamedElement::class.java
      ).filterIsInstance<SolContractDefinition>()
      val inCurrentFile = candidates.firstOrNull { it.containingFile == context.file }
      if (inCurrentFile != null) {
        return@runReadAction inCurrentFile
      }
      if (candidates.size == 1) {
        return@runReadAction candidates.first()
      }
      null
    } ?: return
    if (!ImportFileAction.isImportedAlready(context.file, contract.containingFile)) {
      ImportFileAction.addContractImport(contract, context.file)
    }
  }
}
