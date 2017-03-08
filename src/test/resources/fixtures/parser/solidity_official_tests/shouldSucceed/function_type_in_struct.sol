contract test {
  struct S {
    function (uint x, uint y) internal returns (uint a) f;
    function (uint, uint) external returns (uint) g;
    uint d;
  }
}