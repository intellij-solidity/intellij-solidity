package me.serce.solidity.lang.psi.impl.mixin

import com.intellij.lang.ASTNode
import me.serce.solidity.lang.psi.impl.SolidityNamedElementImpl
import me.serce.solidity.lang.resolve.ref.SolidityPathReference

open class SolidityImportPathElement(node: ASTNode) : SolidityNamedElementImpl(node) {
  override fun getReference() = SolidityPathReference(this)
}

