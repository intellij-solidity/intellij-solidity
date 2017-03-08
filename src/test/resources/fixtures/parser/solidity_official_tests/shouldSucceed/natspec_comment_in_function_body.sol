contract test {
  /// fun1 description
  function fun1(uint256 a) {
    var b;
    /// I should not interfere with actual natspec comments
    uint256 c;
    mapping(address=>bytes32) d;
    bytes7 name = "Solidity";
  }
  /// This is a test function
  /// and it has 2 lines
  function fun(bytes32 input) returns (bytes32 out) {}
}