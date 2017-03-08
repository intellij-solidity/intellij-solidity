contract c {
  uint[] a;
  function f() returns (uint, uint) {
    return ([3, ,4][0]);
  }
}