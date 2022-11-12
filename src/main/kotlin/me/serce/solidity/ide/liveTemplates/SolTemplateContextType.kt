package me.serce.solidity.ide.liveTemplates

import com.intellij.codeInsight.template.FileTypeBasedContextType
import me.serce.solidity.lang.SolidityFileType
import me.serce.solidity.lang.SolidityLanguage
import java.util.*

class SolTemplateContextType : FileTypeBasedContextType(
  SolidityLanguage.id.uppercase(Locale.getDefault()),
  SolidityLanguage.id,
  SolidityFileType
)
