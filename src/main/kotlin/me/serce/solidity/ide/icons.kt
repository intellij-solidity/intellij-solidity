package me.serce.solidity.ide

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object SolidityIcons {
  val FILE_ICON: Icon = IconLoader.getIcon("/icons/sol-file.png", SolidityIcons::class.java)

  val ENUM: Icon = IconLoader.getIcon("/icons/EnumDeclaration.png", SolidityIcons::class.java)
  val EVENT: Icon = IconLoader.getIcon("/icons/Event.png", SolidityIcons::class.java)
  val ERROR: Icon = IconLoader.getIcon("/icons/Error.png", SolidityIcons::class.java)
  val CONTRACT: Icon = IconLoader.getIcon("/icons/Contract.png", SolidityIcons::class.java)
  val INTERFACE: Icon = IconLoader.getIcon("/icons/Interface.png", SolidityIcons::class.java)
  val LIBRARY: Icon = IconLoader.getIcon("/icons/Library.png", SolidityIcons::class.java)
  val STRUCT: Icon = IconLoader.getIcon("/icons/StructDeclaration.png", SolidityIcons::class.java)

  val FUNCTION: Icon = IconLoader.getIcon("/icons/PublicFunction.png", SolidityIcons::class.java) // todo new icon?

  val FUNCTION_PUB: Icon = IconLoader.getIcon("/icons/PublicFunction.png", SolidityIcons::class.java)
  val FUNCTION_EXT: Icon = IconLoader.getIcon("/icons/ExternalFunction.png", SolidityIcons::class.java)
  val FUNCTION_INT: Icon = IconLoader.getIcon("/icons/InternalFunction.png", SolidityIcons::class.java)
  val FUNCTION_PRV: Icon = IconLoader.getIcon("/icons/PrivateFunction.png", SolidityIcons::class.java)

  val STATE_VAR: Icon = IconLoader.getIcon("/icons/Variable.png", SolidityIcons::class.java)
  val IMMUTABLE_VARIABLE: Icon = IconLoader.getIcon("/icons/ImmutableVariable.png", SolidityIcons::class.java)
  val CONSTANT_VARIABLE: Icon = IconLoader.getIcon("/icons/ConstantVariable.png", SolidityIcons::class.java)

  val VIEW: Icon = IconLoader.getIcon("/icons/ViewFunction.png", SolidityIcons::class.java)
  val PURE: Icon = IconLoader.getIcon("/icons/PureFunction.png", SolidityIcons::class.java)
  val WRITE: Icon = IconLoader.getIcon("/icons/WriteFunction.png", SolidityIcons::class.java)
  val PAYABLE: Icon = IconLoader.getIcon("/icons/PayableFunction.png", SolidityIcons::class.java)

  val CONSTRUCTOR : Icon = IconLoader.getIcon("/icons/Constructor.png", SolidityIcons::class.java)
  val RECEIVE : Icon = IconLoader.getIcon("/icons/Receive.png", SolidityIcons::class.java)
  val MODIFIER : Icon = IconLoader.getIcon("/icons/Modifier.png", SolidityIcons::class.java)



  val SHOW_REGION_TOGGLE : Icon = IconLoader.getIcon("/icons/Region.png", SolidityIcons::class.java)
}
