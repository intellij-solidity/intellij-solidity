package me.serce.solidity.lang.completion

import com.intellij.patterns.PlatformPatterns
import me.serce.solidity.lang.core.SolidityTokenTypes

fun emitStartStatement() = PlatformPatterns
        .psiElement(SolidityTokenTypes.IDENTIFIER)
        .afterLeaf(PlatformPatterns.psiElement(SolidityTokenTypes.EMIT))