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
            function doit() internal onlySeller {
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

  fun testResolveMulti2() = checkByCode("""
        contract B {
            modifier onlySeller1() {
                     //x
            }

            modifier onlySeller2() {
            }

            function doit() onlySeller1 onlySeller2 {
                               //^              
            }
        }
  """)

    fun testResolveModifierToOnlyOneReference() {
        InlineFile(
            code = """
        pragma solidity ^0.8.10;

        contract Ownable {
            modifier onlyOwner() {
                _;
            }
        }
    """, name = "ownableOfLib.sol"
        )
        InlineFile(
            code = """
        pragma solidity ^0.8.10;

        import "./ownableOfLib.sol" as ownableOfLib;
        
        library someLib {
        
        }
    """, name = "someLib.sol"
        )
        testResolveBetweenFiles(
            InlineFile(
                code = """
        pragma solidity ^0.8.10;

        abstract contract Ownable {
            modifier onlyOwner() {
                        //x
                _;
            }
        }
    """, name = "ownable.sol"
            ),
            InlineFile(
                code = """
        pragma solidity ^0.8.10;
  
        import "./ownable.sol";
        import "./someLib.sol";
        
        contract main is Ownable {
            constructor(){
            }
        
            function foo() public onlyOwner {
                                  //^
            }
        }
      """, name = "main.sol"
            )
        )
    }

    fun testResolveBaseModifierImportedWithAlias() = testResolveBetweenFiles(
        InlineFile(
            code = """
        pragma solidity ^0.8.20;

        abstract contract AccessControl {
            modifier onlyRole(bytes32 role) {
                     //x
                _;
            }
        }
      """, name = "AccessControl.sol"
        ),

        InlineFile(
            code = """
        pragma solidity ^0.8.20;

        import {AccessControl} from "./AccessControl.sol";

        abstract contract AccessControlDefaultAdminRules is AccessControl {
            bytes32 public constant DEFAULT_ADMIN_ROLE = 0x00;

            function beginDefaultAdminTransfer(address newAdmin)
                public
                onlyRole(DEFAULT_ADMIN_ROLE)
                //^
            {
                newAdmin;
            }
        }
      """, name = "AccessControlDefaultAdminRules.sol"
        )
    )

  override fun checkByCode(@Language("Solidity") code: String) {
    super.checkByCodeInternal<SolModifierInvocation, SolNamedElement>(code)
  }
}
