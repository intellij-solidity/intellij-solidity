contract A {
  function f() {
    uint x = 3 > 0 ? 3 : 0;
    uint y = (3 > 0) ? 3 : 0;
  }
}