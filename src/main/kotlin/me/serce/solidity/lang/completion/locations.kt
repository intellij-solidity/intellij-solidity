package me.serce.solidity.lang.completion

import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.lang.core.SolidityTokenTypes
import me.serce.solidity.lang.psi.*


fun emitStartStatement() =
  psiElement(SolidityTokenTypes.IDENTIFIER)
    .afterLeaf(psiElement(SolidityTokenTypes.EMIT))

fun revertStartStatement() =
  psiElement(SolidityTokenTypes.IDENTIFIER)
    .inside(SolRevertStatement::class.java)
    .afterLeaf(psiElement(SolidityTokenTypes.REVERT_STATEMENT))

fun stateVarInsideContract(): ElementPattern<PsiElement> =
  psiElement(SolidityTokenTypes.IDENTIFIER)
    .inside(psiElement(SolPrimaryExpression::class.java))
    .inside(SolidityFile::class.java)

fun rootDeclaration(): ElementPattern<PsiElement> = psiElement()
  .withSuperParent(2, SolPrimaryExpression::class.java)
  .withSuperParent(3, SolidityFile::class.java)

fun expression(): ElementPattern<PsiElement> =
  StandardPatterns.or(
    functionCall(), primaryExpression(), functionCallArguments()
  )

fun functionCall(): ElementPattern<PsiElement> =
  psiElement(SolidityTokenTypes.IDENTIFIER).inside(SolFunctionCallExpression::class.java)

fun primaryExpression(): ElementPattern<PsiElement> =
  psiElement(SolidityTokenTypes.IDENTIFIER).inside(SolPrimaryExpression::class.java)

fun functionCallArguments(): ElementPattern<PsiElement> =
  psiElement(SolidityTokenTypes.IDENTIFIER).inside(SolFunctionCallArguments::class.java)

fun mapExpression(): ElementPattern<PsiElement> =
  psiElement(SolidityTokenTypes.IDENTIFIER).inside(SolMapExpression::class.java)

fun pathImportExpression(): ElementPattern<PsiElement> = StandardPatterns.or(
  psiElement(SolidityTokenTypes.STRINGLITERAL).inside(SolImportPath::class.java),
  psiElement(SolidityTokenTypes.STRINGLITERAL).afterLeaf(psiElement().withText("from"))
  )

fun insideContract(): ElementPattern<PsiElement> = psiElement()
  .inside(psiElement(SolPrimaryExpression::class.java))
  .inside(SolidityFile::class.java)

fun insideFunction(): ElementPattern<PsiElement> = psiElement().inside(psiElement(SolFunctionDefinition::class.java))

fun inMemberAccess(): ElementPattern<PsiElement> = psiElement().inside(SolMemberAccessExpression::class.java)

fun inFunctionParameterDef(): ElementPattern<PsiElement> = psiElement()
  .inside(SolParameterDef::class.java)

fun inImportDeclaration(): ElementPattern<PsiElement> = psiElement()
  .inside(SolImportDirective::class.java)

fun inStateVariableDeclaration(): ElementPattern<PsiElement> = psiElement(SolidityTokenTypes.IDENTIFIER)
  .inside(psiElement(SolPrimaryExpression::class.java))

fun inFunctionDeclaration(): ElementPattern<PsiElement> = psiElement()
  .inside(SolFunctionDefinition::class.java)

fun inConstructorDeclaration(): ElementPattern<PsiElement> = psiElement()
  .inside(SolConstructorDefinition::class.java)

fun inEventDeclaration(): ElementPattern<PsiElement> = psiElement()
  .inside(SolEventDefinition::class.java)

fun inUsingForDeclaration(): ElementPattern<PsiElement> = psiElement()
  .inside(SolUsingForDeclaration::class.java)

fun insideLoop(): ElementPattern<PsiElement> = psiElement().inside(
  StandardPatterns.or(
    psiElement(SolWhileStatement::class.java),
    psiElement(SolForStatement::class.java)
  )
)

fun isAfterAddressKeyword(): ElementPattern<PsiElement> =
  psiElement()
    .afterLeaf(psiElement(SolidityTokenTypes.ADDRESS))

fun inMappingDeclaration(): ElementPattern<PsiElement> = StandardPatterns.or(
  psiElement()
    .afterLeaf(psiElement(SolidityTokenTypes.LPAREN).afterLeaf(psiElement(SolidityTokenTypes.MAPPING))),
  psiElement().afterLeaf(psiElement(SolidityTokenTypes.TO))
)


