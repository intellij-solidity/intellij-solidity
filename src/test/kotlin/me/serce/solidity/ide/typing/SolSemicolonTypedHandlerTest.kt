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

  fun testInsertBeforeLineCommentOnSameLine() = checkByText(
    """
      contract C {
        function b() public {
          a(1, 2<caret>) // trailing
        }
      }
    """.trimIndent(),
    """
      contract C {
        function b() public {
          a(1, 2);<caret> // trailing
        }
      }
    """.trimIndent()
  ) {
    myFixture.type(';')
  }

  fun testDoNotMoveWhenSemicolonAlreadyPresentSameLine() = checkByText(
    """
      contract C {
        function b() public {
          a(1, 2<caret>); // already there
        }
      }
    """.trimIndent(),
    """
      contract C {
        function b() public {
          a(1, 2;<caret>); // already there
        }
      }
    """.trimIndent()
  ) {
    myFixture.type(';')
  }

  fun testDoNotMoveWhenSemicolonOnNextLine() = checkByText(
    """
      contract C {
        function b() public {
          a(
            1,
            2<caret>
          )
          ;
        }
      }
    """.trimIndent(),
    """
      contract C {
        function b() public {
          a(
            1,
            2;<caret>
          )
          ;
        }
      }
    """.trimIndent()
  ) {
    myFixture.type(';')
  }

  fun testDoNotMoveWhenWhenSemicolonAndBlockCommentSameLine() = checkByText(
    """
      contract C {
        function b() public {
          a(1, 2<caret>) /* note */ ;
        }
      }
    """.trimIndent(),
    """
      contract C {
        function b() public {
          a(1, 2;<caret>) /* note */ ;
        }
      }
    """.trimIndent()
  ) {
    myFixture.type(';')
  }

  fun testSkipBlockCommentBetweenCaretAndParenAcrossLines() = checkByText(
    """
      contract C {
        function b() public {
          a(
            1,
            2<caret>
          /* mid */
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
          /* mid */
          );<caret>
        }
      }
    """.trimIndent()
  ) {
    myFixture.type(';')
  }

  fun testNonParenSignificantTokenAhead_DefaultInsert() = checkByText(
    """
      contract C {
        function b() public {
          a(1<caret> , 2) // comma ahead first -> default
        }
      }
    """.trimIndent(),
    """
      contract C {
        function b() public {
          a(1;<caret> , 2) // comma ahead first -> default
        }
      }
    """.trimIndent()
  ) {
    myFixture.type(';')
  }
}
