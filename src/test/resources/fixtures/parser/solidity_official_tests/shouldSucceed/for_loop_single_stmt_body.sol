contract test {
  function fun(uint256 a) {
    uint256 i = 0;
    for (i = 0; i < 10; i++)
      continue;
  }
}