package me.serce.solidity.lang.resolve.ref

import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.dataformat.toml.TomlMapper
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class SolImportConfigService(val project: Project) {

  private data class FoundryImportConfig(
    val remappings: List<Pair<String, String>>,
    val reverseRemappings: Map<String, String>,
  )

  private data class CachedConfig(
    val remappingsStamp: Long?,
    val foundryStamp: Long?,
    val config: FoundryImportConfig,
  )

  private val rootToConfig = ConcurrentHashMap<String, CachedConfig>()
  private val emptyConfig = FoundryImportConfig(emptyList(), emptyMap())
  private val tomlMapper by lazy { TomlMapper() }

  fun resolve(path: String, fromFile: VirtualFile): VirtualFile? {
    var current: VirtualFile? = when {
        fromFile.isDirectory -> fromFile
        else -> fromFile.parent
    }
    while (current != null) {
      val config = when {
        hasFoundryConfig(current) -> getOrLoadConfig(current)
        else -> emptyConfig
      }

      val remappedPath = applyRemappings(config.remappings, path)
      val remappedFile = current.findFileByRelativePath(remappedPath)
      if (remappedFile != null) {
        return remappedFile
      }

      val fallbackFile = foundryDefaultFallback(current, path)
      if (fallbackFile != null) {
        return fallbackFile
      }

      current = current.parent
    }
    return null
  }

  fun reverseRemappings(fromFile: VirtualFile): Map<String, String> {
    val fromDir = when {
      fromFile.isDirectory -> fromFile
      else -> fromFile.parent ?: return emptyMap()
    }
    val foundryRoot = findFoundryRoot(fromDir)
    if (foundryRoot == null) {
      return emptyMap()
    }
    return getOrLoadConfig(foundryRoot).reverseRemappings
  }

  private fun getOrLoadConfig(foundryRoot: VirtualFile): FoundryImportConfig {
    val remappingsFile = foundryRoot.findChild("remappings.txt")
    val foundryFile = foundryRoot.findChild("foundry.toml")
    val remappingsStamp = remappingsFile?.modificationStamp
    val foundryStamp = foundryFile?.modificationStamp
    val rootPath = foundryRoot.path

    val cached = rootToConfig[rootPath]
    if (cached != null && cached.remappingsStamp == remappingsStamp && cached.foundryStamp == foundryStamp) {
      return cached.config
    }

    val updated = rootToConfig.compute(rootPath) { _, guarded ->
      if (guarded != null && guarded.remappingsStamp == remappingsStamp && guarded.foundryStamp == foundryStamp) {
        return@compute guarded
      }
      val remappings = ArrayList<Pair<String, String>>()
      remappings += parseRemappingsFile(remappingsFile)
      remappings += remappingsFromFoundryConfigFile(foundryFile)
      CachedConfig(
        remappingsStamp = remappingsStamp,
        foundryStamp = foundryStamp,
        config = FoundryImportConfig(
          remappings = remappings,
          reverseRemappings = buildReverseRemappings(remappings),
        ),
      )
    } ?: return emptyConfig
    return updated.config
  }

  private fun findFoundryRoot(fromDir: VirtualFile): VirtualFile? {
    var current: VirtualFile? = fromDir
    while (current != null) {
      if (hasFoundryConfig(current)) {
        return current
      }
      current = current.parent
    }
    return null
  }

  private fun hasFoundryConfig(dir: VirtualFile): Boolean {
    return dir.findChild("remappings.txt") != null || dir.findChild("foundry.toml") != null
  }

  private fun parseRemappingsFile(file: VirtualFile?): List<Pair<String, String>> {
    if (file == null) {
      return emptyList()
    }
    val mappingsContents = runCatching {
      file.contentsToByteArray().toString(Charsets.UTF_8).split(newlineRegex)
    }.getOrElse {
      return emptyList()
    }
    return mappingsContents.mapNotNull { mapping ->
      val splitMapping = mapping.split("=", limit = 2)
      if (splitMapping.size == 2) {
        Pair(splitMapping[0].trim(), splitMapping[1].trim())
      } else {
        null
      }
    }
  }

  private fun remappingsFromFoundryConfigFile(file: VirtualFile?): List<Pair<String, String>> {
    if (file == null) {
      return emptyList()
    }
    val data = runCatching {
      tomlMapper.readTree(file.readText())
    }.getOrNull()
    val remappings = data?.get("profile")?.get("default")?.get("remappings")
    if (remappings == null) {
      return emptyList()
    }
    return remappings
      .filterIsInstance<TextNode>()
      .map {
        // each is e.g. "forge-std/=lib/forge-std/src/"
        it.textValue().trim('"').split("=", limit = 2)
      }
      .filter { it.size == 2 }
      .map {
        val first = it[0].trim()
        val secondRaw = it[1].trim()
        // normalize target for import path concatenation
        val second = if (secondRaw.endsWith("/")) {
          secondRaw
        } else {
          "$secondRaw/"
        }
        first to second
      }
  }

  // apply foundry remappings to import path
  private fun applyRemappings(remappings: List<Pair<String, String>>, path: String): String {
    val match = remappings
      .withIndex()
      .filter { (_, mapping) -> path.startsWith(mapping.first) }
      .maxWithOrNull(
        compareBy<IndexedValue<Pair<String, String>>> { it.value.first.length }
          .thenBy { it.index } // Solidity remapping ties are resolved by last-specified mapping.
      )
      ?.value
      ?: return path
    val (prefix, target) = match
    return target + path.removePrefix(prefix)
  }

  private fun buildReverseRemappings(remappings: List<Pair<String, String>>): Map<String, String> {
    val reverse = linkedMapOf<String, String>()
    remappings.forEach { (prefix, target) ->
      val trimmedPrefix = prefix.trim()
      val trimmedTarget = target.trim()
      if (trimmedPrefix.isNotEmpty() && trimmedTarget.isNotEmpty()) {
        reverse.putIfAbsent(trimmedTarget, trimmedPrefix)
        reverse.putIfAbsent(trimmedTarget.trimEnd('/'), trimmedPrefix)
      }
    }
    return reverse
  }

  // default lib located at: forge-std/Test.sol => lib/forge-std/src/Test.sol
  private fun foundryDefaultFallback(foundryRoot: VirtualFile, path: String): VirtualFile? {
    val segments = path.replace("\\", "/").split("/").filter { it.isNotEmpty() }
    if (segments.size < 2) {
      return null
    }
    val libName = segments.first()
    val libFile = segments.drop(1).joinToString("/")
    return foundryRoot.findFileByRelativePath("lib/$libName/src/$libFile")
  }

  companion object {
    private val newlineRegex = "[\r\n]+".toRegex()

    @JvmStatic
    fun getInstance(project: Project): SolImportConfigService = project.service()
  }
}
