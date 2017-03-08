contract C {
  struct s { uint a; }
  using LibraryName for uint;
  using Library2 for *;
  using Lib for s;
  function f() {
  }
}