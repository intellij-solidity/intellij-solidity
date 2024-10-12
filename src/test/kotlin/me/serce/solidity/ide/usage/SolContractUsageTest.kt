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

  fun testFields() = multipleResolveTest(
    """
      contract A {}
             //^
      contract B {
          A field1;
        //x
          A field2;
        //x
      }
      contract C {
          A field1;
        //x
      }
    """
  )
}
