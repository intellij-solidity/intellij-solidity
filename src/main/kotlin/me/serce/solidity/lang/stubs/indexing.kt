package me.serce.solidity.lang.stubs

import com.intellij.psi.stubs.IndexSink


fun IndexSink.indexEnumDef(stub: SolidityEnumDefStub) {
  indexNamedStub(stub)
  indexGotoClass(stub)
}

fun IndexSink.indexContractDef(stub: SolidityContractOrLibDefStub) {
  indexNamedStub(stub)
  indexGotoClass(stub)
}


private fun IndexSink.indexNamedStub(stub: SolidityNamedStub) {
  stub.name?.let {
    occurrence(SolidityNamedElementIndex.KEY, it)
  }
}

private fun IndexSink.indexGotoClass(stub: SolidityNamedStub) {
  stub.name?.let {
    occurrence(SolidityGotoClassIndex.KEY, it)
  }
}
