package me.serce.solidity.lang.core

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import me.serce.solidity.lang.SolidityFileType
import me.serce.solidity.lang.SolidityLanguage
import me.serce.solidity.lang.YulFileType
import me.serce.solidity.lang.YulLanguage
import me.serce.solidity.lang.psi.SolElement

class SolidityFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, SolidityLanguage), SolElement {
  override fun getFileType(): FileType = SolidityFileType
  override fun toString(): String = "Solidity File"
}

class SolElementType(val name: String) : IElementType(name, SolidityLanguage)

class YulFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, YulLanguage), SolElement {
  override fun getFileType() = YulFileType
  override fun toString(): String = "Yul File"
}

object YulFileElementType : IFileElementType(YulLanguage)
