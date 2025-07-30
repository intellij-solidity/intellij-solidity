package me.serce.solidity.lang.core

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.lang.LanguageExtensionPoint
import com.intellij.openapi.extensions.Extensions
import com.intellij.testFramework.ParsingTestCase

abstract class SolidityParsingTestBase(baseDir: String) : ParsingTestCase(baseDir, "sol", true, SolidityParserDefinition()) {

  override fun setUp() {
    super.setUp()
    CoreApplicationEnvironment.registerExtensionPoint(
      Extensions.getRootArea(), "com.intellij.lang.braceMatcher", LanguageExtensionPoint::class.java)
  }

  override fun getTestDataPath() = "src/test/resources"
}

class SolidityCompleteCustomParsingTest : SolidityParsingTestBase("fixtures/parser") {
  fun testAddress() = doTest(true, true)
  fun testArithmetic() = doTest(true, true)
  fun testArrays() = doTest(true, true)
  fun testAssembly() = doTest(true, true)
  fun testAssignments() = doTest(true, true)
  fun testBytes() = doTest(true, true)
  fun testContract() = doTest(true, true)
  fun testConstants() = doTest(true, true)
  fun testCalldataArray() = doTest(true, true)
  fun testCallChain() = doTest(true, true)
  fun testCallOptions() = doTest(true, true)
  fun testComments() = doTest(true, true)
  fun testCommentsEOL() = doTest(true, true)
  fun testConstructor() = doTest(true, true)
  fun testContractWithDifferentFields() = doTest(true, true)
  fun testContractWithLiterals() = doTest(true, true)
  fun testCustomStorageLayout() = doTest(true, true)
  fun testDestructuring() = doTest(true, true)
  fun testDigits() = doTest(true, true)
  fun testEmit() = doTest(true, true)
  fun testEnums() = doTest(true, true)
  fun testEvent() = doTest(true, true)
  fun testError() = doTest(true, true)
  fun testFallback() = doTest(true, true)
  fun testFunctions() = doTest(true, true)
  fun testImports() = doTest(true, true)
  fun testIdentifier() = doTest(true, true)
  fun testInterfaces() = doTest(true, true)
  fun testLibrary() = doTest(true, true)
  fun testLiterals() = doTest(true, true)
  fun testLocationSpecifier() = doTest(true, true)
  fun testMapInvoke() = doTest(true, true)
  fun testMappings() = doTest(true, true)
  fun testModifiers() = doTest(true, true)
  fun testNatSpec() = doTest(true, true)
  fun testNew() = doTest(true, true)
  fun testPragma() = doTest(true, true)
  fun testReturnTuples() = doTest(true, true)
  fun testSlice() = doTest(true, true)
  fun testStateMutability() = doTest(true, true)
  fun testStateVars() = doTest(true, true)
  fun testTernary() = doTest(true, true)
  fun testTransient() = doTest(true, true)
  fun testTryCatch() = doTest(true, true)
  fun testTypeExpression() = doTest(true, true)
  fun testUnchecked() = doTest(true, true)
  fun testUserDefinedValueTypes() = doTest(true, true)
  fun testUsing() = doTest(true, true)
  // The code below test is inlined as is rather than being placed in a .sol file
  // because prior to 2.4.7, the parser would cause IntelliJ to hang on this input.
  fun testDeepNesting() = doCodeTest("""
    contract C {
        function f() public pure {
            uint ok = 0;
            uint nok = 0;

            (-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-ok++))))))))))))))))))))))))))))));

            (-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-(-nok++))))))))))))))))))))))))))))));
        }
    }
  """.trimIndent())
}

