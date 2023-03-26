package me.serce.solidity.ide

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object SolidityIcons {
  val FILE_ICON: Icon = IconLoader.getIcon("/icons/sol-file.png", SolidityIcons::class.java)

  val ENUM: Icon = IconLoader.getIcon("/icons/sol-enum.png", SolidityIcons::class.java)
  val EVENT: Icon = IconLoader.getIcon("/icons/sol-enum.png", SolidityIcons::class.java)
  val ERROR: Icon = IconLoader.getIcon("/icons/sol-enum.png", SolidityIcons::class.java)
  val CONTRACT: Icon = IconLoader.getIcon("/icons/sol-contract.png", SolidityIcons::class.java)
  val STRUCT: Icon = IconLoader.getIcon("/icons/sol-contract.png", SolidityIcons::class.java)
  val FUNCTION: Icon = IconLoader.getIcon("/icons/sol-method.png", SolidityIcons::class.java)
  val STATE_VAR: Icon = IconLoader.getIcon("/icons/sol-state-var.png", SolidityIcons::class.java)
}
