package me.serce.solidity.ide.inspections

class UnprotectedFunctionInspectionTest : SolInspectionsTestBase(UnprotectedFunctionInspection()) {
  fun testOwner() = checkByText("""
      contract Ownable {
        address public owner;

        function Ownable() {
          setOwner(msg.sender);
        }

        function /*@warning descr="${UnprotectedFunctionInspection.MESSAGE}"@*/setOwner/*@/warning@*/(address owner) {
          owner = msg.sender;
        }
      }
  """)

  fun testMultipleOwners() = checkByText("""
      contract Ownable {
        uint[256] m_owners;

        function /*@warning descr="${UnprotectedFunctionInspection.MESSAGE}"@*/initMultiowned/*@/warning@*/(address[] _owners, uint _required) {
          m_owners[1] = uint(msg.sender);
        }
      }
  """)
}
