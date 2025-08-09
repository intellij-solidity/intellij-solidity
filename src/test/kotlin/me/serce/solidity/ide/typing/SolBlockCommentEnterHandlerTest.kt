package me.serce.solidity.ide.typing

import me.serce.solidity.utils.SolTestBase

class SolBlockCommentEnterHandlerTest : SolTestBase() {
  fun testBlockCommentContinuation() = checkByText(
    "/*<caret>",
    """
      /*
       * <caret>
       */
    """.trimIndent()
  ) {
    myFixture.type('\n')
  }

  fun testBlockCommentContinuationWithDoubleStars() = checkByText(
    "/**<caret>",
    """
      /**
       * <caret>
       */
    """.trimIndent()
  ) {
    myFixture.type('\n')
  }

  fun testBlockCommentEnterWithinTheComment() = checkByText(
    """
      /*
       * <caret>
       */
    """.trimIndent(),
    """
      /*
       *
       * <caret>
       */
    """.trimIndent()
  ) {
    myFixture.type('\n')
  }

  fun testBlockCommentEnterWithinText() = checkByText(
    """
      /*
       * foo<caret>
       */
    """.trimIndent(),
    """
      /*
       * foo
       * <caret>
       */
    """.trimIndent()
  ) {
    myFixture.type('\n')
  }

  fun testBlockCommentContinuationWithIndent() = checkByText(
    """
      contract C {
        /*<caret>
      }
    """.trimIndent(),
    """
      contract C {
        /*
         * <caret>
         */
      }
    """.trimIndent()
  ) {
    myFixture.type('\n')
  }
}
