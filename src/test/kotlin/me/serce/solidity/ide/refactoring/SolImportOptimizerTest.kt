package me.serce.solidity.ide.refactoring

import com.intellij.openapi.command.WriteCommandAction
import me.serce.solidity.ide.formatting.SolImportOptimizer
import me.serce.solidity.utils.SolTestBase

class SolImportOptimizerTest : SolTestBase() {
  fun testOptimiseImports() {
    InlineFile("contract A {}", "A.sol")
    InlineFile("contract B {}", "B.sol")

    checkEditorAction(
      """
          pragma solidity ^0.8.26;

          import {A} from "./A.sol";
          import {B} from "./B.sol";

          contract C is B {
          }
      """, //
      """
          pragma solidity ^0.8.26;

          import {B} from "./B.sol";

          contract C is B {
          }
      """, "OptimizeImports"
    )
  }

  fun testOptimiseImportsWithoutImports() {
    checkEditorAction(
      """
          pragma solidity ^0.8.26;

          contract C {
            function a() external {}
          }
      """, //
      """
          pragma solidity ^0.8.26;

          contract C {
            function a() external {}
          }
      """, "OptimizeImports"
    )
  }

  fun testLightOptimiseImportsSortsImports() {
    InlineFile("contract A {}", "A.sol")
    InlineFile("contract B {}", "B.sol")

    checkByText(
      """
          pragma solidity ^0.8.26;

          import {B} from "./B.sol";
          import {A} from "./A.sol";

          contract C is B {
          }
      """,
      """
          pragma solidity ^0.8.26;

          import {A} from "./A.sol";
          import {B} from "./B.sol";

          contract C is B {
          }
      """
    ) {
      runOptimizer(fullOptimization = false)
    }
  }

  fun testLightOptimiseImportsNoOpOnSingleImport() {
    InlineFile("contract A {}", "A.sol")

    checkByText(
      """
          pragma solidity ^0.8.26;

          import {A} from "./A.sol";

          contract C is A {
          }
      """,
      """
          pragma solidity ^0.8.26;

          import {A} from "./A.sol";

          contract C is A {
          }
      """
    ) {
      runOptimizer(fullOptimization = false)
    }
  }

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

  private fun runOptimizer(fullOptimization: Boolean) {
    val optimizer = SolImportOptimizer()
    WriteCommandAction.runWriteCommandAction(project) {
      optimizer.processFile(fixture.file, fullOptimization).run()
    }
  }
}
