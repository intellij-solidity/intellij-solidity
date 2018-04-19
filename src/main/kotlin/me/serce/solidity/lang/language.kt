package me.serce.solidity.lang

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile
import me.serce.solidity.ide.SolidityIcons
import java.nio.charset.StandardCharsets.UTF_8

object SolidityLanguage : Language("Solidity", "text/solidity") {
  override fun isCaseSensitive() = true
}

class SolidityFileTypeFactory : FileTypeFactory() {
  override fun createFileTypes(consumer: FileTypeConsumer) {
    consumer.consume(
      SolidityFileType,
      ExtensionFileNameMatcher(SolidityFileType.DEFAULTS.EXTENSION))
  }
}

object SolidityFileType : LanguageFileType(SolidityLanguage) {
  object DEFAULTS {
    const val EXTENSION = "sol"
    const val DESCRIPTION = "Solidity file"
  }

  override fun getName() = DEFAULTS.DESCRIPTION
  override fun getDescription() = DEFAULTS.DESCRIPTION
  override fun getDefaultExtension() = DEFAULTS.EXTENSION
  override fun getIcon() = SolidityIcons.FILE_ICON
  override fun getCharset(file: VirtualFile, content: ByteArray) = UTF_8.name()
}
