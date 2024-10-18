package me.serce.solidity.ide.usage

class SolContractUsageTest : SolUsageTestBase() {
  fun testFindInheritance() = doTest(
    """
      contract A {}
             //^
      contract B is A {}
      contract C is A {}
  """, 2
  )

  fun testFields() = doTest(
    """
      contract A {}
             //^
      contract B {
          A field1;
          A field2;
      }
      contract C {
          A field1;
      }
    """, 3
  )
}
