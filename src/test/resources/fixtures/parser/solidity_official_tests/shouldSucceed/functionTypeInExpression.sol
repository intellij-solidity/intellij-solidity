contract test {
  function f(uint x, uint y) returns (uint a) {}
  function g() {
    function (uint, uint) internal returns (uint) f1 = f;
  }
}