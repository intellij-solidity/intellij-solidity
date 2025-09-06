package me.serce.solidity.lang.core.resolve

class SolVarResolveTest : SolResolveTestBase() {
  fun testLocal1() = checkByCode("""
        contract B {
            function B() {
                var a0 = 0;
                var a = 0;
                  //x
                var b = 1;
                a = a++;
              //^
            }
        }
  """)

  fun testLocal2() = checkByCode("""
        contract B {
            function B() {
                var a0 = 0;
                var a = 0;
                  //x
                var b = 1;
                a = a++;
                  //^
            }
        }
  """)

  fun testLocalWithIf() = checkByCode("""
        contract B {
            function B() {
                if (true) {
                  var a0 = 0;
                  var a = 0;
                    //x
                  var b = 1;
                  a = a++;
                    //^
                }
            }
        }
  """)

  fun testField1() = checkByCode("""
        contract B {
            uint public lastC;
                        //x

            function B() {
                lastC = 1;
                //^
            }
        }
  """)

  fun testFieldWhenFunction() = checkByCode("""
      contract C {
          int public member;
                    //x
                    
          function member(uint value) public {

          }
          
          function other() public {
              member = 1;
               //^
          }
      }
  """)

  fun testConstantVariable() = checkByCode("""
      uint constant X = 32**22 + 8;
                  //x
    
      contract C { 
          function other() public {
              uint local = X;
                         //^
          }
      }
  """)

  fun testFunctionParams() = checkByCode("""
        contract B {
            function B(uint abc) {
                          //x
                _;
                abc = 1;
                //^
            }
        }
  """)

  fun testFunctionParamInOtherFunctionCall() = checkByCode("""
        contract B {
            function B(uint abc) {
                          //x
                _;
                require(abc == 100, "some condition");
                       //^
            }
        }
  """)

  fun testFunctionParamsSecond() = checkByCode("""
        contract B {
            function B(uint abc1, uint abc2) {
                                       //x
                abc1 = 1;
                abc2 = 1;
                //^
            }
        }
  """)

  fun testConstructorParams() = checkByCode("""
        contract B {
            constructor(uint abc) {
                          //x
                _;
                abc = 1;
                //^
            }
        }
  """)

  fun testReturnVars() = checkByCode("""
        contract B {
            function test() returns (uint abc) {
                                         //x
                _;
                abc = 1;
                //^
            }
        }
  """)

  fun testTupleDeclaration() = checkByCode("""
        contract B {
            function test() {
                var (var1, var2) = (5, 5);
                     //x
                _;
                var1 = 1;
                //^
            }
        }
  """)

  fun testTupleTypedDeclaration() = checkByCode("""
        contract B {
            function test() {
                (uint var1, uint var2) = (5, 5);
                     //x
                _;
                var1 = 1;
                //^
            }
        }
  """)

  fun testResolveStateInheritance() = checkByCode("""
        contract C {
            uint abc;
                //x
        }

        contract B {}

        contract A is B, C {
            function A() {
                abc = 1;
                //^
            }
        }
  """)

  fun testResolveSuper() = checkByCode("""
        contract B {
            var b;
              //x
        }

        contract A is B {
            function doit() {
                super.b;
                    //^
            }
        }
  """)

  fun testResolveSuperMultipleInheritance() = checkByCode("""
        contract Parent1 {

        }

        contract Parent2 {
            var b;
              //x
        }

        contract A is Parent1, Parent2 {
            function doit() {
                super.b;
                    //^
            }
        }
  """)

  fun testResolveForLoopVariable() = checkByCode("""
        contract B {
            function test() {
                for (var i = 0; i < 2; i++ ) {
                       //x
                    var g = i;
                          //^
                }
            }
        }          
    """)

  fun testResolveTryStatementVariable() = checkByCode("""
        contract B {
            function getData(address token) external returns (uint value);
            function test() {
                try feed.getData(token) returns (uint v) {
                                                    //x
                    return (v, true);
                          //^
                } catch Error(string memory reason) {
                }
            }
        }          
    """)

  fun testResolveCatchClauseVariable() = checkByCode("""
        contract B {
            function getData(address token) external returns (uint value);
            function test() {
                try feed.getData(token) returns (uint v) {
                    return (v, true);
                } catch Error(string memory reason) {
                                             //x
                    var r2 = reason;
                              //^
                }
            }
        }          
    """)

  fun testResolveUncheckBlockVariable() = checkByCode("""
        contract B {
            function test() {
                unchecked {
                    uint256 length = 1;
                              //x
                    string memory buffer = new string(length);
                                                        //^ 
                }
            }
        }      
    """)

  fun testResolveModifierParameter() = checkByCode("""
        contract B {
          modifier reinitializer(uint8 version) {
                                         //x
              var v = version;
                       //^ 
          }
        }          
    """)

  fun testResolveParameterDefInTuple() = checkByCode("""
        contract B {
          function test() {
              uint reserves = (tokenIn == token0 ? reserves0 : reserves1);
                      //x
              var v = reserves;
                       //^ 
          }
        }          
    """)

  fun testResolveImportedConstant() = testResolveBetweenFiles(
    InlineFile(
      code = """
          pragma solidity ^0.8.26;
                
          address constant USER_1 = address(0x1111111111111111111111111111111111111111);
                          //x
      """,
      name = "a.sol"
    ),
    InlineFile(
      """
          pragma solidity ^0.8.26;      
                
          import {USER_1} from "./a.sol";
                  //^
          contract b {
            address public user;

              function setUser() public {
                user = USER_1;
              }
          }
                      
    """
    )
  )

  fun testResolveImportedConstant2() = testResolveBetweenFiles(
    InlineFile(
      code = """
          pragma solidity ^0.8.26;
                
          address constant USER_1 = address(0x1111111111111111111111111111111111111111);
                          //x
      """,
      name = "a.sol"
    ),
    InlineFile(
      """
          pragma solidity ^0.8.26;      
                
          import {USER_1} from "./a.sol";
                  
          contract b {
            address public user;

              function setUser() public {
                user = USER_1;
                        //^
              }
          }
                      
    """
    )
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
}
