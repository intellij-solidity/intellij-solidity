package me.serce.solidity.ide.hints


import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import me.serce.solidity.lang.psi.SolDeclarationItem
import me.serce.solidity.lang.psi.SolFunctionCallArguments
import me.serce.solidity.lang.psi.SolFunctionCallElement
import me.serce.solidity.lang.types.inferDeclType

class SolParameterInlayHintProvider : InlayParameterHintsProvider {

  override fun getParameterHints(element: PsiElement): List<InlayInfo> {
    return HintType.values().filter { it.isApplicable(element) }.flatMap { it.provideHints(element) }
  }

  override fun getDefaultBlackList(): Set<String> {
    return emptySet()
  }
}

enum class HintType(
     private val description: String,
    defaultEnabled: Boolean
) {

  TUPLE_HINT(
    "myTupleHintDescription",
      true
  ) {
      override fun provideHints(e: PsiElement): List<InlayInfo> {
          val item = e as? SolDeclarationItem ?: return emptyList()
          return listOf(InlayInfo(inferDeclType(item).toString(), e.textOffset))
      }

      override fun isApplicable(e: PsiElement): Boolean = e is SolDeclarationItem
  },

    PARAMETER_HINT(
      "myHintDescription",
        true
    ) {
        override fun provideHints(e: PsiElement): List<InlayInfo> {
            val callElement = e.parentOfType<SolFunctionCallElement>(true) ?: return emptyList()
            return provideArgumentNameHints(callElement)
        }

        override fun isApplicable(e: PsiElement): Boolean = e is SolFunctionCallArguments
    };

    companion object {
        private val values = values()

        fun resolve(e: PsiElement): List<HintType> =
            values.filter { it.isApplicable(e) }
    }

    abstract fun isApplicable(e: PsiElement): Boolean
    open fun provideHints(e: PsiElement): List<InlayInfo> = emptyList()
}

fun provideArgumentNameHints(element: SolFunctionCallElement): List<InlayInfo> {
  val params = element.resolveDefinitions().takeIf { it?.size == 1 }?.get(0)?.parseParameters() ?: return emptyList()
  // if (expressionList.none { it.isUnclearExpression() }) return emptyList()
  val args = element.functionCallArguments.expressionList

  return params.zip(args).map { InlayInfo(it.first.let { it.first ?: it.second.toString() }, it.second.startOffset) }


}


