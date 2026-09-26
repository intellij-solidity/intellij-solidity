package me.serce.solidity.ide.inspections

import junit.framework.TestCase

class ChecksumAddressTest : TestCase() {
  fun testChecksumsEip55Vectors() {
    assertEquals(
      "0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed",
      toChecksumAddress("0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed")
    )
    assertEquals(
      "0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359",
      toChecksumAddress("0xfb6916095ca1df60bb79ce92ce3ea74c37c5d359")
    )
    assertEquals(
      "0xdbF03B407c01E7cD3CBea99509d93f8DDDC8C6FB",
      toChecksumAddress("0xdbf03b407c01e7cd3cbea99509d93f8dddc8c6fb")
    )
    assertEquals(
      "0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb",
      toChecksumAddress("0xd1220a0cf47c7b9be7a2e6ba89f429762e7b9adb")
    )
  }

  fun testNormalizesInputBeforeChecksumming() {
    assertEquals(
      "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2",
      toChecksumAddress("0xC02AAA39B223FE8D0A0E5C4F27EAD9083C756CC2")
    )
  }
}