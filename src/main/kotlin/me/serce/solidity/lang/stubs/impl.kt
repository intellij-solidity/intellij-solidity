package me.serce.solidity.lang.stubs

import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IStubFileElementType
import me.serce.solidity.lang.SolidityLanguage
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.psi.impl.*

class SolidityFileStub(file: SolidityFile?) : PsiFileStubImpl<SolidityFile>(file) {
  override fun getType() = Type

  object Type : IStubFileElementType<SolidityFileStub>(SolidityLanguage) {
    // bump version every time stub tree changes
    override fun getStubVersion() = 7

    override fun getBuilder(): StubBuilder = object : DefaultStubBuilder() {
      override fun createStubForFile(file: PsiFile) = SolidityFileStub(file as SolidityFile)
    }

    override fun serialize(stub: SolidityFileStub, dataStream: StubOutputStream) {}

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) = SolidityFileStub(null)

    override fun getExternalId(): String = "Solidity.file"
  }
}

fun factory(name: String): SolStubElementType<*, *> = when (name) {
  "ENUM_DEFINITION" -> SolEnumDefStub.Type
  "CONTRACT_DEFINITION" -> SolContractOrLibDefStub.Type
  "FUNCTION_DEFINITION" -> SolFunctionDefStub.Type
  "MODIFIER_DEFINITION" -> SolModifierDefStub.Type
  "STATE_VARIABLE_DECLARATION" -> SolStateVarDeclStub.Type

  "ELEMENTARY_TYPE_NAME" -> SolTypeRefStub.Type("ELEMENTARY_TYPE_NAME", ::SolElementaryTypeNameImpl)
  "MAPPING_TYPE_NAME" -> SolTypeRefStub.Type("MAPPING_TYPE_NAME", ::SolMappingTypeNameImpl)
  "FUNCTION_TYPE_NAME" -> SolTypeRefStub.Type("FUNCTION_TYPE_NAME", ::SolFunctionTypeNameImpl)
  "ARRAY_TYPE_NAME" -> SolTypeRefStub.Type("ARRAY_TYPE_NAME", ::SolArrayTypeNameImpl)
  "USER_DEFINED_LOCATION_TYPE_NAME" -> SolTypeRefStub.Type("USER_DEFINED_LOCATION_TYPE_NAME", ::SolUserDefinedLocationTypeNameImpl)

  "USER_DEFINED_TYPE_NAME" -> SolTypeRefStub.Type("USER_DEFINED_TYPE_NAME", ::SolUserDefinedTypeNameImpl)

  else -> error("Unknown element $name")
}


class SolEnumDefStub(parent: StubElement<*>?,
                     elementType: IStubElementType<*, *>,
                     override val name: String?)
  : StubBase<SolEnumDefinition>(parent, elementType), SolNamedStub {

  object Type : SolStubElementType<SolEnumDefStub, SolEnumDefinition>("ENUM_DEFINITION") {
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
      SolEnumDefStub(parentStub, this, dataStream.readNameAsString())

    override fun serialize(stub: SolEnumDefStub, dataStream: StubOutputStream) = with(dataStream) {
      writeName(stub.name)
    }

    override fun createPsi(stub: SolEnumDefStub) = SolEnumDefinitionImpl(stub, this)

    override fun createStub(psi: SolEnumDefinition, parentStub: StubElement<*>?) =
      SolEnumDefStub(parentStub, this, psi.name)

    override fun indexStub(stub: SolEnumDefStub, sink: IndexSink) = sink.indexEnumDef(stub)
  }
}

class SolFunctionDefStub(parent: StubElement<*>?,
                         elementType: IStubElementType<*, *>,
                         override val name: String?)
  : StubBase<SolFunctionDefinition>(parent, elementType), SolNamedStub {

  object Type : SolStubElementType<SolFunctionDefStub, SolFunctionDefinition>("FUNCTION_DEFINITION") {
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
      SolFunctionDefStub(parentStub, this, dataStream.readNameAsString())

    override fun serialize(stub: SolFunctionDefStub, dataStream: StubOutputStream) = with(dataStream) {
      writeName(stub.name)
    }

    override fun createPsi(stub: SolFunctionDefStub) = SolFunctionDefinitionImpl(stub, this)

    override fun createStub(psi: SolFunctionDefinition, parentStub: StubElement<*>?) =
      SolFunctionDefStub(parentStub, this, psi.name)

    override fun indexStub(stub: SolFunctionDefStub, sink: IndexSink) = sink.indexFunctionDef(stub)
  }
}

