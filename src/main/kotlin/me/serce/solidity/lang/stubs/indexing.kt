package me.serce.solidity.lang.stubs

import com.intellij.psi.stubs.IndexSink


fun IndexSink.indexEnumDef(stub: SolEnumDefStub) {
  indexNamedStub(stub)
  indexGotoClass(stub)
}

fun IndexSink.indexContractDef(stub: SolContractOrLibDefStub) {
  indexNamedStub(stub)
  indexGotoClass(stub)
}

fun IndexSink.indexFunctionDef(stub: SolFunctionDefStub) {
  indexNamedStub(stub)
}

fun IndexSink.indexModifierDef(stub: SolModifierDefStub) {
  indexNamedStub(stub)
  indexModifier(stub)
}

fun IndexSink.indexStateVarDecl(stub: SolStateVarDeclStub) {
  indexNamedStub(stub)
}


private fun IndexSink.indexModifier(stub: SolModifierDefStub) {
  stub.name?.let {
    occurrence(SolModifierIndex.KEY, it)
  }
}

private fun IndexSink.indexNamedStub(stub: SolNamedStub) {
  stub.name?.let {
    occurrence(SolNamedElementIndex.KEY, it)
  }
}

private fun IndexSink.indexGotoClass(stub: SolNamedStub) {
  stub.name?.let {
    occurrence(SolGotoClassIndex.KEY, it)
  }
}
