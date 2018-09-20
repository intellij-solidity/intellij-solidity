package me.serce.solidity.lang.stubs

import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import me.serce.solidity.lang.psi.SolNamedElement

class SolGotoClassIndex : StringStubIndexExtension<SolNamedElement>() {
  companion object {
    val KEY: StubIndexKey<String, SolNamedElement> = StubIndexKey.createIndexKey(SolGotoClassIndex::class.java.canonicalName)
  }

  override fun getVersion(): Int = SolidityFileStub.Type.stubVersion
  override fun getKey(): StubIndexKey<String, SolNamedElement> = KEY
}

class SolModifierIndex : StringStubIndexExtension<SolNamedElement>() {
  companion object {
    val KEY: StubIndexKey<String, SolNamedElement> = StubIndexKey.createIndexKey(SolModifierIndex::class.java.canonicalName)
  }

  override fun getVersion(): Int = SolidityFileStub.Type.stubVersion
  override fun getKey(): StubIndexKey<String, SolNamedElement> = KEY
}

class SolNamedElementIndex : StringStubIndexExtension<SolNamedElement>() {
  companion object {
    val KEY: StubIndexKey<String, SolNamedElement> = StubIndexKey.createIndexKey(SolNamedElementIndex::class.java.canonicalName)
  }

  override fun getVersion(): Int = SolidityFileStub.Type.stubVersion
  override fun getKey(): StubIndexKey<String, SolNamedElement> = KEY
}

class SolEventIndex : StringStubIndexExtension<SolNamedElement>() {
  companion object {
    val KEY: StubIndexKey<String, SolNamedElement> = StubIndexKey.createIndexKey(SolEventIndex::class.java.canonicalName)
  }

  override fun getVersion(): Int = SolidityFileStub.Type.stubVersion
  override fun getKey(): StubIndexKey<String, SolNamedElement> = KEY
}

class SolFunctionIndex : StringStubIndexExtension<SolNamedElement>() {
  companion object {
    val KEY: StubIndexKey<String, SolNamedElement> = StubIndexKey.createIndexKey(SolFunctionIndex::class.java.canonicalName)
  }

  override fun getVersion(): Int = SolidityFileStub.Type.stubVersion
  override fun getKey(): StubIndexKey<String, SolNamedElement> = KEY
}