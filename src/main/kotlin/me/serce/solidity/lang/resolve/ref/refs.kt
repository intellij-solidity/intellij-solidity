package me.serce.solidity.lang.resolve.ref

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.descendants
import me.serce.solidity.lang.completion.SolCompleter
import me.serce.solidity.lang.core.SolidityTokenTypes
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.psi.impl.SolFunctionDefMixin
import me.serce.solidity.lang.psi.impl.SolNewExpressionElement
import me.serce.solidity.lang.psi.impl.SolYulPathElement
import me.serce.solidity.lang.resolve.SolResolver
import me.serce.solidity.lang.resolve.canBeApplied
import me.serce.solidity.lang.resolve.function.SolFunctionResolver
import me.serce.solidity.lang.types.*
import me.serce.solidity.wrap

class SolUserDefinedTypeNameReference(element: SolUserDefinedTypeName) : SolReferenceBase<SolUserDefinedTypeName>(element), SolReference {
  override fun multiResolve(): Collection<PsiElement> {
    val parent = element.parent
    if (parent is SolNewExpressionElement) {
      return SolResolver.resolveNewExpression(parent)
    } else if (parent is SolUsingForElement) {
      val resolved = SolResolver.resolveUsingForElement(element)
      if (resolved != null) return listOf(resolved)
    }
    return SolResolver.resolveTypeNameUsingImports(element)
  }

  override fun getVariants() = SolCompleter.completeTypeName(element)
}

class SolQualifierTypeNameReference(
  element: SolUserDefinedTypeName, private val identifier: PsiElement
) : SolReferenceBase<SolUserDefinedTypeName>(element), SolReference {

  override fun calculateDefaultRangeInElement(): TextRange {
    return TextRange(identifier.startOffsetInParent, identifier.startOffsetInParent + identifier.textLength)
  }

  override fun multiResolve(): Collection<PsiElement> {
    return SolResolver.resolveTypeNameUsingImports(identifier)
  }

  override fun handleElementRename(newName: String): PsiElement {
    doRename(identifier, newName)
    return element
  }
}

class SolVarLiteralReference(element: SolVarLiteral) : SolReferenceBase<SolVarLiteral>(element), SolReference {
  override fun multiResolve() = SolResolver.resolveVarLiteralReference(element)

  override fun getVariants() = SolCompleter.completeLiteral(element).toList().toTypedArray()
}

class SolYulLiteralReference(element: SolYulPathElement) : SolReferenceBase<SolYulPathElement>(element), SolReference {
  override fun multiResolve() = SolResolver.resolveVarLiteralReference(element)

  override fun getVariants() = SolCompleter.completeLiteral(element).toList().toTypedArray()
}


class SolModifierReference(
  element: SolReferenceElement,
  private val modifierElement: SolModifierInvocationElement
) : SolReferenceBase<SolReferenceElement>(element), SolReference {

  override fun calculateDefaultRangeInElement() = element.referenceNameElement.parentRelativeRange

  override fun multiResolve(): List<SolNamedElement> {
    val contract = modifierElement.findContract() ?: return emptyList()
    val superNames: List<String> = (contract.collectSupers.map { it.name } + contract.name).filterNotNull()
    return SolResolver.resolveTypeNameUsingImports(modifierElement)
      .filterIsInstance<SolModifierDefinition>()
      .filter { it.contract.name in superNames }
  }

  override fun getVariants() = SolCompleter.completeModifier(modifierElement)

  override fun doRename(identifier: PsiElement, newName: String) {
    val modifierId = modifierElement.descendants().filter { it.elementType == SolidityTokenTypes.IDENTIFIER }.firstOrNull() ?: return
    super.doRename(modifierId, newName)
  }
}

class SolMemberAccessReference(element: SolMemberAccessExpression) : SolReferenceBase<SolMemberAccessExpression>(element), SolReference {
  override fun calculateDefaultRangeInElement(): TextRange {
    return element.identifier?.parentRelativeRange ?: super.calculateDefaultRangeInElement()
  }

  override fun multiResolve(): List<SolNamedElement> {
    val firstMemberElement = element.childOfType<SolPrimaryExpression>()
    val importAlias = firstMemberElement?.varLiteral?.let { varLiteral -> SolResolver.resolveAlias(varLiteral) }
    if (importAlias != null && SolResolver.isAliasOfFile(importAlias)) {
      return when (element.parent is SolFunctionCallExpression) {
        true -> (element.parent.reference as SolFunctionCallReference).resolveFunctionCallAndFilter()
          .mapNotNull { it.resolveElement() }

        else -> {
          SolResolver.resolveTypeNameUsingImports(element).toList()
        }
      }
    }
    return SolResolver.resolveMemberAccess(element).mapNotNull { it.resolveElement() }
  }

