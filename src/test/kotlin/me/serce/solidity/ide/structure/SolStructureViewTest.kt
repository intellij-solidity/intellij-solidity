package me.serce.solidity.ide.structure

import me.serce.solidity.ide.SolStructureViewModel
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.lang.psi.SolNamedElement
import me.serce.solidity.utils.SolTestBase

class SolStructureViewTest : SolTestBase() {
  fun testFileLevelEnumIsVisible() {
    val file = InlineFile(
      """
        enum TestEnum {A, B}
        contract C {}
      """.trimIndent()
    )
    val model = SolStructureViewModel(myFixture.editor, file.psiFile as SolidityFile)
    val root = model.root
    val names = root.children.mapNotNull { it?.value }
      .filterIsInstance<SolNamedElement>()
      .map { it.name }
    assertEquals(listOf("TestEnum", "C"), names)
  }
}
