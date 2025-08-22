package me.serce.solidity.ide

import com.intellij.icons.AllIcons
import com.intellij.ide.structureView.*
import com.intellij.ide.structureView.customRegions.CustomRegionTreeElement
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase
import com.intellij.ide.util.treeView.smartTree.*
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
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

class SolStructureViewExtension: StructureViewExtension {
  override fun getType(): Class<out PsiElement> {
    return SolElement::class.java
  }

  override fun getChildren(p0: PsiElement?): Array<StructureViewTreeElement> {
    return emptyArray()
  }

  override fun getCurrentEditorElement(p0: Editor?, p1: PsiElement?): Any? {
    return null
  }

  override fun filterChildren(baseChildren: MutableCollection<StructureViewTreeElement>, extensionChildren: List<StructureViewTreeElement>) {
    val project = baseChildren.firstNotNullOfOrNull { it.value?.let { it as? PsiElement ?: (((it as? StructureViewTreeElement)?.children?.firstOrNull() as? StructureViewTreeElement)?.value as? PsiElement) } }?.project ?: return
    if (!StructureViewFactoryEx.getInstanceEx(project).isActionActive(CUSTOM_REGION_NAME)) {
      doFilter(baseChildren)
    }
  }

  private fun doFilter(baseChildren: MutableCollection<StructureViewTreeElement>) {
    val rest = baseChildren.toMutableList()
    val iter = rest.listIterator()
    for (i in iter) {
      if (i is CustomRegionTreeElement) {
        iter.remove()
        i.children.filterIsInstance<StructureViewTreeElement>().forEach { iter.add(it) }
      }
      }
    //    super.filterChildren(baseChildren, extensionChildren)
    baseChildren.clear()
    baseChildren.addAll(rest)
  }
}

class SolStructureViewModel(editor: Editor?, file: SolidityFile) : TextEditorBasedStructureViewModel(editor, file),
  StructureViewModel.ElementInfoProvider {

  override fun getRoot() = SolTreeElement(psiFile)

  override fun getPsiFile(): SolidityFile = super.getPsiFile() as SolidityFile

  override fun isAlwaysShowsPlus(element: StructureViewTreeElement) = element.value is SolidityFile

  override fun isAlwaysLeaf(element: StructureViewTreeElement) = false

  override fun getSorters(): Array<Sorter> {
    return arrayOf(Sorter.ALPHA_SORTER, VisibilitySorter, MutabilitySorter);
  }

  override fun getFilters(): Array<Filter> {
    return arrayOf(RegionFilter())
  }
}

private const val CUSTOM_REGION_NAME = "SHOW_CUSTOM_REGIONS"

class RegionFilter : Filter {
  override fun getPresentation(): ActionPresentation {
    return ActionPresentationData("Show Custom Regions", null as String?, SolidityIcons.SHOW_REGION_TOGGLE);
  }

  override fun getName(): String {
    return CUSTOM_REGION_NAME
  }

  override fun isVisible(p0: TreeElement?): Boolean {
    return true
  }

  override fun isReverted(): Boolean {
    return false
  }

}

object VisibilitySorter : Sorter {
  override fun getPresentation(): ActionPresentation {
    return ActionPresentationData("By Visibility", null as String?, AllIcons.ObjectBrowser.VisibilitySort);
  }

  override fun getName(): String = "VISIBILITY_COMPARATOR"

  override fun getComparator(): Comparator<*> {
    return Comparator { o1: Any?, o2: Any?  -> compare(o1, o2) { it.visibility } };
  }

  override fun isVisible(): Boolean = true

}

object MutabilitySorter : Sorter {
  override fun getPresentation(): ActionPresentation {
    return ActionPresentationData("By Mutability", null as String?, AllIcons.ObjectBrowser.SortByType);
  }
  override fun getName(): String = "MUTABILITY_COMPARATOR"

  override fun getComparator(): Comparator<*> {
    return Comparator { o1: Any?, o2: Any?  -> compare(o1, o2) { it.mutability } };
  }

  override fun isVisible(): Boolean = true

}

private fun compare(o1: Any?, o2: Any?, getter: (SolFunctionDefinition) -> Enum<*>?) : Int {
  val s1 = o1 as? SolLeafTreeElement ?: return 0
  val s2 = o2 as? SolLeafTreeElement ?: return 0
  val f1 = s1.value as? SolFunctionDefinition ?: return 0
  val f2 = s2.value as? SolFunctionDefinition ?: return 0
  return (getter(f1)?.ordinal ?: 0) - (getter(f2)?.ordinal ?: 0)
}

class SolTreeElement(item: PsiFile) : PsiTreeElementBase<PsiFile>(item) {
  override fun getPresentableText() = element?.name

  override fun getChildrenBase(): Collection<StructureViewTreeElement> {
    val el = element ?: return emptyList()
    return (el.childrenOfType<SolContractDefinition>().map(::SolContractTreeElement) +
      el.childrenOfType<SolStructDefinition>().map(::SolStructTreeElement) +
      el.childrenOfType<SolEnumDefinition>().map(::SolLeafTreeElement))
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

