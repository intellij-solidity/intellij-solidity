contract C {
  function f() {
    uint a = (1);
    var (b,) = (1,);
    var (c,d) = (1, 2 + a);
    var (e,) = (1, 2, b);
    (a) = 3;
  }
}