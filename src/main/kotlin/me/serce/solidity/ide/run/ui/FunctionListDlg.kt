package me.serce.solidity.ide.run.ui

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

class FunctionListDialog(private val myContract: SolContractDefinition, filter: Condition<SolFunctionDefinition>, parent: JComponent) : DialogWrapper(parent, false) {
  private val myListModel = SortedListModel<SolFunctionDefinition>(METHOD_NAME_COMPARATOR)
  private val myList = JBList<SolFunctionDefinition>(myListModel)
  private val myWholePanel = JPanel(BorderLayout())

  val selected: SolFunctionDefinition?
    get() {
      return myList.selectedValue
    }

  init {
    createList(myContract.functionDefinitionList.toTypedArray(), filter)
    myWholePanel.add(ScrollPaneFactory.createScrollPane(myList))
    myList.cellRenderer = object : ColoredListCellRenderer<SolFunctionDefinition>() {
      override fun customizeCellRenderer(list: JList<out SolFunctionDefinition>, function: SolFunctionDefinition, index: Int, selected: Boolean, hasFocus: Boolean) {
        if (function.name == null) {
          return
        }
        append(function.name!!, StructureNodeRenderer.applyDeprecation(function, SimpleTextAttributes.REGULAR_ATTRIBUTES))
        val contract = function.contract
        if (myContract != contract)
          append(" (" + contract.name + ")", StructureNodeRenderer.applyDeprecation(contract, SimpleTextAttributes.GRAY_ATTRIBUTES))
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
    title = "Choose Function to execute"
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

private val METHOD_NAME_COMPARATOR = comparator@ { f1 : SolFunctionDefinition, f2 : SolFunctionDefinition ->
  if (f1.name == null ) {
    return@comparator 1
  }
  if (f2.name == null) {
    return@comparator -1
  }
  f1.name!!.compareTo(f2.name!!, ignoreCase = true) }
