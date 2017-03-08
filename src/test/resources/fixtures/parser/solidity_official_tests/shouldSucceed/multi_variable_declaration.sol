contract C {
  function f() {
    var (a,b,c) = g();
    var (d) = 2;
    var (,e) = 3;
    var (f,) = 4;
    var (x,,) = g();
    var (,y,) = g();
    var () = g();
    var (,,) = g();
  }
  function g() returns (uint, uint, uint) {}
}