package me.serce.solidity.lang.core.resolve

class SolStructResolveTest : SolResolveTestBase() {
  fun testStructResolve() = checkByCode("""
      contract B {
          struct Prop {
                //x
              uint8 prop;
          }

          Prop[] aa;
          //^
      }
  """)
}
