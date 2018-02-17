package me.serce.solidity.ide.run.ui

import com.intellij.execution.ExecutionBundle
import com.intellij.ide.structureView.impl.StructureNodeRenderer
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Condition
import com.intellij.ui.*
import com.intellij.ui.components.JBList
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolFunctionDefinition
import java.awt.BorderLayout
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel

class FunctionListDialog(private val myClass: SolContractDefinition, filter: Condition<SolFunctionDefinition>, parent: JComponent) : DialogWrapper(parent, false) {
  private val myListModel = SortedListModel<SolFunctionDefinition>(METHOD_NAME_COMPARATOR)
  private val myList = JBList<Any>(myListModel)
  private val myWholePanel = JPanel(BorderLayout())

  val selected: SolFunctionDefinition?
    get() {
      val selectedValue = myList.selectedValue ?: return null
      return selectedValue as SolFunctionDefinition
    }

  init {
    createList(myClass.functionDefinitionList.toTypedArray(), filter)
    myWholePanel.add(ScrollPaneFactory.createScrollPane(myList))
    myList.cellRenderer = object : ColoredListCellRenderer<Any>() {
      override fun customizeCellRenderer(list: JList<*>, value: Any, index: Int, selected: Boolean, hasFocus: Boolean) {
        val psiMethod = value as SolFunctionDefinition
        append(psiMethod.name!!/*PsiFormatUtil.formatMethod(psiMethod, PsiSubstitutor.EMPTY, PsiFormatUtil.SHOW_NAME, 0)*/,
          StructureNodeRenderer.applyDeprecation(psiMethod, SimpleTextAttributes.REGULAR_ATTRIBUTES))
        val containingClass = psiMethod.contract
        if (myClass != containingClass)
          append(" (" + containingClass.name + ")",
            StructureNodeRenderer.applyDeprecation(containingClass, SimpleTextAttributes.GRAY_ATTRIBUTES))
      }
    }
    myList.selectionMode = ListSelectionModel.SINGLE_SELECTION
    object : DoubleClickListener() {
      override fun onDoubleClick(e: MouseEvent): Boolean {
        this@FunctionListDialog.close(DialogWrapper.OK_EXIT_CODE)
        return true
      }
    }.installOn(myList)

    ScrollingUtil.ensureSelectionExists(myList)
    TreeUIHelper.getInstance().installListSpeedSearch(myList)
    title = ExecutionBundle.message("choose.test.method.dialog.title")
    init()
  }

  private fun createList(allMethods: Array<SolFunctionDefinition>, filter: Condition<SolFunctionDefinition>) {
    val methods = allMethods.indices
      .map { allMethods[it] }
      .filter { filter.value(it) }
      .toList()
    myListModel.addAll(methods)
  }

  override fun createCenterPanel(): JComponent? {
    return myWholePanel
  }

  override fun getPreferredFocusedComponent(): JComponent? {
    return myList
  }
}

private val METHOD_NAME_COMPARATOR = { psiMethod : SolFunctionDefinition, psiMethod1 : SolFunctionDefinition -> psiMethod.name!!.compareTo(psiMethod1.name!!, ignoreCase = true) }
