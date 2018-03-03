package me.serce.solidity.lang.core.resolve

class SolMemberAccessResolveTest : SolResolveTestBase() {
  fun testResolveStructMember() = checkByCode("""
      contract B {
          struct Prop {
              uint8 prop;
                   //x
          }

          Prop[] aa;

          function B() {
              aa[0].prop;
                   //^
          }
      }
  """)

  fun testResolveContractMember() = checkByCode("""
      contract C {
          int public prop;
                    //x
      }

      contract B {
          function B(C c) {
              c.prop;
              //^
          }
      }
  """)

  fun testResolveContractParent() = checkByCode("""
      contract C {
          int public prop;
                    //x
      }

      contract K {
          int public noprop;
      }

      contract D is K, C {
          int public neigherprop;
      }

      contract B {
          function B(C c) {
              c.prop;
               //^
          }
      }
  """)
}