  override fun getVariants() = SolCompleter.completeMemberAccess(element)
}

class SolNewExpressionReference(val element: SolNewExpression) : SolReferenceBase<SolNewExpression>(element), SolReference {

  override fun calculateDefaultRangeInElement(): TextRange {
    return element.referenceNameElement.parentRelativeRange
  }

  override fun multiResolve(): Collection<PsiElement> {
    val types = SolResolver.resolveTypeNameUsingImports(element.referenceNameElement)
    return types
      .filterIsInstance(SolContractDefinition::class.java)
      .flatMap {
        val constructors = it.findConstructors()
        if (constructors.isEmpty()) {
          listOf(it)
        } else {
          constructors
        }
      }
  }
}

fun SolContractDefinition.findConstructors(): List<SolElement> {
  return if (this.constructorDefinitionList.isNotEmpty()) {
    this.constructorDefinitionList
  } else {
    this.functionDefinitionList
      .filter { it.name == this.name }
  }
}

class SolFunctionCallReference(element: SolFunctionCallExpression) : SolReferenceBase<SolFunctionCallExpression>(element), SolReference {
  override fun calculateDefaultRangeInElement(): TextRange {
    return element.referenceNameElement.parentRelativeRange
  }

  fun resolveFunctionCall(): Collection<SolCallable> {
    if (element.parent is SolRevertStatement) {
      val errors = SolResolver.resolveTypeNameUsingImports(element).filterIsInstance<SolErrorDefinition>()
      if (errors.isNotEmpty()) {
        return errors
      }
    }
    if (element.parent is SolEmitStatement) {
      val events = SolResolver.resolveTypeNameUsingImports(element).filterIsInstance<SolEventDefinition>()
      if (events.isNotEmpty()) {
        return events
      }
    }
    if (element.firstChild is SolPrimaryExpression) {
      val structs = SolResolver.resolveTypeNameUsingImports(element.firstChild).filterIsInstance<SolStructDefinition>()
      if (structs.isNotEmpty()) {
        return structs
      }
    }
    val resolved: Collection<SolCallable> = when (val expr = element.expression) {
      is SolPrimaryExpression -> {
        val regular = expr.varLiteral?.let { SolResolver.resolveVarLiteral(it) }
          ?.filter { it !is SolStateVariableDeclaration }
          ?.filterIsInstance<SolCallable>()
          ?: emptyList()
        val casts = resolveElementaryTypeCasts(expr)
        regular + casts
      }
      is SolMemberAccessExpression -> {
        SolResolver.resolveMemberFunctions(expr)
      }
      else ->
        emptyList()
    }
    return removeOverrides(resolved.groupBy { it.callablePriority }.entries.minByOrNull { it.key }?.value ?: emptyList())
  }

  private fun removeOverrides(callables: Collection<SolCallable>): Collection<SolCallable> {
    val test = callables.filterIsInstance<SolFunctionDefinition>().flatMap { SolFunctionResolver.collectOverridden(it) }.toSet()
    return callables
      .filter {
        when (it) {
          is SolFunctionDefinition -> !test.contains(it)
          else -> true
        }
      }
  }

  private fun resolveElementaryTypeCasts(expr: SolPrimaryExpression): Collection<SolCallable> {
    return expr.elementaryTypeName
      ?.let {
        val type = getSolType(it)
        object : SolCallable {
          override fun resolveElement(): SolNamedElement? = null
          override fun parseParameters(): List<Pair<String?, SolType>> = listOf(null to SolUnknown)
          override fun parseType(): SolType = type
          override val callablePriority: Int = 1000
          override fun getName(): String? = null
        }
      }
      .wrap()
  }

  override fun multiResolve(): Collection<PsiElement> {
    return resolveFunctionCallAndFilter()
      .mapNotNull { it.resolveElement() }
  }

  fun resolveFunctionCallAndFilter(): List<SolCallable> {
    return resolveFunctionCall()
      .filter { it.canBeApplied(element.functionCallArguments) }
  }
}

class LibraryFunDefinition(private val original: SolFunctionDefinition) : SolFunctionDefinition by original {
  override val parameters: List<SolParameterDef>
    get() = original.parameters.drop(1)


  override fun parseParameters(): List<Pair<String?, SolType>> {
    return SolFunctionDefMixin.parseParameters(parameters)
  }

  override fun equals(other: Any?): Boolean {
    if (other is LibraryFunDefinition) {
      return original == other.original
    }
    return super.equals(other)
  }

  override fun hashCode(): Int {
    return original.hashCode()
  }

}
fun SolFunctionDefinition.toLibraryFunDefinition(): SolFunctionDefinition {
  return LibraryFunDefinition(this)
}
