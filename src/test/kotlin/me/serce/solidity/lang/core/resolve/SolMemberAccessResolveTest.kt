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
}