class SolidityCompleteOfficialSuccessfulParsingTest :
  SolidityParsingTestBase("fixtures/parser/solidity_official_tests/shouldSucceed") {
  fun testArraysInEvents() = doTest(true, true)
  fun testArraysInExpressions() = doTest(true, true)
  fun testArraysInStorage() = doTest(true, true)
  fun testCallingFunction() = doTest(true, true)
  fun testCommentEndWithDoubleStar() = doTest(true, true)
  fun testComplexExpression() = doTest(true, true)
  fun testComplexImport() = doTest(true, true)
  fun testConditionalMultiple() = doTest(true, true)
  fun testConditionalTrueFalseLiteral() = doTest(true, true)
  fun testConditionalWithAssignment() = doTest(true, true)
  fun testConditionalWithConstants() = doTest(true, true)
  fun testConditionalWithVariables() = doTest(true, true)
  fun testContractInheritance() = doTest(true, true)
  fun testContractMultipleInheritance() = doTest(true, true)
  fun testContractMultipleInheritanceWithArguments() = doTest(true, true)
  fun testDeclaringFixedAndUfixedVariables() = doTest(true, true)
  fun testDeclaringFixedLiteralVariables() = doTest(true, true)
  fun testElseIfStatement() = doTest(true, true)
  fun testEmptyComment() = doTest(true, true)
  fun testEmptyFunction() = doTest(true, true)
  fun testEnumValidDeclaration() = doTest(true, true)
  fun testEvent() = doTest(true, true)
  fun testEventArguments() = doTest(true, true)
  fun testEventArgumentsIndexed() = doTest(true, true)
  fun testExpExpression() = doTest(true, true)
  fun testExternalFunction() = doTest(true, true)
  fun testFallbackFunction() = doTest(true, true)
  fun testForLoopSimpleInitexpr() = doTest(true, true)
  fun testForLoopSimpleNoexpr() = doTest(true, true)
  fun testForLoopSingleStmtBody() = doTest(true, true)
  fun testForLoopVardefInitexpr() = doTest(true, true)
  fun testFromIsNotKeyword() = doTest(true, true)
  fun testFunctionNatspecDocumentation() = doTest(true, true)
  fun testFunctionNoBody() = doTest(true, true)
  fun testFunctionNormalComments() = doTest(true, true)
  fun testFunctionTypeAsParameter() = doTest(true, true)
  fun testFunctionTypeAsStorageVariable() = doTest(true, true)
  fun testFunctionTypeAsStorageVariableWithAssignment() = doTest(true, true)
  fun testFunctionTypeInExpression() = doTest(true, true)
  fun testFunctionTypeInStruct() = doTest(true, true)
  fun testFunctionTypeStateVariable() = doTest(true, true)
  fun testIfStatement() = doTest(true, true)
  fun testImportDirective() = doTest(true, true)
  fun testInlineArrayDeclaration() = doTest(true, true)
  fun testLibrarySimple() = doTest(true, true)
  fun testLiteralConstantsWithEtherSubdenominations() = doTest(true, true)
  fun testLiteralConstantsWithEtherSubdenominationsInExpressions() = doTest(true, true)
  fun testLocationSpecifiersForLocals() = doTest(true, true)
  fun testLocationSpecifiersForParams() = doTest(true, true)
  fun testMapping() = doTest(true, true)
  fun testMappingAndArrayOfFunctions() = doTest(true, true)
  fun testMappingInStruct() = doTest(true, true)
  fun testMappingToMappingInStruct() = doTest(true, true)
  fun testMemberAccessParserAmbiguity() = doTest(true, true)
  fun testModifier() = doTest(true, true)
  fun testModifierArguments() = doTest(true, true)
  fun testModifierInvocation() = doTest(true, true)
  fun testMultiArrays() = doTest(true, true)
  fun testMultiVariableDeclaration() = doTest(true, true)
  fun testMultilineFunctionDocumentation() = doTest(true, true)
  fun testMultipleContracts() = doTest(true, true)
  fun testMultipleContractsAndImports() = doTest(true, true)
  fun testMultipleFunctionsNatspecDocumentation() = doTest(true, true)
  fun testNatspecCommentInFunctionBody() = doTest(true, true)
  fun testNatspecDocstringAfterSignature() = doTest(true, true)
  fun testNatspecDocstringBetweenKeywordAndSignature() = doTest(true, true)
  fun testNoFunctionParams() = doTest(true, true)
  fun testOperatorExpression() = doTest(true, true)
  fun testOverloadedFunctions() = doTest(true, true)
  fun testPlaceholderInFunctionContext() = doTest(true, true)
  fun testSingleFunctionParam() = doTest(true, true)
  fun testSmokeTest() = doTest(true, true)
  fun testStatementStartingWithTypeConversion() = doTest(true, true)
  fun testStructDefinition() = doTest(true, true)
  fun testTuples() = doTest(true, true)
  fun testTwoExactFunctions() = doTest(true, true)
  fun testTypeConversionToDynamicArray() = doTest(true, true)
  fun testUsingFor() = doTest(true, true)
  fun testVariableDefinition() = doTest(true, true)
  fun testVariableDefinitionWithInitialization() = doTest(true, true)
  fun testVisibilitySpecifiers() = doTest(true, true)
  fun testWhileLoop() = doTest(true, true)
}

class SolidityCompleteOfficialFailingParsingTest :
  SolidityParsingTestBase("fixtures/parser/solidity_official_tests/shouldFail") {
  fun testConstantIsKeyword() = doTest(true)
//  fun testEmptyEnumDeclaration() = doTest(true)
  fun testExternalVariable() = doTest(true)
  fun testFunctionTypeAsStorageVariableWithModifiers() = doTest(true)
  fun testInlineArrayEmptyCellsCheckLvalue() = doTest(true)
  fun testInlineArrayEmptyCellsCheckWithoutLvalue() = doTest(true)
  fun testInvalidFixedConversionLeadingZeroesCheck() = doTest(true)
  fun testLocalConstVariable() = doTest(true)
  fun testLocationSpecifiersForState() = doTest(true)
  fun testLocationSpecifiersWithVar() = doTest(true)
  fun testMalformedEnumDeclaration() = doTest(true)
  fun testMissingArgumentInNamedArgs() = doTest(true)
  fun testMissingParameterNameInNamedArgs() = doTest(true)
  fun testMissingVariableNameInDeclaration() = doTest(true)
  fun testModifierWithoutSemicolon() = doTest(true)
  fun testNoDoubleRadixInFixedLiteral() = doTest(true)
  fun testPayableAccessor() = doTest(true)
  fun testTransient() = doTest(true)
  fun testVarArray() = doTest(true)
//  fun testVariableDefinitionInFunctionParameter() = doTest(true)
//  fun testVariableDefinitionInFunctionReturn() = doTest(true)
//  fun testVariableDefinitionInMapping() = doTest(true)
}
