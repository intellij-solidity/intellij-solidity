package me.serce.solidity.ide.inspections

class AddressChecksumInspectionTest : SolInspectionsTestBase(AddressChecksumInspection()) {
  fun testFlagsInvalidLowercaseChecksum() = checkByText(
    """
        contract A {
            address a = /*@error descr="Invalid address checksum. Correct checksummed address: 0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"@*/0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2/*@/error@*/;
        }
    """.trimIndent()
  )

  fun testAcceptsValidChecksum() = checkByText(
    """
        contract A {
            address a = 0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2;
        }
    """.trimIndent()
  )

  fun testAcceptsAllDigitAddress() = checkByText(
    """
        contract A {
            address a = 0x0000000000000000000000000000000000000000;
        }
    """.trimIndent()
  )

  fun testAcceptsNonAddressNumber() = checkByText(
    """
        contract A {
            uint256 a = 0x1234;
        }
    """.trimIndent()
  )

  fun testConvertToChecksummedAddressFix() {
    myFixture.configureByText("A.sol", "contract A { address a = 0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2; }")
    enableInspection()
    applyQuickFix("Convert to checksummed address")
    myFixture.checkResult("contract A { address a = 0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2; }")
  }

  fun testPrependZeroFix() {
    myFixture.configureByText("A.sol", "contract A { address a = 0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2; }")
    enableInspection()
    applyQuickFix("Prepend '00' to treat as a number")
    myFixture.checkResult("contract A { address a = 0x00c02aaa39b223fe8d0a0e5c4f27ead9083c756cc2; }")
  }
}
