package me.serce.solidity.lang.core

import com.intellij.lang.ASTNode
import com.intellij.lang.LanguageUtil
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import me.serce.solidity.SolidityParser
import me.serce.solidity.lang.core.SolidityTokenTypes
import me.serce.solidity.lang.SolidityLanguage
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.lang.core.SolidityLexer

class SolidityParserDefinition : ParserDefinition {
    override fun createParser(project: Project?): PsiParser = SolidityParser()

    override fun createFile(viewProvider: FileViewProvider): PsiFile = SolidityFile(viewProvider)

    override fun spaceExistanceTypeBetweenTokens(left: ASTNode?, right: ASTNode?): ParserDefinition.SpaceRequirements? =
            LanguageUtil.canStickTokensTogetherByLexer(left, right, SolidityLexer())

    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

    override fun getWhitespaceTokens(): TokenSet = WHITE_SPACES

    override fun getCommentTokens(): TokenSet = COMMENTS

    override fun getFileNodeType(): IFileElementType? = FILE

    override fun createLexer(project: Project?): Lexer = SolidityLexer()

    override fun createElement(node: ASTNode?): PsiElement = SolidityTokenTypes.Factory.createElement(node)

    companion object {
        val FILE: IFileElementType = IFileElementType(SolidityLanguage)
        val WHITE_SPACES: TokenSet = TokenSet.create(TokenType.WHITE_SPACE)
        val COMMENTS: TokenSet = TokenSet.create(SolidityTokenTypes.COMMENT)
    }

}