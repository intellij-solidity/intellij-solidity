package me.serce.solidity.lang.completion

import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.lang.core.SolidityTokenTypes
import me.serce.solidity.lang.psi.SolBlock
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.lang.psi.SolPrimaryExpression

fun emitStartStatement() =
  psiElement(SolidityTokenTypes.IDENTIFIER)
    .afterLeaf(psiElement(SolidityTokenTypes.EMIT))

fun stateVarInsideContract() =
  psiElement(SolidityTokenTypes.IDENTIFIER)
    .inside(psiElement(SolPrimaryExpression::class.java))
    .inside(SolidityFile::class.java)

fun insideFunctionDefinition(): ElementPattern<PsiElement> =
  psiElement(SolidityTokenTypes.IDENTIFIER)
    .inside(psiElement(SolBlock::class.java))
    .inside(psiElement(SolFunctionDefinition::class.java))
