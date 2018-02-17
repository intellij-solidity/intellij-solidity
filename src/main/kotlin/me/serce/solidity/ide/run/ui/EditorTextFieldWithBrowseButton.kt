package me.serce.solidity.ide.run.ui

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.ui.EditorTextField
import com.intellij.ui.TextAccessor
import me.serce.solidity.ide.run.SolidityFileType



class EditorTextFieldWithBrowseButton(project: Project) : ComponentWithBrowseButton<EditorTextField>(createEditorTextField(project), null), TextAccessor {

  override fun getText(): String {
    return childComponent.text
  }

  override fun setText(text: String?) {
    childComponent.text = text ?: ""
  }

  companion object {
    private fun createEditorTextField(project: Project): EditorTextField {
      return if (project.isDefault) EditorTextField() else EditorTextField(createDocument(""), project, SolidityFileType)
    }

    private fun createDocument(text: String): Document? {
      return EditorFactory.getInstance().createDocument(text)
    }
  }
}
