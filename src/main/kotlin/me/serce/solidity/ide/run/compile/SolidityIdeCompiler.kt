package me.serce.solidity.ide.run.compile

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.compiler.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.util.io.lastModified
import me.serce.solidity.ide.settings.SoliditySettings
import me.serce.solidity.lang.SolidityFileType
import me.serce.solidity.lang.psi.impl.SolImportPathElement
import me.serce.solidity.lang.stubs.SolImportIndex
import java.io.DataInput
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

private val log = logger<SolidityIdeCompiler>()

fun DataInput?.toValidityState(): ValidityState {
  if (this != null) try {
    return TimestampValidityState.load(this)
  } catch (e: Exception) {
    log.warn("Failed to read a timestamp", e)
  }
  return EmptyValidityState()
}

object SolidityIdeCompiler : Validator {
// extends Validator to be run after the regular (javac) compiler,
// otherwise the last one will erase all the compiled contract files

  override fun createValidityState(`in`: DataInput?): ValidityState {
    return `in`.toValidityState()
  }

  override fun getProcessingItems(context: CompileContext): Array<FileProcessingCompiler.ProcessingItem> {
    if (!SoliditySettings.instance.useSolcJ || !Solc.isEnabled()) {
      return FileProcessingCompiler.ProcessingItem.EMPTY_ARRAY
    }
    return collectProcessingItems(context)
  }

  fun collectProcessingItems(context: CompileContext): Array<FileProcessingCompiler.ProcessingItem> {
    val scope = context.compileScope
    val files = scope.getFiles(SolidityFileType, true).asSequence().filterNotNull()
    return runReadAction {
      val importKeys = StubIndex.getInstance().getAllKeys(SolImportIndex.KEY, context.project)
      val imports = importKeys.asSequence()
        .map { StubIndex.getElements(SolImportIndex.KEY, it, context.project, GlobalSearchScope.projectScope(context.project), SolImportPathElement::class.java) }
        .flatten()
        .groupBy { it.containingFile.virtualFile }
      files.map { file ->
        val base = Paths.get(file.parent.path)
        val importStamps = imports[file]?.map { stampForImport(base, it) }?.sum() ?: 0
        val myValidityState = TimestampValidityState(file.timeStamp + importStamps)
        SolProcessingItem(myValidityState, file.canonicalFile!!)
      }.toList().toTypedArray()
    }
  }

  private fun stampForImport(base: Path, pathElement: SolImportPathElement): Long =
    try {
      base.resolve(pathElement.text.replace("\"", "")).normalize().lastModified().toMillis()
    } catch (e: Exception) {
      log.debug("failed to calculate timestamp", e)
      0
    }

  override fun process(context: CompileContext, items: Array<FileProcessingCompiler.ProcessingItem>): Array<FileProcessingCompiler.ProcessingItem> {
    val modules = context.compileScope.affectedModules
    val fileByModule = items
      .map { Pair(it, modules.find { m -> m.moduleScope.contains(it.file) } ?: return@map null) }
      .filterNotNull()
      .groupBy { it.second }

    val compiled = fileByModule.map { entry ->
      Pair(entry.value, Solc.compile(entry.value.map { File(it.first.file.path) }, SolidityCompiler.getOutputDir(entry.key), context.project.baseDir))
    }.onEach { SolcMessageProcessor.process(it.second, context) }
      .groupBy { it.second.success }

    val success = compiled[true] ?: return emptyArray()
    return success.flatMap { it.first }.map { it.first }.toTypedArray()
  }


  override fun validateConfiguration(scope: CompileScope?): Boolean {
    return Solc.isEnabled()
  }

  override fun getDescription(): String {
    return "Solidity Language Compiler (Solc)"
  }
}

