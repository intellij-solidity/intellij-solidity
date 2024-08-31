package me.serce.solidity.lang.core.resolve

class SolAliasResolveTest : SolResolveTestBase() {
    fun testResolveImportedFunctionFromBracketWithAlias() = testResolveBetweenFiles(
        InlineFile(
            code = """
          contract a {
            function doit() {
                     //x
            }
          }
      """,
            name = "a.sol"
        ),
        InlineFile(
            """
          import {a as A} from "./a.sol";
              
          contract b {
            function test() {
                A.doit();
                //^
            }
          }
                      
    """
        )
    )

    fun testResolveContractFromBracketWithAlias() = testResolveBetweenFiles(
        InlineFile(
            code = """
          contract a {
                 //x
            function doit() {
            }
          }
      """,
            name = "a.sol"
        ),
        InlineFile(
            """
          import {a as A} from "./a.sol";
                     
          contract b {
            function test() {
                A.doit();
              //^
            }
          }
    """
        )
    )

    fun testResolveImportedFunctionFromPathWithAlias() = testResolveBetweenFiles(
        InlineFile(
            code = """
          contract a {
            function doit() {
                     //x
            }
          }
      """,
            name = "a.sol"
        ),
        InlineFile(
            """
          import "./a.sol" as A;
              
          contract b {
            function test() {
                A.doit();
                //^
            }
          }
                      
    """
        )
    )

    fun testResolveContractFromPathWithAlias() = testResolveBetweenFiles(
        InlineFile(
            code = """
          contract a {
                 //x
            function doit() {
            }
          }
      """,
            name = "a.sol"
        ),
        InlineFile(
            """
          import "./a.sol" as A;
                     
          contract b {
            function test() {
                A.doit();
              //^
            }
          }
    """
        )
    )

    fun testResolveImportedFunctionFromAsteriskWithAlias() = testResolveBetweenFiles(
        InlineFile(
            code = """
          contract a {
            function doit() {
                     //x
            }
          }
      """,
            name = "a.sol"
        ),
        InlineFile(
            """
          import a as A from "./a.sol";
              
          contract b {
            function test() {
                A.doit();
                //^
            }
          }
                      
    """
        )
    )

    fun testResolveContractFromAsteriskWithAlias() = testResolveBetweenFiles(
        InlineFile(
            code = """
          contract a {
                 //x
            function doit() {
            }
          }
      """,
            name = "a.sol"
        ),
        InlineFile(
            """
          import a as A from "./a.sol";
                     
          contract b {
            function test() {
                A.doit();
              //^
            }
          }
    """
        )
    )
}
