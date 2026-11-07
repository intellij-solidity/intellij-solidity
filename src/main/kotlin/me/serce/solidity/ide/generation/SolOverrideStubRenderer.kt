package me.serce.solidity.ide.generation

import me.serce.solidity.ide.inspections.fixes.SolSpecifierEditor
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.lang.psi.SolModifierDefinition
import me.serce.solidity.lang.psi.SolNamedElement
import me.serce.solidity.lang.psi.Visibility
import me.serce.solidity.lang.resolve.function.SolOverrideResolver

/** Renders the unindented source of a member that overrides [base]; the caller indents it. */
object SolOverrideStubRenderer {

  fun render(base: SolNamedElement, overrideBases: List<String>): String {
    val override = SolSpecifierEditor.overrideText(overrideBases)
    return when (base) {
      is SolModifierDefinition -> "modifier ${base.name}${SolOverrideResolver.parametersText(base) ?: ""} $override {\n_;\n}"
      is SolFunctionDefinition -> renderFunction(base, override)
      else -> error("Unsupported member: $base")
    }
  }

  private fun renderFunction(f: SolFunctionDefinition, override: String): String {
    val head = if (f.specialFunction != null) SolOverrideResolver.displayName(f) else "function ${f.name}"
    val parts = mutableListOf(head + SolOverrideResolver.parametersText(f))
    parts += f.functionVisibilitySpecifierList.mapNotNull { it.visibilitySpecifier?.text }
    parts += f.stateMutabilitySpecifierList.map { it.text }
    parts += override
    val header = parts.joinToString(" ") + (f.returns?.let { " returns ${it.text}" } ?: "")
    val body = superCall(f)
    return if (body.isEmpty()) "$header {\n}" else "$header {\n$body\n}"
  }

  /** `super.f(args);` when the base implementation is reachable through `super`, otherwise empty. */
  private fun superCall(f: SolFunctionDefinition): String {
    if (!SolOverrideResolver.hasBody(f) || f.specialFunction != null || f.visibility == Visibility.EXTERNAL) return ""
    val args = f.parameters.map { it.name ?: return "" }
    val call = "super.${f.name}(${args.joinToString(", ")});"
    return if (f.returns != null) "return $call" else call
  }
}
