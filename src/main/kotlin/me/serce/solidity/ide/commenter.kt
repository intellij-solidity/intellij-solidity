package me.serce.solidity.ide

import com.intellij.lang.Commenter

class SolidityCommenter: Commenter {
    override fun getLineCommentPrefix() = "//"

    override fun getCommentedBlockCommentPrefix() = null
    override fun getCommentedBlockCommentSuffix() = null
    override fun getBlockCommentPrefix() = null
    override fun getBlockCommentSuffix() = null
}