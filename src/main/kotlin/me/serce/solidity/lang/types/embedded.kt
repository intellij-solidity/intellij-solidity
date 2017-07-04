package me.serce.solidity.lang.types

import com.intellij.openapi.project.Project
import me.serce.solidity.lang.psi.SolPsiFactory

class SolEmbeddedTypeFactory(val project: Project) {
  private val psiFactory: SolPsiFactory = SolPsiFactory(project)

  fun solMessageType(): SolType {
    return SolStruct(psiFactory.createStruct("""
    struct msg {
        bytes data;
        uint gas;
        address sender;
        uint value;
    }
  """))
  }
}
