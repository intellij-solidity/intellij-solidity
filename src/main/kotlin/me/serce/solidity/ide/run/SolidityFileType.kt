package me.serce.solidity.ide.run

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.VirtualFile
import me.serce.solidity.ide.SolidityIcons
import javax.swing.Icon

object SolidityFileType : FileType {
  override fun getDefaultExtension(): String {
    return ".sol"
  }

  override fun getIcon(): Icon? {
    return SolidityIcons.FILE_ICON
  }

  override fun getCharset(file: VirtualFile, content: ByteArray): String? {
    return CharsetToolkit.UTF8
  }

  override fun getName(): String {
    return "SOL"
  }

  override fun getDescription(): String {
    return "Solidity Ethereum contract"
  }

  override fun isBinary(): Boolean {
    return false;
  }

  override fun isReadOnly(): Boolean {
    return false;
  }
}
