package me.serce.solidity.ide

import com.intellij.ide.structureView.*
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.psi.util.childrenOfType
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.lang.psi.*
import javax.swing.Icon

class SolPsiStructureViewFactory : PsiStructureViewFactory {
  override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder {
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

  override fun isAlwaysLeaf(element: StructureViewTreeElement) = false

  override fun getSorters(): Array<Sorter> {
    return arrayOf(Sorter.ALPHA_SORTER);
  }
}



class SolTreeElement(item: PsiFile) : PsiTreeElementBase<PsiFile>(item) {
  override fun getPresentableText() = element?.virtualFile?.presentableName // TODO: name

  override fun getChildrenBase(): Collection<StructureViewTreeElement> {
    val el = element ?: return emptyList()
    return (el.childrenOfType<SolContractDefinition>().map(::SolContractTreeElement) +
      el.childrenOfType<SolStructDefinition>().map(::SolStructTreeElement))
      .sortedBy { it.element?.textOffset }
  }
}

abstract class SolNamedPsiTreeElementBase<T: SolNamedElement>(item: T) : PsiTreeElementBase<T>(item) {
  override fun getPresentableText() = element?.name
}

class SolContractTreeElement(item: SolContractDefinition) : SolNamedPsiTreeElementBase<SolContractDefinition>(item) {

  override fun getChildrenBase(): Collection<StructureViewTreeElement> = element?.let {
    (it.constructorDefinitionList + it.functionDefinitionList + it.stateVariableDeclarationList +
      it.userDefinedValueTypeDefinitionList + it.modifierDefinitionList +
     it.errorDefinitionList + it.enumDefinitionList + it.eventDefinitionList )
      .map(::SolLeafTreeElement) + it.structDefinitionList.map(::SolStructTreeElement)
  } ?: emptyList()
}

class SolStructTreeElement(item: SolStructDefinition) : SolNamedPsiTreeElementBase<SolStructDefinition>(item) {
  override fun getChildrenBase(): Collection<StructureViewTreeElement> {
    return element?.variableDeclarationList?.map(::SolLeafTreeElement) ?: emptyList()
  }

}

class SolLeafTreeElement(item: SolNamedElement, private val icon: Icon? = null) : PsiTreeElementBase<SolNamedElement>(item) {
  override fun getPresentableText() = element?.let {
    when (it) {
      is SolConstructorDefinition -> "constructor${params(it.parameterList)}"
      is SolFunctionDefinition -> "${it.name ?: it.firstChild.text }${params(it.parameterListList.getOrNull(0))} ${it.returns?.let { "returns ${params(it)}"} ?: ""}"
      is SolVariableDeclaration -> "${it.name} : ${it.typeName?.text}"
      is SolStateVariableDeclaration -> "${it.name} : ${it.typeName.text}"
      else -> it.name
    }
  }

  private fun params(it: SolParameterList?): String {
    if (it == null) return "()"
    return "(" + it.parameterDefList.joinToString { it.typeName.text  + (it.name?.let {" $it"} ?: "") } + ")"
  }

  override fun getIcon(open: Boolean): Icon? {
    return icon ?: super.getIcon(open)
  }

  override fun getChildrenBase(): Collection<StructureViewTreeElement> = emptyList()
}

