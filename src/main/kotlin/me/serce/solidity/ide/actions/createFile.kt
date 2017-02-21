package me.serce.solidity.ide.actions

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import me.serce.solidity.ide.SolidityIcons

private val CAPTION = "New Solidity File"

class SolidityCreateFileAction : CreateFileFromTemplateAction(CAPTION, "", SolidityIcons.FILE_ICON), DumbAware {

  override fun getActionName(directory: PsiDirectory?, newName: String?, templateName: String?) = CAPTION

  override fun buildDialog(project: Project?, directory: PsiDirectory?,
                           builder: CreateFileFromTemplateDialog.Builder) {
    builder.setTitle(CAPTION).addKind("Empty File", SolidityIcons.FILE_ICON, "Solidity File")
  }
}
