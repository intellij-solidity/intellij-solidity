package me.serce.solidity.lang

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile
import me.serce.solidity.ide.SolidityIcons
import java.nio.charset.StandardCharsets.UTF_8

object SolidityLanguage : Language("Solidity", "text/solidity") {
  override fun isCaseSensitive() = true
}

object SolidityFileType : LanguageFileType(SolidityLanguage) {
  object DEFAULTS {
    const val DESCRIPTION = "Solidity file"
  }

  override fun getName() = DEFAULTS.DESCRIPTION
  override fun getDescription() = DEFAULTS.DESCRIPTION
  override fun getDefaultExtension() = "sol"
  override fun getIcon() = SolidityIcons.FILE_ICON
  override fun getCharset(file: VirtualFile, content: ByteArray): String = UTF_8.name()
}

object YulLanguage : Language(SolidityLanguage, "Yul", "text/yul") {
  override fun isCaseSensitive() = true
}

object YulFileType : LanguageFileType(YulLanguage) {
  object DEFAULTS {
    const val DESCRIPTION = "Yul file"
  }

  override fun getName() = DEFAULTS.DESCRIPTION
  override fun getDescription() = DEFAULTS.DESCRIPTION
  override fun getDefaultExtension() = "yul"
  override fun getIcon() = SolidityIcons.FILE_ICON
  override fun getCharset(file: VirtualFile, content: ByteArray): String = UTF_8.name()
}
