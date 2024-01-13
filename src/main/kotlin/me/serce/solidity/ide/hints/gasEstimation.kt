package me.serce.solidity.ide.hints

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.hints.codeVision.CodeVisionProviderBase
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.findDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import me.serce.solidity.ide.SolidityIcons
import me.serce.solidity.lang.SolidityLanguage
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.types.SolUserType
import me.serce.solidity.lang.types.findContract
import me.serce.solidity.lang.types.getSolType
import java.awt.event.MouseEvent
import java.util.*

@Service(Service.Level.PROJECT)
class SolGasEstimationService(project: Project) {

  private val relativePath = "artifacts/build-info"

  @Volatile
  var hardHatRoot : VirtualFile? = null

  @Volatile
  var enabled: Boolean = true
    set(value) {
      field = value
      if (value) readCompiledData()
    }

  init {
    FilenameIndex.processFilesByNames(setOf("hardhat.config.js", "hardhat.config.ts"), false, GlobalSearchScope.allScope(project), null) {
      if (it.path.contains("node_modules")) return@processFilesByNames true
      hardHatRoot = it.parent
        readCompiledData()
      false
    }
    VirtualFileManager.getInstance().addAsyncFileListener(
          { events ->
            if (events.any { it.path.contains(relativePath) }) {
                VirtualFileManager.getInstance().asyncRefresh { if (enabled) readCompiledData() }
              }
            null
          }, Disposer.newDisposable())
  }

  private fun readCompiledData() {
    jsonNode = null
    val compiledOutputFile = hardHatRoot!!.findDirectory(relativePath)?.children?.firstOrNull { it.name.endsWith(".json") }
    compiledOutputFile?.toNioPath()?.toFile()?.let { jsonNode = ObjectMapper().readTree(it) }
  }

  @Volatile
  private var jsonNode: JsonNode? = null

  fun findEstimation(element: SolFunctionDefElement): String? {
    if (jsonNode == null) return null
    val file = hardHatRoot?.toNioPath()?.relativize(element.containingFile.virtualFile.toNioPath())?.toString()?.replace("\\", "/")
      ?: return null
    val contract = element.findContract()?.name ?: ""

    val estimates = jsonNode?.get("output")?.get("contracts")?.get(file)?.get(contract)?.get("evm")?.get("gasEstimates")
      ?: return null
    if (estimates is NullNode) {
      return "null"
    }
    val funName = if (element is SolConstructorDefinition) contract.replaceFirstChar { it.lowercase(Locale.getDefault()) } else element.name
      ?: return null
    val isInternal = element.visibility.let { it == Visibility.INTERNAL || it == Visibility.PRIVATE }

    val s = "$funName(${
      element.takeIf { it !is SolConstructorDefinition }?.parameters?.joinToString(",") {
        getSolType(it.typeName)
          .let { type ->
            if (type is SolUserType) "${type.abiName} .*${type}.*" else (type.toString() + (it.takeIf { isInternal }
              ?.let { PsiTreeUtil.findChildOfType(it, SolStorageLocation::class.java)?.text?.let { " $it" } } ?: ""))
          }
      }
    }"

    val funNameWParen by lazy { "$funName(" }

    return estimates.get(if (isInternal) "internal" else "external")?.let {
      (if (s.contains(".*")) it.fields().asSequence().find { it.key.matches(s.replace("(", "\\(").replace(")", "\\)").toRegex()) }?.value else it.get(s))
        ?: tryCollapseNodes(it, funNameWParen)
    }?.asText() ?: return "N/A";
  }

    private fun tryCollapseNodes(it: JsonNode, funNameWParen: String): JsonNode? {
        return it.fields().asSequence().filter { it.key.startsWith(funNameWParen) }.groupBy { it.value.textValue() }.takeIf { it.size == 1 }?.values?.single()?.first()?.value
    }
}


class SolGasEstimationInlayProvider : CodeVisionProviderBase() {
  override val id: String
    get() = "sol.gas.estimation"
  override val name: String
    get() = "Solidity Gas Estimation Hint"
  override val relativeOrderings: List<CodeVisionRelativeOrdering>
    get() = emptyList()

  override fun acceptsElement(element: PsiElement): Boolean = element is SolFunctionDefinition && element.children.any { it is SolBlock } && element.contract?.isAbstract != true

  override fun acceptsFile(file: PsiFile): Boolean = file.language == SolidityLanguage && file.project.serviceIfCreated<SolGasEstimationService>()?.let {it.enabled && it.hardHatRoot != null} ?: false

  override fun getHint(element: PsiElement, file: PsiFile): String? {
    val estimation = file.project.service<SolGasEstimationService>().findEstimation(element as? SolFunctionDefElement ?: return null).takeIf { it != "infinite" } ?: return null
    return "\u26FD $estimation"
  }

  override fun handleClick(editor: Editor, element: PsiElement, event: MouseEvent?) {
    // do nothing
  }
}

class ShowGasEstimationAction : ToggleAction({"Show Gas Estimations"}, SolidityIcons.FILE_ICON) {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun isSelected(e: AnActionEvent): Boolean {
    return e.project?.serviceIfCreated<SolGasEstimationService>()?.enabled ?: false
  }

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    val project = e.project ?: return
    if (state) {
      project.service<SolGasEstimationService>().enabled = true
    } else {
      project.serviceIfCreated<SolGasEstimationService>()?.enabled = false
    }
    PsiManager.getInstance(project).dropPsiCaches()
    DaemonCodeAnalyzer.getInstance(project).restart()
  }
}
