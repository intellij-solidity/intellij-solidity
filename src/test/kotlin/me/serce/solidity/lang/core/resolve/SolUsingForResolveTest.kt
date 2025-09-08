package me.serce.solidity.lang.core.resolve

class SolUsingForResolveTest : SolResolveTestBase() {
  fun testResolveContractInUsingFor() = checkByCode("""
    pragma solidity ^0.8.26;
    
    type Foo is uint256;

    contract FooLib {
              //x   
        function isHappy(Foo f) internal pure returns(bool) {
            return Foo.unwrap(f) > 100;
        }
    }
    
    using {
        FooLib.isHappy
          //^
    } for Foo global;
     """)

  fun testResolveLibraryInUsingFor() = checkByCode("""
    pragma solidity ^0.8.26;
    
    type Foo is uint256;

    library FooLib {
              //x   
        function isHappy(Foo f) internal pure returns(bool) {
            return Foo.unwrap(f) > 100;
        }
    }
    
    using {
        FooLib.isHappy
          //^
    } for Foo global;
     """)

  fun testResolveFunctionInUsingForAsOperator() = checkByCode(
    """
   pragma solidity ^0.8.10;

    type Foo is uint256;
    
    function isHappy(Foo a, Foo b) pure returns (Foo) {
               //x
        return a + b;
    }
    
    using {isHappy as +} for Foo global;
            //^
     """
  )

  fun testResolveFunctionInUsingForStruct() = checkByCode(
    """
   pragma solidity ^0.8.26;

    struct S { uint256 x; }
         //x
    
    library L {
        function inc(S storage s) internal returns (uint256) {
            s.x += 1;
            return s.x;
        }
    }
    
    using { L.inc } for S;
                      //^
     """
  )
  fun testResolveFunctionInUsingForArray() = checkByCode(
    """
   pragma solidity ^0.8.26;

    library L {
        function len(uint256[] storage a) internal view returns (uint256) {
                 //x
            return a.length;
        }
    }
    
    using { L.len } for uint256[];
             //^
     """
  )
  fun testResolveFunctionInUsingForFunction() = checkByCode(
    """
   pragma solidity ^0.8.26;

    library L {
        function foo(function(uint256) pure returns (uint256) f, uint256 x) internal pure returns (uint256) {
                //x
            return f(x);
        }
    }

    using { L.foo } for function(uint256) pure returns (uint256);
            //^

     """
  )
  fun testResolveMultipleFunctionInUsingFor() = checkByCode(
    """
  pragma solidity ^0.8.26;

    library L {
      function f1(uint256 a) internal pure returns (uint256) {
              //x
        return a;
      }
      function f2(uint256 a) internal pure returns (uint256) {
        return a + 1;
      }
    }

    using { L.f1, L.f2 } for uint256;
            //^

     """
  )

  fun testResolveMultipleFunctionInUsingFor2() = checkByCode(
    """
  pragma solidity ^0.8.26;

    library L {
      function f1(uint256 a) internal pure returns (uint256) {
        return a;
      }
      function f2(uint256 a) internal pure returns (uint256) {
              //x
        return a + 1;
      }
    }

    using { L.f1, L.f2 } for uint256;
                   //^

     """
  )

  fun testResolveTypeInUsingFor() = checkByCode("""
    pragma solidity ^0.8.26;
    
    type Foo is uint256;
        //x   

    library FooLib {
        function isHappy(Foo f) internal pure returns(bool) {
            return Foo.unwrap(f) > 100;
        }
    }
    
    using {
        FooLib.isHappy
    } for Foo global;
        //^
     """)

  fun testResolveFunctionInUsingFor() = checkByCode("""
    pragma solidity ^0.8.26;
    
    type Foo is uint256;

    function isHappy(Foo f) internal pure returns(bool) {
                //x   
        return Foo.unwrap(f) > 100;
    }
    
    using {
        isHappy
          //^
    } for Foo global;
     """)

  fun testResolveFunctionInUsingFor2() = checkByCode("""
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
