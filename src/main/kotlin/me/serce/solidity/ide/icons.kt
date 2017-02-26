package me.serce.solidity.ide

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object SolidityIcons {
  val FILE_ICON: Icon = IconLoader.getIcon("/icons/sol-file.png")

  val ENUM: Icon = AllIcons.Nodes.Enum
  val CONTRACT: Icon = AllIcons.Nodes.Class
}
