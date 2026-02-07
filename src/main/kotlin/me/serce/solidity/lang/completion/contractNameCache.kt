package me.serce.solidity.lang.completion

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.stubs.StubIndex
import me.serce.solidity.lang.SolidityLanguage
import me.serce.solidity.lang.stubs.SolGotoClassIndex

@Service(Service.Level.PROJECT)
class SolContractNamesCache(private val project: Project) {
  // Completion asks for contract names often, so caching avoids repeated getAllKeys() 
  // which is relatively expensive, even though it's from an index
  private val namesCache: CachedValue<Set<String>> = CachedValuesManager.getManager(project).createCachedValue({
    val names = StubIndex.getInstance()
      .getAllKeys(SolGotoClassIndex.KEY, project)
      .toSet()
    CachedValueProvider.Result.create(
      names,
      ProjectRootModificationTracker.getInstance(project),
      PsiModificationTracker.getInstance(project).forLanguage(SolidityLanguage)
    )
  }, false)

  fun allNames(): Set<String> = namesCache.value

  companion object {
    fun getInstance(project: Project): SolContractNamesCache = project.service()
  }
}
