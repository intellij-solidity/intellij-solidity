package me.serce.solidity.ide

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object SolidityIcons {
  val FILE_ICON: Icon = IconLoader.getIcon("/icons/sol-file.png")

  val ENUM: Icon = IconLoader.getIcon("/icons/sol-enum.png")
  val CONTRACT: Icon = IconLoader.getIcon("/icons/sol-contract.png")
  val STRUCT: Icon = IconLoader.getIcon("/icons/sol-contract.png")
  val FUNCTION: Icon = IconLoader.getIcon("/icons/sol-method.png")
  val STATE_VAR: Icon = IconLoader.getIcon("/icons/sol-state-var.png")
}
