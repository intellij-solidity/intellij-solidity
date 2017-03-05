package me.serce.solidity.ide.navigation

import com.intellij.navigation.ChooseByNameContributor
import com.intellij.navigation.GotoClassContributor
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import me.serce.solidity.lang.psi.SolNamedElement
import me.serce.solidity.lang.stubs.SolGotoClassIndex


abstract class SolNavigationContributorBase<T>(
  private val indexKey: StubIndexKey<String, T>,
  private val clazz: Class<T>) : ChooseByNameContributor, GotoClassContributor where T : NavigationItem, T : SolNamedElement {

  override fun getNames(project: Project?, includeNonProjectItems: Boolean): Array<out String> = when (project) {
    null -> emptyArray()
    else -> StubIndex.getInstance().getAllKeys(indexKey, project).toTypedArray()
  }

  override fun getItemsByName(name: String?,
                              pattern: String?,
                              project: Project?,
                              includeNonProjectItems: Boolean): Array<out NavigationItem> {

    if (project == null || name == null) {
      return emptyArray()
    }
    val scope = when {
      includeNonProjectItems -> GlobalSearchScope.allScope(project)
      else -> GlobalSearchScope.projectScope(project)
    }

    return StubIndex.getElements(indexKey, name, project, scope, clazz).toTypedArray<NavigationItem>()
  }

  override fun getQualifiedName(item: NavigationItem?): String? = item?.name

  override fun getQualifiedNameSeparator(): String = "."
}


class SolClassNavigationContributor
  : SolNavigationContributorBase<SolNamedElement>(SolGotoClassIndex.KEY, SolNamedElement::class.java)
