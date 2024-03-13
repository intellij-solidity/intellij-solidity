package me.serce.solidity.lang.completion

class SolMemberAccessCompletionTest : SolCompletionTestBase() {

  fun testEmptyCompletion() = checkCompletion(hashSetOf("doSmth"), """
        contract SomeContract {
            function doSmth() public {
            
            }        
        }
        
        contract Test {
            function doSmth(SomeContract c) {
                c./*caret*/
            }
        }
  """)

  fun testFunctionCompletion() = checkCompletion(hashSetOf("doSmth3"), """
        contract SomeContract {
            function doSmth() public {
            
            }

            function doSmth3() public {
            
            } 
        }
        
        contract Test {
            function doSmth(SomeContract c) {
                c.doS/*caret*/
            }
        }
  """)

  fun testPublicStateVarCompletion() = checkCompletion(hashSetOf("smth", "smth2"), """
        contract SomeContract {
            uint public smth;
            uint public smth2;
        }
        
        contract Test {
            function doSmth(SomeContract c) {
                c.sm/*caret*/
            }
        }
  """, strict = true)

  fun testOnlyPublicStateVarCompletion() = checkCompletion(hashSetOf("smthPublic1", "smthPublic2"), """
        contract SomeContract {
            uint smth;
            uint public smthPublic1;
            uint public smthPublic2;
        }
        
        contract Test {
            function doSmth(SomeContract c) {
                c.sm/*caret*/
            }
        }
  """, strict = true)

  fun testOnlyPublicFunctionCompletion() = checkCompletion(hashSetOf("doSmth1", "doSmth2"), """
        contract SomeContract {
            function doSmthInternal() internal {
            
            }

            function doSmthPrivate() private {
            
            }

            function doSmth1() public {
            
            }
                    
            function doSmth2() public {
            
            }         
        }
        
        contract Test {
            function doFunc(SomeContract c) {
                c.doS/*caret*/
            }
        }
  """, strict = true)

  fun testOverridesOnlyOnce() {
    InlineFile("""
        contract BaseContract {
            function doSmth1() public {
            
            } 
        }
        contract SomeContract is BaseContract {
            function doSmth1() public {
            
            }
                    
            function doSmth2() public {
            
            }         
        }
        
        contract Test {
            function doFunc(SomeContract c) {
                c.doS/*caret*/
            }
        }
  """).withCaret()
    val variants = myFixture.completeBasic()
      .map { it.lookupString }
      .groupBy { it }
      .mapValues { it.value.size }
    assertEquals(variants["doSmth1"], 1)
  }

  fun testWrapUserDefinedType() = checkCompletion(hashSetOf("wrap", "unwrap"), """
        contract B {
          type ShortString is bytes32;
        
          function a()  {
            ShortString./*caret*/;
          }
        }
        
  """, strict = true)

}
