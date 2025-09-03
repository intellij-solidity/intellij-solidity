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

  fun testResolveContractMemberWhenFunction() = checkByCode("""
      contract C {
          int public prop;
                    //x
                    
          function prop(uint value) public {
          
          }
      }

      contract B {
          function B(C c) {
              c.prop();
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

  fun testResolveInsideFunctionCall() = checkByCode("""
      contract C {
          bool public def;
                     //x 
      }

      contract test {
          function test(C c){
              require(c.def, "");
                       //^
          }
      }
  """)

  fun testResolveFunctionInUsingFor() = checkByCode("""
    pragma solidity ^0.8.26;
    
    type Foo is uint256;

    library FooLib {
        function isHappy(Foo f) internal pure returns(bool) {
                    //x   
            return Foo.unwrap(f) > 100;
        }
    }
    
    using {
        FooLib.isHappy
                //^
    } for Foo global;
     """)
}
