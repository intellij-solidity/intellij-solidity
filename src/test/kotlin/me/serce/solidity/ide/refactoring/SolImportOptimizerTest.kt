package me.serce.solidity.ide.refactoring

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
}