class SolModifierDefStub(parent: StubElement<*>?,
                         elementType: IStubElementType<*, *>,
                         override val name: String?)
  : StubBase<SolModifierDefinition>(parent, elementType), SolNamedStub {

  object Type : SolStubElementType<SolModifierDefStub, SolModifierDefinition>("MODIFIER_DEFINITION") {
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
      SolModifierDefStub(parentStub, this, dataStream.readNameAsString())

    override fun serialize(stub: SolModifierDefStub, dataStream: StubOutputStream) = with(dataStream) {
      writeName(stub.name)
    }

    override fun createPsi(stub: SolModifierDefStub) = SolModifierDefinitionImpl(stub, this)

    override fun createStub(psi: SolModifierDefinition, parentStub: StubElement<*>?) =
      SolModifierDefStub(parentStub, this, psi.name)

    override fun indexStub(stub: SolModifierDefStub, sink: IndexSink) = sink.indexModifierDef(stub)
  }
}


class SolStateVarDeclStub(parent: StubElement<*>?,
                          elementType: IStubElementType<*, *>,
                          override val name: String?)
  : StubBase<SolStateVariableDeclaration>(parent, elementType), SolNamedStub {

  object Type : SolStubElementType<SolStateVarDeclStub, SolStateVariableDeclaration>("STATE_VARIABLE_DECLARATION") {
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
      SolStateVarDeclStub(parentStub, this, dataStream.readNameAsString())

    override fun serialize(stub: SolStateVarDeclStub, dataStream: StubOutputStream) = with(dataStream) {
      writeName(stub.name)
    }

    override fun createPsi(stub: SolStateVarDeclStub) = SolStateVariableDeclarationImpl(stub, this)

    override fun createStub(psi: SolStateVariableDeclaration, parentStub: StubElement<*>?) =
      SolStateVarDeclStub(parentStub, this, psi.name)

    override fun indexStub(stub: SolStateVarDeclStub, sink: IndexSink) = sink.indexStateVarDecl(stub)
  }
}


class SolContractOrLibDefStub(parent: StubElement<*>?,
                              elementType: IStubElementType<*, *>,
                              override val name: String?)
  : StubBase<SolContractDefinition>(parent, elementType), SolNamedStub {

  object Type : SolStubElementType<SolContractOrLibDefStub, SolContractDefinition>("CONTRACT_DEFINITION") {
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
      SolContractOrLibDefStub(parentStub, this, dataStream.readNameAsString())

    override fun serialize(stub: SolContractOrLibDefStub, dataStream: StubOutputStream) = with(dataStream) {
      writeName(stub.name)
    }

    override fun createPsi(stub: SolContractOrLibDefStub) = SolContractDefinitionImpl(stub, this)

    override fun createStub(psi: SolContractDefinition, parentStub: StubElement<*>?) =
      SolContractOrLibDefStub(parentStub, this, psi.name)

    override fun indexStub(stub: SolContractOrLibDefStub, sink: IndexSink) = sink.indexContractDef(stub)
  }
}

class SolTypeRefStub(parent: StubElement<*>?,
                     elementType: IStubElementType<*, *>)
  : StubBase<SolTypeName>(parent, elementType) {

  class Type<T : SolTypeName>(val debugName: String,
                              private val psiFactory: (SolTypeRefStub, IStubElementType<*, *>) -> T)
    : SolStubElementType<SolTypeRefStub, T>(debugName) {
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
      SolTypeRefStub(parentStub, this)

    override fun serialize(stub: SolTypeRefStub, dataStream: StubOutputStream) = with(dataStream) {
    }

    override fun createPsi(stub: SolTypeRefStub) = psiFactory(stub, this)

    override fun createStub(psi: T, parentStub: StubElement<*>?) = SolTypeRefStub(parentStub, this)

    override fun indexStub(stub: SolTypeRefStub, sink: IndexSink) {}
  }
}

private fun StubInputStream.readNameAsString(): String? = readName()?.string
