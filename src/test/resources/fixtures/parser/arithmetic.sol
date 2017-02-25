contract test {
  function f(uint x, uint y) returns (uint z) {
    var c = (x) + 3;
    var c = x + 3;
    var b = 7 + (c * (8 - 7)) - x;
    return -(-b | 0);
  }
}
