package me.serce.solidity.ide.typing

import me.serce.solidity.utils.SolTestBase

class SolSemicolonTypedHandlerTest : SolTestBase() {
  fun testInsertAfterParen() = checkByText(
    """
      contract C {
        function b() public {
          a(12, <caret>)
        }
      }
    """.trimIndent(),
    """
      contract C {
        function b() public {
          a(12, );<caret>
        }
      }
    """.trimIndent()
  ) {
    myFixture.type(';')
  }

  fun testInsertAfterParenSkipSpaces() = checkByText(
    """
      contract C {
        function b() public {
          a(1, 2<caret>   )
        }
      }
    """.trimIndent(),
    """
      contract C {
        function b() public {
          a(1, 2   );<caret>
        }
      }
    """.trimIndent()
  ) {
    myFixture.type(';')
  }

  fun testNoParenAhead_DefaultInsert() = checkByText(
    """
      contract C {
        function b() public {
          a(12<caret>, 3)
        }
      }
    """.trimIndent(),
    """
      contract C {
        function b() public {
          a(12;<caret>, 3)
        }
      }
    """.trimIndent()
  ) {
    myFixture.type(';')
  }

  fun testInsideStringLiteral_DefaultInsert() = checkByText(
    """
      contract C {
        function b() public {
          a("he<caret>llo")
        }
      }
    """.trimIndent(),
    """
      contract C {
        function b() public {
          a("he;<caret>llo")
        }
      }
    """.trimIndent()
  ) {
    myFixture.type(';')
  }

  fun testInsideComment_DefaultInsert() = checkByText(
    """
      contract C {
        function b() public {
          a(1, 2) // note<caret>
        }
      }
    """.trimIndent(),
    """
      contract C {
        function b() public {
          a(1, 2) // note;<caret>
        }
      }
    """.trimIndent()
  ) {
    myFixture.type(';')
  }

  fun testAcrossNewlineWhitespace() = checkByText(
    """
      contract C {
        function b() public {
          a(
            1,
            2<caret>
          )
        }
      }
    """.trimIndent(),
    """
      contract C {
        function b() public {
          a(
            1,
            2
          );<caret>
        }
      }
    """.trimIndent()
  ) {
    myFixture.type(';')
  }
}
