package me.serce.solidity.lang.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.InitialPatternCondition
import com.intellij.patterns.ObjectPattern
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.*
import com.intellij.psi.util.descendantsOfType
import com.intellij.util.ProcessingContext
import com.intellij.util.text.findTextRange
import me.serce.solidity.ide.hints.isInheritDoc
import me.serce.solidity.lang.core.SolidityTokenTypes
import me.serce.solidity.lang.psi.SolImportAliasedPair
import me.serce.solidity.lang.psi.SolUserDefinedTypeName

// https://docs.soliditylang.org/en/develop/natspec-format.html#tags
private val NATSPEC_TAGS = arrayListOf(
  "title" to "A title that should describe the contract/interface",
  "author" to "The name of the author",
  "notice" to "Explain to an end user what this does",
  "dev" to "Explain to a developer any extra details",
  "param" to "Documents a parameter just like in Doxygen",
  "return" to "Documents the return variables of a contract’s function",
  "inheritdoc" to "Copies all missing tags from the base function",
  "custom" to "Custom tag, semantics is application-defined"
)

class NatSpecCompletionProvider(val prefix: String) : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet
  ) {
    NATSPEC_TAGS
      .map { (tag, descr) -> LookupElementBuilder.create(prefix + tag).withTailText(" $descr") }
      .forEach { result.addElement(it.keywordPrioritised()) }
  }
}

class SolNatSpecCompletionContributor : CompletionContributor(), DumbAware {
  init {
    extend(
      CompletionType.BASIC,
      psiElement(SolidityTokenTypes.NAT_SPEC_TAG),
      NatSpecCompletionProvider(""))
    extend(
      CompletionType.BASIC,
      psiElement(SolidityTokenTypes.COMMENT),
      NatSpecCompletionProvider("@"))
  }
}


private val natSpecContractReference = ObjectPattern.Capture(object : InitialPatternCondition<PsiComment>(PsiComment::class.java) {
    override fun accepts(o: Any?, context: ProcessingContext?): Boolean {
        return (o as? PsiComment)?.prevSibling?.isInheritDoc() == true
    }
})

class SolDocReferenceContributor : PsiReferenceContributor() {
  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {

    registrar.registerReferenceProvider(natSpecContractReference,
      object : PsiReferenceProvider() {

        override fun getReferencesByElement(
          element: PsiElement,
          context: ProcessingContext
        ): Array<PsiReference> {
          val file = element.containingFile
          val refText = element.text.trimStart().split("\\s".toRegex(), 2)[0]
          file.descendantsOfType<SolUserDefinedTypeName>().find { it.name == refText && it.parent !is SolImportAliasedPair }?.let {
            val elements = object : PsiReferenceBase<PsiComment>(element as PsiComment, element.text.findTextRange(refText)) {
              override fun resolve() = it.reference?.resolve()
            }
            return arrayOf(elements)
          }
          return emptyArray()
        }
      }, PsiReferenceRegistrar.HIGHER_PRIORITY)
  }
}
