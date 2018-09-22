package me.serce.solidity.lang.completion

import com.intellij.patterns.PlatformPatterns
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.lang.core.SolidityTokenTypes
import me.serce.solidity.lang.psi.SolPrimaryExpression

fun emitStartStatement() = PlatformPatterns
        .psiElement(SolidityTokenTypes.IDENTIFIER)
        .afterLeaf(PlatformPatterns.psiElement(SolidityTokenTypes.EMIT))

fun stateVarInsideContract() = PlatformPatterns
  .psiElement(SolidityTokenTypes.IDENTIFIER)
  .inside(PlatformPatterns.psiElement(SolPrimaryExpression::class.java))
  .inside(SolidityFile::class.java)
