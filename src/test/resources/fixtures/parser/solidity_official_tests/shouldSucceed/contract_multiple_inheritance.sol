contract base {
  function fun() {
    uint64(2);
  }
}
contract derived is base, nonExisting {
  function fun() {
    uint64(2);
  }
}