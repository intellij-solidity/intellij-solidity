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

  private fun runOptimizer(fullOptimization: Boolean) {
    val optimizer = SolImportOptimizer()
    WriteCommandAction.runWriteCommandAction(project) {
      optimizer.processFile(fixture.file, fullOptimization).run()
    }
  }
}
