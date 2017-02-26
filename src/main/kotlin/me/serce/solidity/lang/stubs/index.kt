package me.serce.solidity.lang.stubs

import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import me.serce.solidity.lang.psi.SolidityNamedElement

class SolidityGotoClassIndex : StringStubIndexExtension<SolidityNamedElement>() {
  companion object {
    val KEY: StubIndexKey<String, SolidityNamedElement> = StubIndexKey.createIndexKey(SolidityGotoClassIndex::class.java.canonicalName)
  }

  override fun getVersion(): Int = SolidityFileStub.Type.stubVersion
  override fun getKey(): StubIndexKey<String, SolidityNamedElement> = KEY
}


class SolidityNamedElementIndex : StringStubIndexExtension<SolidityNamedElement>() {
  companion object {
    val KEY: StubIndexKey<String, SolidityNamedElement> = StubIndexKey.createIndexKey(SolidityNamedElementIndex::class.java.canonicalName)
  }

  override fun getVersion(): Int = SolidityFileStub.Type.stubVersion
  override fun getKey(): StubIndexKey<String, SolidityNamedElement> = KEY
}
