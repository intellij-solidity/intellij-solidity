contract A {
  function a() {
    uint a = b
    ? c
    : d
    ? e
    : f;

    return (
    a,
    b == c
    ? d
    : e,
    f
    );
  }
}
