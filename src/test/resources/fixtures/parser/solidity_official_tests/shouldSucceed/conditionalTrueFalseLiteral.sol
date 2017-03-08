contract A {
  function f() {
    uint x = true ? 1 : 0;
    uint y = false ? 0 : 1;
  }
}