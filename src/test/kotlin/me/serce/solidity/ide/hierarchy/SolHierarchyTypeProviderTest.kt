package me.serce.solidity.ide.hierarchy

import com.intellij.testFramework.codeInsight.hierarchy.HierarchyViewTestFixture
import me.serce.solidity.ide.SupertypesHierarchyTreeStructure
import me.serce.solidity.ide.SubtypesHierarchyTreeStructure
import me.serce.solidity.ide.TypeHierarchyTreeStructure
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.utils.SolTestBase
import com.intellij.psi.util.PsiTreeUtil

class SolHierarchyTypeProviderTest : SolTestBase() {
  fun testSupertypesHierarchy() {
    val file = InlineFile(
      """
      contract A {}
      contract B is A {}
      contract C is B {}
      """.trimIndent()
    )
    val contracts = PsiTreeUtil.findChildrenOfType(file.psiFile, SolContractDefinition::class.java)
    val contractC = contracts.first { it.name == "C" }
    val structure = SupertypesHierarchyTreeStructure(contractC)
    HierarchyViewTestFixture.doHierarchyTest(
      structure,
      """
      <node text="C" base="true">
        <node text="B">
          <node text="A"/>
        </node>
        <node text="A"/>
      </node>
      """.trimIndent()
    )
  }

  fun testSubtypesHierarchy() {
    val file = InlineFile(
      """
      contract A {}
      contract B is A {}
      contract C is B {}
      """.trimIndent()
    )
    val contracts = PsiTreeUtil.findChildrenOfType(file.psiFile, SolContractDefinition::class.java)
    val contractA = contracts.first { it.name == "A" }
    val structure = SubtypesHierarchyTreeStructure(contractA, "")
    HierarchyViewTestFixture.doHierarchyTest(
      structure,
      """
      <node text="A" base="true">
        <node text="C"/>
        <node text="B">
          <node text="C"/>
        </node>
      </node>
      """.trimIndent()
    )
  }

  fun testTypeHierarchy() {
    val file = InlineFile(
      """
      contract A {}
      contract B is A {}
      contract C is B {}
      """.trimIndent()
    )
    val contracts = PsiTreeUtil.findChildrenOfType(file.psiFile, SolContractDefinition::class.java)
    val contractC = contracts.first { it.name == "C" }
    val structure = TypeHierarchyTreeStructure(contractC, "")
    HierarchyViewTestFixture.doHierarchyTest(
      structure,
      """
      <node text="A">
        <node text="B">
          <node text="C" base="true"/>
        </node>
      </node>
      """.trimIndent()
    )
  }
}

