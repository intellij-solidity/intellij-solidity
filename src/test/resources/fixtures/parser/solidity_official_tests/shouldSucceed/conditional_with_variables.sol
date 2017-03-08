contract A {
  function f() {
    uint x = 3;
    uint y = 1;
    uint z = (x > y) ? x : y;
    uint w = x > y ? x : y;
  }
}