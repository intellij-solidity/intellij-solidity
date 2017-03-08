contract c {
  modifier mod1(uint a) { if (msg.sender == a) _; }
  modifier mod2 { if (msg.sender == 2) _; }
  function f() mod1(7) mod2 { }
}