package me.serce.solidity.lang

import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.*
import com.intellij.openapi.vfs.VirtualFile
import java.nio.charset.StandardCharsets.UTF_8

object SolidityLanguage : Language("Solidity", "text/solidity") {
    override fun isCaseSensitive() = true
}

class SoliditylFileTypeFactory : FileTypeFactory() {
    override fun createFileTypes(consumer: FileTypeConsumer) {
        consumer.consume(
                SolidityFileType,
                ExtensionFileNameMatcher(SolidityFileType.DEFAULTS.EXTENSION))
    }
}

object SolidityFileType : LanguageFileType(SolidityLanguage) {
    object DEFAULTS {
        val EXTENSION = "sol"
        val DESCRIPTION = "Solidity file"
    }

    override fun getName() = DEFAULTS.DESCRIPTION
    override fun getDescription() = DEFAULTS.DESCRIPTION
    override fun getDefaultExtension() = DEFAULTS.EXTENSION
    override fun getIcon() = AllIcons.FileTypes.Text
    override fun getCharset(file: VirtualFile, content: ByteArray) = UTF_8.name()
}