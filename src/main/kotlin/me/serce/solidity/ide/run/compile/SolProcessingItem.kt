package me.serce.solidity.ide.run.compile

import com.intellij.openapi.compiler.FileProcessingCompiler
import com.intellij.openapi.compiler.ValidityState
import com.intellij.openapi.vfs.VirtualFile

class SolProcessingItem(
  private val myValidityState: ValidityState,
  private val myFile: VirtualFile
) : FileProcessingCompiler.ProcessingItem {

  override fun getValidityState(): ValidityState? {
    return myValidityState
  }

  override fun getFile(): VirtualFile {
    return myFile
  }
}
