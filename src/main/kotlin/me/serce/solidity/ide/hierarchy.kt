package me.serce.solidity.ide

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.ide.hierarchy.*
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.util.CompositeAppearance
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.util.ArrayUtilRt
import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.getLogger
import me.serce.solidity.ide.navigation.findAllImplementations
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.parentOfType
import me.serce.solidity.lang.types.SolContract
import java.awt.Font
import javax.swing.JPanel
import javax.swing.JTree

class SolHierarchyTypeProvider : HierarchyProvider {
  override fun getTarget(dataContext: DataContext): PsiElement? {
    val project: Project = CommonDataKeys.PROJECT.getData(dataContext) ?: return null

    val editor = CommonDataKeys.EDITOR.getData(dataContext)
    if (editor != null) {
      val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return null

      val targetElement = TargetElementUtil.findTargetElement(editor, TargetElementUtil.ELEMENT_NAME_ACCEPTED or
        TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED or
        TargetElementUtil.LOOKUP_ITEM_ACCEPTED)
      if (targetElement is SolContractDefinition) {
        return targetElement
      }

      val offset = editor.caretModel.offset
      return file.findElementAt(offset)?.parentOfType<SolContractDefinition>(false)
    } else {
      val element = CommonDataKeys.PSI_ELEMENT.getData(dataContext)
      return if (element is SolContractDefinition) element else null
    }
  }

  override fun createHierarchyBrowser(p0: PsiElement): HierarchyBrowser {
    return SolTypeHierarchyBrowser(p0 as SolContractDefinition)
  }

  override fun browserActivated(hierarchyBrowser: HierarchyBrowser) {
    val browser = hierarchyBrowser as TypeHierarchyBrowserBase
    val typeName =
      if (browser.isInterface) TypeHierarchyBrowserBase.getSubtypesHierarchyType() else TypeHierarchyBrowserBase.getTypeHierarchyType()
    browser.changeView(typeName)
  }

}

class SolTypeHierarchyBrowser(element: SolContractDefinition) : TypeHierarchyBrowserBase(element.project, element) {
  override fun getElementFromDescriptor(descriptor: HierarchyNodeDescriptor): PsiElement? {
    return (descriptor as? TypeHierarchyNodeDescriptor)?.psiElement
  }

  override fun createTrees(trees: MutableMap<in String, in JTree>) {
    createTreeAndSetupCommonActions(trees, IdeActions.GROUP_TYPE_HIERARCHY_POPUP)
  }

  override fun createLegendPanel(): JPanel? = null

  override fun isApplicableElement(p0: PsiElement): Boolean = p0 is SolContractDefinition

  override fun createHierarchyTreeStructure(typeName: String, psiElement: PsiElement): HierarchyTreeStructure? {
    if (getSupertypesHierarchyType() == typeName) {
      return SupertypesHierarchyTreeStructure(psiElement as SolContractDefinition)
    } else if (getSubtypesHierarchyType() == typeName) {
      return SubtypesHierarchyTreeStructure(psiElement as SolContractDefinition, currentScopeType)
    } else if (getTypeHierarchyType() == typeName) {
      return TypeHierarchyTreeStructure(psiElement as SolContractDefinition, currentScopeType)
    } else {
      getLogger<SolTypeHierarchyBrowser>().error { "unexpected type: $typeName" }
      return null
    }

  }

  override fun getComparator(): Comparator<NodeDescriptor<*>>? {
    val state = HierarchyBrowserManager.getInstance(myProject).state
    return if (state != null && state.SORT_ALPHABETICALLY) {
      Comparator<NodeDescriptor<*>> { o1, o2 ->
        o1.toString().compareTo(o2.toString(), ignoreCase = true)
      }
    } else {
      Comparator { _, _ -> 0 }
    }
  }

  override fun isInterface(p0: PsiElement): Boolean = false /*(p0 as? SolContractDefinition)?.contractType == ContractType.INTERFACE*/

  override fun canBeDeleted(p0: PsiElement?): Boolean = true

  override fun getQualifiedName(p0: PsiElement?): String {
    return (p0 as? SolContractDefinition)?.name ?: ""
  }

}

class SupertypesHierarchyTreeStructure(contract: SolContractDefinition) : HierarchyTreeStructure(contract.project, TypeHierarchyNodeDescriptor(contract.project, null, contract, true)) {
  override fun buildChildren(descriptor: HierarchyNodeDescriptor): Array<Any> {
    val element = descriptor.psiElement as? SolContractDefinition
      ?: return ArrayUtilRt.EMPTY_OBJECT_ARRAY
    return SolContract(element).linearizeParents().map { TypeHierarchyNodeDescriptor(myProject, descriptor, it.ref, false) }.toTypedArray()
  }

}

open class SubtypesHierarchyTreeStructure : HierarchyTreeStructure {
  val currentScopeType: String

  constructor(contract: SolContractDefinition, currentScopeType: String) : super(contract.project, TypeHierarchyNodeDescriptor(contract.project, null, contract, true)) {
    this.currentScopeType = currentScopeType
  }

  constructor(descriptor: HierarchyNodeDescriptor, currentScopeType: String) : super(descriptor.project, descriptor) {
    this.currentScopeType = currentScopeType
  }

  override fun buildChildren(descriptor: HierarchyNodeDescriptor): Array<Any> {
    val element = descriptor.psiElement as? SolContractDefinition ?: return ArrayUtilRt.EMPTY_OBJECT_ARRAY
    return element.findAllImplementations().map { TypeHierarchyNodeDescriptor(myProject, descriptor, it, false) }.toTypedArray()
  }
}

class TypeHierarchyTreeStructure(element: SolContractDefinition, currentScopeType: String)
  : SubtypesHierarchyTreeStructure(createHDescriptor(element), currentScopeType) {
  init {
    setBaseElement(myBaseDescriptor) //to set myRoot
  }

  companion object {
    fun createHDescriptor(element: SolContractDefinition): HierarchyNodeDescriptor {
      var descriptor: HierarchyNodeDescriptor? = null
      val superClasses = SolContract(element).linearizeParents().asReversed()
      for (superClass in superClasses) {
        val newDescriptor = TypeHierarchyNodeDescriptor(element.project, descriptor, superClass.ref, false)
        descriptor?.cachedChildren = arrayOf(newDescriptor)
        descriptor = newDescriptor
      }
      val newDescriptor = TypeHierarchyNodeDescriptor(element.project, descriptor, element, true)
      descriptor?.cachedChildren = arrayOf(newDescriptor)
      return newDescriptor
    }
  }
}

class TypeHierarchyNodeDescriptor(project: Project, parentDescriptor: HierarchyNodeDescriptor?, classOrFunctionalExpression: PsiElement, isBase: Boolean) : HierarchyNodeDescriptor(project, parentDescriptor, classOrFunctionalExpression, isBase) {
  override fun update(): Boolean {
    var changes = super.update()

    if (psiElement == null) {
      return invalidElement()
    }

    if (changes && myIsBase) {
      icon = getBaseMarkerIcon(icon)
    }

    val oldText = myHighlightedText

    myHighlightedText = CompositeAppearance()

    var classNameAttributes: TextAttributes? = null
    if (myColor != null) {
      classNameAttributes = TextAttributes(myColor, null, null, null, Font.PLAIN)
    }
    (psiElement as? SolContractDefinition)?.let {
      myHighlightedText.ending.addText(it.name ?: "", classNameAttributes)
    }
    myName = myHighlightedText.text

    if (myHighlightedText != oldText) {
      changes = true
    }
    return changes
  }
}

