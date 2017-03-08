contract C {
  struct S { uint a; uint b; uint[][][] c; }
  function f() {
    C.S x;
    C.S memory y;
    C.S[10] memory z;
    C.S[10](x);
    x.a = 2;
    x.c[1][2][3] = 9;
  }
}