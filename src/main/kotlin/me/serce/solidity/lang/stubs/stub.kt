package me.serce.solidity.lang.stubs

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import me.serce.solidity.lang.SolidityLanguage
import me.serce.solidity.lang.psi.SolidityElement

interface SolidityNamedStub {
  val name: String?
}

abstract class SolidityStubElementType<S : StubElement<*>, P : SolidityElement>(debugName: String)
  : IStubElementType<S, P>(debugName, SolidityLanguage) {
  final override fun getExternalId(): String = "solidity.${super.toString()}"
}
