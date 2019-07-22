package me.serce.solidity.lang.core.resolve

import me.serce.solidity.lang.psi.SolModifierInvocation
import me.serce.solidity.lang.psi.SolNamedElement
import org.intellij.lang.annotations.Language

class SolModifierResolveTest : SolResolveTestBase() {
  fun testResolveModifier() = checkByCode("""
        contract B {
            modifier onlySeller() {
                     //x
                _;
            }

            function doit() internal onlySeller constant {
                                      //^
            }
        }
  """)

  fun testResolveConstructorModifier() = checkByCode("""
        contract B {
            modifier onlySeller() {
                     //x
                _;
            }

            constructor() public onlySeller constant {
                                      //^
            }
        }
  """)

  fun testResolveModifierAnother() = checkByCode("""
        contract A {
            modifier onlySeller() {
                _;
            }
        }

        contract B {
            modifier onlySeller() {
                     //x
                _;
            }
            function doit() internal onlySeller constant {
                                      //^
            }
        }
  """)

  fun testResolveMulti() = checkByCode("""
        contract B {
            modifier onlySeller1() {
            }

            modifier onlySeller2() {
                     //x
            }

            function doit() onlySeller1 onlySeller2 {
                                             //^
            }
        }
  """)

  override fun checkByCode(@Language("Solidity") code: String) {
    super.checkByCodeInternal<SolModifierInvocation, SolNamedElement>(code)
  }
}
