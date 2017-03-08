contract test {
  function fun(uint256 a) {
    uint256 x = (1 + 4) || false && (1 - 12) + -9;
  }
}