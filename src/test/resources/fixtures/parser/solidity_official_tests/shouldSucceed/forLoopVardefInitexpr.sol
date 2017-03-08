contract test {
  function fun(uint256 a) {
    for (uint256 i = 0; i < 10; i++) {
      uint256 x = i; break; continue;
    }
  }
}