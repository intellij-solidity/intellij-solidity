# Fix Remapping Resolution for Arbitrary Paths Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix remappings.txt path resolution so that remapping targets pointing to `node_modules/` or any arbitrary directory work correctly (issue #458).

**Architecture:** Three changes -- (1) normalize trailing slashes in `parseRemappingsFile()` to match `remappingsFromFoundryConfigFile()` behavior, (2) integrate PR #567's `preferElementsFromDirectImports()` for symbol disambiguation, (3) broaden reverse-remapping coverage in `buildImportPath()` beyond `lib/`-only paths.

**Tech Stack:** Kotlin, IntelliJ Platform SDK, JUnit (via `SolResolveTestBase` / `SolTestBase`)

---

### Task 1: Fix trailing slash normalization in `parseRemappingsFile()`

**Files:**
- Modify: `src/main/kotlin/me/serce/solidity/lang/resolve/ref/SolImportConfigService.kt:115-132`
- Test: `src/test/kotlin/me/serce/solidity/lang/core/resolve/SolImportResolveFoundryTest.kt`

**Step 1: Write the failing test**

Add a test to `SolImportResolveFoundryTest.kt` after the existing `testImportPathResolveFoundryFoundryFile` test. This test creates a `remappings.txt` with a target that has no trailing slash, pointing into `node_modules/`:

```kotlin
fun testImportPathResolveRemappingsToNodeModules() {
    myFixture.addFileToProject(
      "node_modules/@chainlink/contracts/src/Token.sol",
      "contract Token {}"
    )
    myFixture.addFileToProject(
      "remappings.txt",
      "@chainlink/contracts/=node_modules/@chainlink/contracts"
    )
    val usage = myFixture.addFileToProject(
      "contracts/ImportChainlink.sol",
      """
        import "@chainlink/contracts/src/Token.sol";
               //^
        contract ImportUsage {}
      """.trimIndent()
    )
    myFixture.configureFromExistingVirtualFile(usage.virtualFile)
    val (refElement, _) = findElementAndDataInEditor<SolImportPathElement>()
    val resolved = checkNotNull(refElement.reference.resolve()) {
      "Failed to resolve import @chainlink/contracts/src/Token.sol via remappings.txt"
    }
    assertEquals("Token.sol", resolved.containingFile.name)
  }
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "me.serce.solidity.lang.core.resolve.SolImportResolveFoundryTest.testImportPathResolveRemappingsToNodeModules"`
Expected: FAIL -- assertion fails because remapped path concatenation produces `node_modules/@chainlink/contractssrc/Token.sol` (missing slash).

**Step 3: Write the fix**

In `SolImportConfigService.kt`, modify `parseRemappingsFile()` to normalize trailing slashes on the target value. Change lines 124-131:

```kotlin
return mappingsContents.mapNotNull { mapping ->
  val splitMapping = mapping.split("=", limit = 2)
  if (splitMapping.size == 2) {
    val prefix = splitMapping[0].trim()
    val targetRaw = splitMapping[1].trim()
    val target = if (targetRaw.endsWith("/")) targetRaw else "$targetRaw/"
    Pair(prefix, target)
  } else {
    null
  }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "me.serce.solidity.lang.core.resolve.SolImportResolveFoundryTest.testImportPathResolveRemappingsToNodeModules"`
Expected: PASS

**Step 5: Run full existing remapping tests for regression**

Run: `./gradlew test --tests "me.serce.solidity.lang.core.resolve.SolImportResolveFoundryTest"`
Expected: All tests PASS (existing tests already use trailing-slash targets, so normalization is a no-op for them).

**Step 6: Commit**

```bash
git add src/main/kotlin/me/serce/solidity/lang/resolve/ref/SolImportConfigService.kt \
        src/test/kotlin/me/serce/solidity/lang/core/resolve/SolImportResolveFoundryTest.kt
git commit -m "fix: normalize trailing slash in remappings.txt parser (#458)

parseRemappingsFile() did not append trailing slashes to remapping
targets, causing path concatenation to produce broken paths like
node_modules/@chainlink/contractssrc/Token.sol.
Apply the same normalization already present in foundry.toml parsing."
```

---

### Task 2: Add test for remappings.txt with trailing slash (positive case)

**Files:**
- Test: `src/test/kotlin/me/serce/solidity/lang/core/resolve/SolImportResolveFoundryTest.kt`

**Step 1: Write the test**

Verify that a remappings.txt target WITH a trailing slash also works (regression guard):

```kotlin
fun testImportPathResolveRemappingsToNodeModulesWithTrailingSlash() {
    myFixture.addFileToProject(
      "node_modules/@chainlink/contracts/src/Token.sol",
      "contract Token {}"
    )
    myFixture.addFileToProject(
      "remappings.txt",
      "@chainlink/contracts/=node_modules/@chainlink/contracts/"
    )
    val usage = myFixture.addFileToProject(
      "contracts/ImportChainlink.sol",
      """
        import "@chainlink/contracts/src/Token.sol";
               //^
        contract ImportUsage {}
      """.trimIndent()
    )
    myFixture.configureFromExistingVirtualFile(usage.virtualFile)
    val (refElement, _) = findElementAndDataInEditor<SolImportPathElement>()
    val resolved = checkNotNull(refElement.reference.resolve()) {
      "Failed to resolve import with trailing slash in remappings.txt"
    }
    assertEquals("Token.sol", resolved.containingFile.name)
  }
```

