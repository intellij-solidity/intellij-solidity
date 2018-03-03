package me.serce.solidity.ide

import com.intellij.ide.structureView.*
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.lang.psi.*

class SolPsiStructureViewFactory : PsiStructureViewFactory {
  override fun getStructureViewBuilder(psiFile: PsiFile?): StructureViewBuilder {
    val solFile = psiFile as SolidityFile
    return object : TreeBasedStructureViewBuilder() {
      override fun createStructureViewModel(editor: Editor?): StructureViewModel {
        return SolStructureViewModel(editor, solFile)
      }
    }
  }
}

class SolStructureViewModel(editor: Editor?, file: SolidityFile) : TextEditorBasedStructureViewModel(editor, file),
  StructureViewModel.ElementInfoProvider {

  override fun getRoot() = SolTreeElement(psiFile)

  override fun getPsiFile(): SolidityFile = super.getPsiFile() as SolidityFile

  override fun isAlwaysShowsPlus(element: StructureViewTreeElement) = element.value is SolidityFile

  override fun isAlwaysLeaf(element: StructureViewTreeElement) = when (element.value) {
    is SolFunctionDefinition,
    is SolStateVariableDeclaration,
    is SolContractDefinition -> true
    else -> false
  }
}

class SolTreeElement(item: SolElement) : PsiTreeElementBase<SolElement>(item) {
  override fun getPresentableText() = element?.toString() // TODO: name

  override fun getChildrenBase(): Collection<StructureViewTreeElement> {
    val el = element ?: return emptyList()
    return listOf(
      el.children.filterIsInstance(SolContractDefinition::class.java).map(::SolContractTreeElement)
    ).flatten().sortedBy { it.element?.textOffset }
  }
}

class SolContractTreeElement(item: SolContractDefinition) : PsiTreeElementBase<SolContractDefinition>(item) {
  override fun getPresentableText() = element?.name
  override fun getChildrenBase(): Collection<StructureViewTreeElement> = element?.let {
    listOf(
      it.functionDefinitionList.map(::SolLeafTreeElement),
      it.stateVariableDeclarationList.map(::SolLeafTreeElement)
    ).flatten()
  } ?: emptyList()
}

class SolLeafTreeElement(item: SolNamedElement) : PsiTreeElementBase<SolNamedElement>(item) {
  override fun getPresentableText() = element?.name
  override fun getChildrenBase(): Collection<StructureViewTreeElement> = emptyList()
}

