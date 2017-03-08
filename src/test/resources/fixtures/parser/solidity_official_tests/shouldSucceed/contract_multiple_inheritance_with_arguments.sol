contract base {
  function fun() {
    uint64(2);
  }
}
contract derived is base(2), nonExisting("abc", "def", base.fun()) {
  function fun() {
    uint64(2);
  }
}