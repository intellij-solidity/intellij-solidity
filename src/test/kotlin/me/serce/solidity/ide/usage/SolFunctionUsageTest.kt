package me.serce.solidity.ide.usage

class SolFunctionUsageTest : SolUsageTestBase() {

  fun testFindUsageFunctionWithModifier() = doTest(
    """
        pragma solidity ^0.8.26;

        contract Temp {
            modifier onlyOwner() {
                _;
            }
        
            function foo() public onlyOwner {
                    //^
            }
        
            function bar() external {
                foo();
            }
        }
  """,1
  )
}
