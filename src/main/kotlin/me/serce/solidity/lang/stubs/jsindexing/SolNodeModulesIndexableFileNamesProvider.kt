package me.serce.solidity.lang.stubs.jsindexing

import com.intellij.lang.javascript.modules.NodeModulesIndexableFileNamesProvider

/**
 * IDEs supporting JavaScript (WebStorm, IJ Ultimate, etc.) automatically exclude node_modules folders from indexing
 * except for a certain set of files which breaks resolving and anything else that relies on indexes. To combat this
 * behaviour, we explicitly register Solidity files to be indexed.
 */
class SolNodeModulesIndexableFileNamesProvider : NodeModulesIndexableFileNamesProvider() {
  override fun getIndexableExtensions(kind: DependencyKind): List<String> = listOf(".sol")
}
