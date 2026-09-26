package me.serce.solidity.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import me.serce.solidity.ide.inspections.fixes.ConvertToChecksumAddressFix
import me.serce.solidity.ide.inspections.fixes.PrependZeroAddressFix
import me.serce.solidity.lang.psi.SolNumberLiteral
import me.serce.solidity.lang.psi.SolVisitor

class AddressChecksumInspection : LocalInspectionTool() {
  override fun getDisplayName(): String = ""

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : SolVisitor() {
      override fun visitNumberLiteral(o: SolNumberLiteral) {
        val text = o.text
        if (!ADDRESS_REGEX.matches(text) || isValidChecksum(text)) {
          return
        }
        val checksummed = toChecksumAddress(text)
        holder.registerProblem(
          o,
          "Invalid address checksum. Correct checksummed address: $checksummed",
          ProblemHighlightType.GENERIC_ERROR,
          ConvertToChecksumAddressFix(checksummed),
          PrependZeroAddressFix()
        )
      }
    }
  }

  private fun isValidChecksum(address: String): Boolean {
    // EIP-55 only changes the case of letters, so an address without a-f/A-F can never be invalid.
    if (address.none { it in 'a'..'f' || it in 'A'..'F' }) {
      return true
    }
    return toChecksumAddress(address) == address
  }
}