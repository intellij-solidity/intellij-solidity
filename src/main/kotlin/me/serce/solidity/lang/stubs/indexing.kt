package me.serce.solidity.lang.stubs

import com.intellij.psi.stubs.IndexSink

fun IndexSink.indexEnumDef(stub: SolEnumDefStub) {
  indexNamedStub(stub)
  indexGotoClass(stub)
}

fun IndexSink.indexUserDefinedValueTypeDef(stub: SolUserDefinedValueTypeDefStub) {
  indexNamedStub(stub)
  indexGotoClass(stub)
}

fun IndexSink.indexContractDef(stub: SolContractOrLibDefStub) {
  indexNamedStub(stub)
  indexGotoClass(stub)
}

fun IndexSink.indexStructDef(stub: SolStructDefStub) {
  indexNamedStub(stub)
  indexGotoClass(stub)
}

fun IndexSink.indexFunctionDef(stub: SolFunctionDefStub) {
  indexNamedStub(stub)
  indexFunction(stub)
}

fun IndexSink.indexModifierDef(stub: SolModifierDefStub) {
  indexNamedStub(stub)
  indexModifier(stub)
}

fun IndexSink.indexStateVarDecl(stub: SolStateVarDeclStub) {
  indexNamedStub(stub)
}

fun IndexSink.indexConstantVariableDecl(stub: SolConstantVariableDeclStub) {
  indexNamedStub(stub)
}

fun IndexSink.indexEventDef(stub: SolEventDefStub) {
  indexNamedStub(stub)
  indexEvent(stub)
  indexGotoClass(stub)
}

fun IndexSink.indexErrorDef(stub: SolErrorDefStub) {
  indexNamedStub(stub)
  indexError(stub)
  indexGotoClass(stub)
}

fun IndexSink.indexImportPathDef(stub: SolImportPathDefStub) {
  indexNamedStub(stub)
  indexImportPath(stub)
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

private fun IndexSink.indexEvent(stub: SolEventDefStub) {
  stub.name?.let {
    occurrence(SolEventIndex.KEY, it)
  }
}

private fun IndexSink.indexError(stub: SolErrorDefStub) {
  stub.name?.let {
    occurrence(SolErrorIndex.KEY, it)
  }
}

private fun IndexSink.indexFunction(stub: SolFunctionDefStub) {
  stub.name?.let {
    occurrence(SolFunctionIndex.KEY, it)
  }
}

private fun IndexSink.indexImportPath(stub: SolImportPathDefStub) {
  stub.path?.let {
    occurrence(SolImportIndex.KEY, it)
  }
}
