package me.serce.solidity.ide.formatting

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import me.serce.solidity.settings.SoliditySettings

class SolidityExternalFormatterTest : BasePlatformTestCase() {

  fun testResolveForgeExecutableMac() {
    withUserHome("TEST_HOME") {
      val settings = SoliditySettings()
      settings.executablePath = ""

      val formatter = SolidityExternalFormatter()
      val resolved = formatter.resolveForgeExecutable(settings, false)

      val expected = "TEST_HOME/.foundry/bin/forge"
      assertEquals(expected, resolved)
    }
  }

  fun testResolveForgeExecutableWindows() {
    withUserHome("TEST_HOME") {
      val settings = SoliditySettings()
      settings.executablePath = ""

      val formatter = SolidityExternalFormatter()
      val resolved = formatter.resolveForgeExecutable(settings, true)

      // Ideally, this test would verify the win separator, but the Paths.get behaviour isn't mockable. 
      val expected = "TEST_HOME/.foundry/bin/forge.exe"
      assertEquals(expected, resolved)
    }
  }

  private fun withUserHome(fakeHome: String, block: () -> Unit) {
    val old = System.getProperty("user.home")
    try {
      System.setProperty("user.home", fakeHome)
      block()
    } finally {
      if (old == null) {
        System.clearProperty("user.home")
      } else {
        System.setProperty("user.home", old)
      }
    }
  }
}
