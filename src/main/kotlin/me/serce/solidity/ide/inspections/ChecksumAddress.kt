package me.serce.solidity.ide.inspections

import org.bouncycastle.crypto.digests.KeccakDigest

internal val ADDRESS_REGEX = Regex("0x[0-9a-fA-F]{40}")

internal fun toChecksumAddress(address: String): String {
  val lower = address.removePrefix("0x").lowercase()
  val hash = keccak256(lower.toByteArray(Charsets.US_ASCII))
  val result = StringBuilder("0x")
  for (i in lower.indices) {
    val c = lower[i]
    if (c in '0'..'9') {
      result.append(c)
    } else {
      val nibble = (hash[i / 2].toInt() shr (if (i % 2 == 0) 4 else 0)) and 0x0F
      result.append(if (nibble >= 8) c.uppercaseChar() else c)
    }
  }
  return result.toString()
}

private fun keccak256(input: ByteArray): ByteArray {
  val digest = KeccakDigest(256)
  digest.update(input, 0, input.size)
  val output = ByteArray(32)
  digest.doFinal(output, 0)
  return output
}