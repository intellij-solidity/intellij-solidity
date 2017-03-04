package me.serce.solidity.lang.stubs

import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IStubFileElementType
import me.serce.solidity.lang.SolidityLanguage
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.lang.psi.SolidityContractDefinition
import me.serce.solidity.lang.psi.SolidityEnumDefinition
import me.serce.solidity.lang.psi.SolidityTypeName
import me.serce.solidity.lang.psi.impl.*

class SolidityFileStub(file: SolidityFile?) : PsiFileStubImpl<SolidityFile>(file) {
  override fun getType() = Type

  object Type : IStubFileElementType<SolidityFileStub>(SolidityLanguage) {
    override fun getBuilder(): StubBuilder = object : DefaultStubBuilder() {
      override fun createStubForFile(file: PsiFile) = SolidityFileStub(file as SolidityFile)
    }

    override fun serialize(stub: SolidityFileStub, dataStream: StubOutputStream) {}

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) = SolidityFileStub(null)

    override fun getExternalId(): String = "Solidity.file"
  }
}

fun factory(name: String): SolidityStubElementType<*, *> = when (name) {
  "ENUM_DEFINITION" -> SolidityEnumDefStub.Type
  "CONTRACT_DEFINITION" -> SolidityContractOrLibDefStub.Type

  "ELEMENTARY_TYPE_NAME" -> SolidityTypeRefStub.Type("ELEMENTARY_TYPE_NAME", ::SolidityElementaryTypeNameImpl)
  "MAPPING_TYPE_NAME" -> SolidityTypeRefStub.Type("MAPPING_TYPE_NAME", ::SolidityMappingTypeNameImpl)
  "FUNCTION_TYPE_NAME" -> SolidityTypeRefStub.Type("FUNCTION_TYPE_NAME", ::SolidityFunctionTypeNameImpl)
  "ARRAY_TYPE_NAME" -> SolidityTypeRefStub.Type("ARRAY_TYPE_NAME", ::SolidityArrayTypeNameImpl)
  "USER_DEFINED_LOCATION_TYPE_NAME" -> SolidityTypeRefStub.Type("USER_DEFINED_LOCATION_TYPE_NAME", ::SolidityUserDefinedLocationTypeNameImpl)

  "USER_DEFINED_TYPE_NAME" -> SolidityTypeRefStub.Type("USER_DEFINED_TYPE_NAME", ::SolidityUserDefinedTypeNameImpl)

  else -> error("Unknown element $name")
}


class SolidityEnumDefStub(parent: StubElement<*>?,
                          elementType: IStubElementType<*, *>,
                          override val name: String?)
  : StubBase<SolidityEnumDefinition>(parent, elementType), SolidityNamedStub {

  object Type : SolidityStubElementType<SolidityEnumDefStub, SolidityEnumDefinition>("ENUM_DEFINITION") {
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
      SolidityEnumDefStub(parentStub, this, dataStream.readNameAsString())

    override fun serialize(stub: SolidityEnumDefStub, dataStream: StubOutputStream) = with(dataStream) {
      writeName(stub.name)
    }

    override fun createPsi(stub: SolidityEnumDefStub) = SolidityEnumDefinitionImpl(stub, this)

    override fun createStub(psi: SolidityEnumDefinition, parentStub: StubElement<*>?) =
      SolidityEnumDefStub(parentStub, this, psi.name)

    override fun indexStub(stub: SolidityEnumDefStub, sink: IndexSink) = sink.indexEnumDef(stub)
  }
}


class SolidityContractOrLibDefStub(parent: StubElement<*>?,
                          elementType: IStubElementType<*, *>,
                          override val name: String?)
  : StubBase<SolidityContractDefinition>(parent, elementType), SolidityNamedStub {

  object Type : SolidityStubElementType<SolidityContractOrLibDefStub, SolidityContractDefinition>("CONTRACT_DEFINITION") {
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
      SolidityContractOrLibDefStub(parentStub, this, dataStream.readNameAsString())

    override fun serialize(stub: SolidityContractOrLibDefStub, dataStream: StubOutputStream) = with(dataStream) {
      writeName(stub.name)
    }

    override fun createPsi(stub: SolidityContractOrLibDefStub) = SolidityContractDefinitionImpl(stub, this)

    override fun createStub(psi: SolidityContractDefinition, parentStub: StubElement<*>?) =
      SolidityContractOrLibDefStub(parentStub, this, psi.name)

    override fun indexStub(stub: SolidityContractOrLibDefStub, sink: IndexSink) = sink.indexContractDef(stub)
  }
}

class SolidityTypeRefStub(parent: StubElement<*>?,
                          elementType: IStubElementType<*, *>)
  : StubBase<SolidityTypeName>(parent, elementType) {

  class Type<T : SolidityTypeName>(val debugName: String,
                                   private val psiFactory: (SolidityTypeRefStub, IStubElementType<*, *>) -> T)
    : SolidityStubElementType<SolidityTypeRefStub, T>(debugName) {
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
      SolidityTypeRefStub(parentStub, this)

    override fun serialize(stub: SolidityTypeRefStub, dataStream: StubOutputStream) = with(dataStream) {
    }

    override fun createPsi(stub: SolidityTypeRefStub) = psiFactory(stub, this)

    override fun createStub(psi: T, parentStub: StubElement<*>?) = SolidityTypeRefStub(parentStub, this)

    override fun indexStub(stub: SolidityTypeRefStub, sink: IndexSink) {}
  }
}

private fun StubInputStream.readNameAsString(): String? = readName()?.string