**Step 2: Run test**

Run: `./gradlew test --tests "me.serce.solidity.lang.core.resolve.SolImportResolveFoundryTest.testImportPathResolveRemappingsToNodeModulesWithTrailingSlash"`
Expected: PASS (should already work with the fix from Task 1).

**Step 3: Commit**

```bash
git add src/test/kotlin/me/serce/solidity/lang/core/resolve/SolImportResolveFoundryTest.kt
git commit -m "test: add positive test for remapping with trailing slash"
```

---

### Task 3: Integrate PR #567 -- preferElementsFromDirectImports

**Files:**
- Modify: `src/main/kotlin/me/serce/solidity/lang/resolve/engine.kt:151-176`
- Test: `src/test/kotlin/me/serce/solidity/ide/refactoring/SolImportOptimizerTest.kt`

**Step 1: Write the failing test**

Add to `SolImportOptimizerTest.kt` (before the `runOptimizer` private method). This is the test from PR #567 that verifies optimize-imports does not add a transitive duplicate:

```kotlin
fun testOptimiseImportsDoesNotAddTransitiveDuplicateInitializable() {
    myFixture.addFileToProject(
      "openzeppelin/upgrades-core/contracts/Initializable.sol",
      """
        pragma solidity ^0.8.20;

        abstract contract Initializable {}
      """
    )
    myFixture.addFileToProject(
      "openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol",
      """
        pragma solidity ^0.8.20;

        import "../../../upgrades-core/contracts/Initializable.sol";

        abstract contract Initializable {}
      """
    )
    myFixture.addFileToProject(
      "openzeppelin/contracts-upgradeable/access/OwnableUpgradeable.sol",
      """
        pragma solidity ^0.8.20;

        abstract contract OwnableUpgradeable {}
      """
    )

    InlineFile(
      """
        pragma solidity ^0.8.20;

        import "./openzeppelin/contracts-upgradeable/access/OwnableUpgradeable.sol";
        import "./openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";

        contract MyCoin is Initializable, OwnableUpgradeable {}
      """
    )

    runOptimizer(fullOptimization = true)

    val result = fixture.file.text
    val importsCount = Regex("^\\s*import\\s+.*;$", RegexOption.MULTILINE).findAll(result).count()
    assertEquals(2, importsCount)
    assertTrue(result.contains("./openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol"))
    assertFalse(result.contains("./openzeppelin/upgrades-core/contracts/Initializable.sol"))
  }

  fun testOptimiseImportsKeepsTransitiveOnlySymbolResolvable() {
    InlineFile(
      """
        pragma solidity ^0.8.26;

        abstract contract TransitiveBase {}
      """,
      "C.sol"
    )
    InlineFile(
      """
        pragma solidity ^0.8.26;

        import "./C.sol";
      """,
      "B.sol"
    )

    checkByText(
      """
        pragma solidity ^0.8.26;

        import "./B.sol";

        contract Main is TransitiveBase {}
      """,
      """
        pragma solidity ^0.8.26;

        import {TransitiveBase} from "./C.sol";

        contract Main is TransitiveBase {}
      """
    ) {
      runOptimizer(fullOptimization = true)
    }
  }
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "me.serce.solidity.ide.refactoring.SolImportOptimizerTest.testOptimiseImportsDoesNotAddTransitiveDuplicateInitializable"`
Expected: FAIL -- 3 imports instead of 2 (transitive duplicate added).

**Step 3: Implement preferElementsFromDirectImports**

In `engine.kt`, modify `resolveFromPreviousOrStub()` (around line 169) and add the new method. Replace:

```kotlin
val elements = searchElementByStub(identifier, filesOfScope, currentFile.project)
```

With:

```kotlin
val elements = preferElementsFromDirectImports(
  searchElementByStub(identifier, filesOfScope, currentFile.project),
  currentFile
)
```

Add the new method after `resolveFromPreviousOrStub()`:

```kotlin
private fun preferElementsFromDirectImports(
  elements: Set<SolNamedElement>,
  currentFile: PsiFile
): Set<SolNamedElement> {
  if (elements.size <= 1) {
    return elements
  }

  val directImportFiles = currentFile.childrenOfType<SolImportDirective>()
    .mapNotNull { it.importPath?.reference?.resolve()?.containingFile?.virtualFile }
    .toSet()
  val directMatches = elements.filterTo(mutableSetOf()) { named ->
    named.containingFile.virtualFile?.let(directImportFiles::contains) == true
  }
  return directMatches.takeIf { it.isNotEmpty() } ?: elements
}
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "me.serce.solidity.ide.refactoring.SolImportOptimizerTest.testOptimiseImportsDoesNotAddTransitiveDuplicateInitializable" --tests "me.serce.solidity.ide.refactoring.SolImportOptimizerTest.testOptimiseImportsKeepsTransitiveOnlySymbolResolvable"`
Expected: Both PASS

