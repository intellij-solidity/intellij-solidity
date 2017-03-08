contract test {
  function f() {
    function() returns(function() returns(function() returns(function() returns(uint)))) x;
    uint y;
    y = x()()()();
  }
}