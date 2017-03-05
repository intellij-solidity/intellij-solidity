package me.serce.solidity.lang.core

import com.intellij.psi.tree.IElementType
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import me.serce.solidity.lang.SolidityFileType
import me.serce.solidity.lang.SolidityLanguage

class SolidityFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, SolidityLanguage) {
    override fun getFileType(): FileType = SolidityFileType
    override fun toString(): String = "Solidity File"
}

class SolElementType(val name: String) : IElementType(name, SolidityLanguage)
