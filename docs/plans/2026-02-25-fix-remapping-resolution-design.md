# Fix Remapping Resolution for Arbitrary Paths (Issue #458)

## Problem

When `remappings.txt` maps an import prefix to a path under `node_modules/` (or any non-`lib/` directory), the plugin fails to resolve the file. Example:

```
@chainlink/contracts/=node_modules/@chainlink/contracts
```

Importing `@chainlink/contracts/src/X.sol` should resolve to `node_modules/@chainlink/contracts/src/X.sol`, but it doesn't.

## Root Cause

`parseRemappingsFile()` in `SolImportConfigService.kt` does not normalize trailing slashes on the remapping target. `remappingsFromFoundryConfigFile()` does (lines 156-160), but `parseRemappingsFile()` does not.

Given `@chainlink/contracts/=node_modules/@chainlink/contracts` in `remappings.txt`:
- prefix = `@chainlink/contracts/`
- target = `node_modules/@chainlink/contracts` (no trailing slash)

`applyRemappings()` produces: `"node_modules/@chainlink/contracts" + "src/X.sol"` = `"node_modules/@chainlink/contractssrc/X.sol"` -- the slash between target and remainder is missing.

## Changes

### Change 1: Normalize trailing slash in `parseRemappingsFile()`

**File:** `SolImportConfigService.kt`, `parseRemappingsFile()` method

Apply the same trailing-slash normalization that `remappingsFromFoundryConfigFile()` already does. When a remapping target does not end with `/`, append one. This ensures path concatenation works correctly regardless of the source format.

### Change 2: Integrate PR #567's `preferElementsFromDirectImports`

**File:** `engine.kt`, `resolveFromPreviousOrStub()` method

PR #567 adds logic to prefer symbols from directly-imported files when stub search returns multiple candidates with the same name. With #458 fixed, `node_modules` and `lib` can both provide files for the same contract name, making disambiguation essential. Include the `preferElementsFromDirectImports()` function from PR #567.

### Change 3: Improve `buildImportPath()` reverse remapping coverage

**File:** `ImportFileAction.kt`, `buildImportPath()` method

Currently, reverse remapping is only attempted when the path contains `lib/`. Change this to attempt reverse remapping for any path that doesn't match the `node_modules/` or `installed_contracts/` patterns, so that arbitrary remapping targets (e.g., `dependencies/`, `custom_libs/`) produce correct import paths.

## Testing

- Add test fixtures for `remappings.txt` with `node_modules/` targets (with and without trailing slash)
- Add test for `buildImportPath()` with non-standard remapping targets
- Verify existing remapping tests still pass (regression)

## Relationship to PR #567

PR #567 fixes issue #453 (duplicate deprecated imports from optimize-imports). The symbol disambiguation logic in PR #567 becomes more important once #458 is fixed, because resolving imports from both `node_modules` and `lib` increases the chance of same-name symbol collisions. This design incorporates PR #567's core change.
