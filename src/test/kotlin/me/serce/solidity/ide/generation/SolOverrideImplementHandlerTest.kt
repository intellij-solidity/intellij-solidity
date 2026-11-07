package me.serce.solidity.ide.generation

import me.serce.solidity.lang.psi.SolNamedElement
import me.serce.solidity.utils.SolTestBase
import me.serce.solidity.settings.FormatterType
import me.serce.solidity.settings.SoliditySettings
import org.intellij.lang.annotations.Language

class SolOverrideImplementHandlerTest : SolTestBase() {

  private fun doTest(handler: SolOverrideImplementHandler, @Language("Solidity") before: String, @Language("Solidity") after: String) {
    myFixture.configureByText("main.sol", before.trimIndent())
    assertTrue("handler should be available", handler.isValidFor(myFixture.editor, myFixture.file))
    handler.invoke(project, myFixture.editor, myFixture.file)
    // plain string comparison so a failure report contains both texts
    assertEquals(after.trimIndent(), myFixture.editor.document.text)
  }

  private fun candidateNames(handler: SolOverrideImplementHandler, @Language("Solidity") code: String): List<String?> {
    myFixture.configureByText("main.sol", code.trimIndent())
    val contract = handler.contractAt(myFixture.editor, myFixture.file) ?: return emptyList()
    return handler.candidates(contract).map(SolNamedElement::getName)
  }

  fun testOverrideStoreGeneratesSuperCall() = doTest(
    SolOverrideMethodsHandler(),
    """
    contract SimpleStorage {
        function store(uint256 _x) public virtual {}
    }
    contract AddFiveStorage is SimpleStorage {
        <caret>
    }
    """,
    """
    contract SimpleStorage {
        function store(uint256 _x) public virtual {}
    }
    contract AddFiveStorage is SimpleStorage {
        function store(uint256 _x) public override {
            super.store(_x);
        }
    }
    """
  )

  fun testReturnsUsesReturn() = doTest(
    SolOverrideMethodsHandler(),
    """
    contract A {
        function get() public view virtual returns (uint256) { return 1; }
    }
    contract B is A {
        <caret>
    }
    """,
    """
    contract A {
        function get() public view virtual returns (uint256) { return 1; }
    }
    contract B is A {
        function get() public view override returns (uint256) {
            return super.get();
        }
    }
    """
  )

  fun testExternalHasEmptyBody() = doTest(
    SolOverrideMethodsHandler(),
    """
    contract A {
        function f(uint x) external virtual {}
    }
    contract B is A {
        <caret>
    }
    """,
    """
    contract A {
        function f(uint x) external virtual {}
    }
    contract B is A {
        function f(uint x) external override {
        }
    }
    """
  )

  fun testImplementInterface() = doTest(
    SolImplementMethodsHandler(),
    """
    interface I {
        function f() external;
    }
    contract C is I {
        <caret>
    }
    """,
    """
    interface I {
        function f() external;
    }
    contract C is I {
        function f() external override {
        }
    }
    """
  )

  fun testMultipleBasesOverrideList() = doTest(
    SolOverrideMethodsHandler(),
    """
    contract A {
        function f() public virtual {}
    }
    contract B {
        function f() public virtual {}
    }
    contract C is A, B {
        <caret>
    }
    """,
    """
    contract A {
        function f() public virtual {}
    }
    contract B {
        function f() public virtual {}
    }
    contract C is A, B {
        function f() public override(B, A) {
            super.f();
        }
    }
    """
  )

  fun testModifierStub() = doTest(
    SolOverrideMethodsHandler(),
    """
    contract A {
        modifier m(uint x) virtual { _; }
    }
    contract B is A {
        <caret>
    }
    """,
    """
    contract A {
        modifier m(uint x) virtual { _; }
    }
    contract B is A {
        modifier m(uint x) override {
            _;
        }
    }
    """
  )

  fun testAlreadyOverriddenNotOffered() {
    val names = candidateNames(
      SolOverrideMethodsHandler(),
      """
      contract A {
          function f() public virtual {}
          function g() public virtual {}
      }
      contract B is A {
          function f() public override {}
          <caret>
      }
      """
    )
    assertEquals(listOf("g"), names)
  }

  fun testNonVirtualNotOffered() {
    val names = candidateNames(
      SolOverrideMethodsHandler(),
      """
      contract A {
          function f() public {}
          function g() public virtual {}
          function h() private {}
      }
      contract B is A {
          <caret>
      }
      """
    )
    assertEquals(listOf("g"), names)
  }

  fun testImplementSkipsImplemented() {
    val names = candidateNames(
      SolImplementMethodsHandler(),
      """
      interface I {
          function f() external;
          function g() external;
      }
      abstract contract A is I {
          function f() external virtual override {}
      }
      contract B is A {
          <caret>
      }
      """
    )
    assertEquals(listOf("g"), names)
  }

  fun testNotAvailableOutsideContract() {
    myFixture.configureByText(
      "main.sol",
      """
      contract A { function f() public virtual {} }
      <caret>
      contract B is A {}
      """.trimIndent()
    )
    assertFalse(SolOverrideMethodsHandler().isValidFor(myFixture.editor, myFixture.file))
  }

  fun testIndentedWithoutIdeFormatter() {
    val settings = SoliditySettings.getInstance(project)
    val old = settings.formatterType
    settings.formatterType = FormatterType.DISABLED
    try {
      doTest(
        SolOverrideMethodsHandler(),
        """
        contract A {
            function store(uint256 _x) public virtual {}
        }
        contract B is A {
            <caret>
        }
        """,
        """
        contract A {
            function store(uint256 _x) public virtual {}
        }
        contract B is A {
            function store(uint256 _x) public override {
                super.store(_x);
            }
        }
        """
      )
    } finally {
      settings.formatterType = old
    }
  }

  // Ctrl+O must react (with a hint) when bases exist but nothing is virtual, instead of silently doing nothing
  fun testAvailableWhenNothingToOverride() {
    val before = """
      contract A {
          function f() public {}
      }
      contract B is A {
          <caret>
      }
      """.trimIndent()
    myFixture.configureByText("main.sol", before)
    val handler = SolOverrideMethodsHandler()
    assertTrue(handler.isValidFor(myFixture.editor, myFixture.file))
    handler.invoke(project, myFixture.editor, myFixture.file)
    assertEquals(before.replace("<caret>", ""), myFixture.editor.document.text)
  }

  fun testNotAvailableWithoutBases() {
    myFixture.configureByText("main.sol", "contract A {\n    <caret>\n}")
    assertFalse(SolOverrideMethodsHandler().isValidFor(myFixture.editor, myFixture.file))
  }
}
