contract c {
  uint[] a;
  function f() returns (uint, uint) {
    a = [1,2,3];
    return (a[3], [2,3,4][0]);
  }
}