contract c {
  uint[] a;
  function f() returns (uint) {
    a = [,2,3];
    return (a[0]);
  }
}