**Step 5: Run full test suite for regression**

Run: `./gradlew test --tests "me.serce.solidity.ide.refactoring.SolImportOptimizerTest"`
Expected: All tests PASS

**Step 6: Commit**

```bash
git add src/main/kotlin/me/serce/solidity/lang/resolve/engine.kt \
        src/test/kotlin/me/serce/solidity/ide/refactoring/SolImportOptimizerTest.kt
git commit -m "fix: prefer direct imports over transitive when resolving symbols (#453)

When stub search returns multiple candidates with the same name,
prefer elements from files that are directly imported by the current
file. This prevents optimize-imports from adding redundant transitive
imports (e.g., deprecated Initializable from upgrades-core).

Co-authored-by: PR #567"
```

---

### Task 4: Broaden reverse remapping in `buildImportPath()`

**Files:**
- Modify: `src/main/kotlin/me/serce/solidity/ide/inspections/fixes/ImportFileAction.kt:162-188`
- Test: `src/test/kotlin/me/serce/solidity/ide/inspections/fixes/ImportFileActionTest.kt`

**Step 1: Write the failing test**

Add to `ImportFileActionTest.kt` a test for a remapping target using a custom directory (not `lib/` or `node_modules/`):

```kotlin
fun testBuildImportPathWithCustomRemappingTarget() {
    myFixture.addFileToProject(
      "remappings.txt",
      "@deps/=dependencies/@deps/"
    )
    val customDep = myFixture.addFileToProject(
      "dependencies/@deps/token/ERC20.sol",
      "contract ERC20 {}"
    )
    check("@deps/token/ERC20.sol", customDep)
  }
```

Note: the existing `check()` helper method creates a source file in `contracts/` and calls `buildImportPath`.

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "me.serce.solidity.ide.inspections.fixes.ImportFileActionTest.testBuildImportPathWithCustomRemappingTarget"`
Expected: FAIL -- produces relative path like `../dependencies/@deps/token/ERC20.sol` instead of `@deps/token/ERC20.sol`.

**Step 3: Implement the fix**

In `ImportFileAction.kt`, modify `buildImportPath()` (lines 162-188). Move the reverse-remapping check before the `node_modules` and `installed_contracts` checks, and apply it to any path:

```kotlin
fun buildImportPath(project: Project, source: VirtualFile, destination: VirtualFile): String {
  return Paths.get(source.path).parent.relativize(Paths.get(destination.path)).toString().let { importPath ->
    val separator = File.separator

    // Try reverse remappings first -- works for lib/, dependencies/, or any custom target
    val mapping = SolImportConfigService.getInstance(project).reverseRemappings(source)
    val reverseMatched = mapping.keys.firstOrNull { importPath.contains(it) }
      ?.let { importPath.substring(importPath.indexOf(it)).replaceFirst(it, mapping[it]!!) }

    when {
      reverseMatched != null -> reverseMatched

      importPath.contains("node_modules$separator") -> {
        val idx = importPath.indexOf("node_modules$separator")
        importPath.substring(idx + "node_modules$separator".length)
      }

      importPath.contains("installed_contracts$separator") -> {
        val idx = importPath.indexOf("installed_contracts$separator")
        importPath.substring(idx + "installed_contracts$separator".length)
          .replaceFirst("${separator}contracts${separator}", separator)
      }

      !importPath.startsWith(".") -> ".$separator$importPath"
      else -> importPath
    }
  }.replace("\\", "/")
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "me.serce.solidity.ide.inspections.fixes.ImportFileActionTest.testBuildImportPathWithCustomRemappingTarget"`
Expected: PASS

**Step 5: Run full ImportFileActionTest for regression**

Run: `./gradlew test --tests "me.serce.solidity.ide.inspections.fixes.ImportFileActionTest"`
Expected: All tests PASS (the existing `@oz` remapped test should still work since reverse remapping now runs first).

**Step 6: Commit**

```bash
git add src/main/kotlin/me/serce/solidity/ide/inspections/fixes/ImportFileAction.kt \
        src/test/kotlin/me/serce/solidity/ide/inspections/fixes/ImportFileActionTest.kt
git commit -m "fix: apply reverse remappings for any target directory in buildImportPath

Previously reverse remappings only applied when the path contained
lib/. Now they are checked first for any path, supporting custom
dependency directories like dependencies/, vendor/, etc."
```

---

### Task 5: Full regression test suite

**Step 1: Run the complete test suite**

Run: `./gradlew clean build check`
Expected: BUILD SUCCESSFUL, all tests pass.

**Step 2: If any failures, investigate and fix before proceeding**

**Step 3: Final commit if any fixups were needed**

---

### Task 6: Clean up and prepare PR

**Step 1: Review all changes**

Run: `git log --oneline master..HEAD` to see all commits.

**Step 2: Verify commit history is clean**

Each commit should be self-contained and pass tests independently.
