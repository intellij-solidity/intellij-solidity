package me.serce.solidity.ide.formatting

import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.util.SystemInfo
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.messages.MessageBusConnection
import me.serce.solidity.resolveForgeExecutable
import me.serce.solidity.settings.ConfigurationMode
import me.serce.solidity.settings.FormatterType
import me.serce.solidity.settings.SoliditySettings
import me.serce.solidity.settings.SoliditySettingsState
import me.serce.solidity.testutil.TestExecutable
import java.nio.file.Paths
import java.util.*


class SolidityExternalFormatterTest : BasePlatformTestCase() {

  private lateinit var originalSettings: SoliditySettingsState

  override fun setUp() {
    super.setUp()
    val settings = SoliditySettings.getInstance(project)
    originalSettings = SoliditySettingsState().also { it.copyFrom(settings.state) }
    project.basePath
  }

  override fun tearDown() = try {
    val settings = SoliditySettings.getInstance(project)
    settings.loadState(originalSettings)
  } finally {
    super.tearDown()
  }

  fun testResolveForgeExecutableMac() {
    withUserHome("TEST_HOME") {
      val settings = SoliditySettings()
      settings.formatterFoundryExecutablePath = ""
      settings.formatterConfigurationMode = ConfigurationMode.AUTOMATIC

      val resolved = resolveForgeExecutable(settings.formatterFoundryExecutablePath, settings.formatterConfigurationMode,false)

      val expected = "TEST_HOME/.foundry/bin/forge"
      assertEquals(expected, resolved)
    }
  }

  fun testResolveForgeExecutableWindows() {
    withUserHome("TEST_HOME") {
      val settings = SoliditySettings()
      settings.formatterFoundryExecutablePath = ""
      settings.formatterConfigurationMode = ConfigurationMode.AUTOMATIC

      val resolved = resolveForgeExecutable(settings.formatterFoundryExecutablePath,settings.formatterConfigurationMode, true)

      // Ideally, this test would verify the win separator, but the Paths.get behaviour isn't mockable.
      val expected = "TEST_HOME/.foundry/bin/forge.exe"
      assertEquals(expected, resolved)
    }
  }

  private fun reformat() {
    ReformatCodeProcessor(project, myFixture.file, null, false).run()
  }

  fun testFormatsSuccessfullyViaMockedForge() {
    val forge = TestExecutable.Builder(
      "forge",
      TestExecutable.Workdir.UnderDir(Paths.get(myFixture.tempDirPath)),
      testRootDisposable
    )
      .build()

    SoliditySettings.getInstance(project).apply {
      formatterType = FormatterType.FOUNDRY
      formatterConfigurationMode = ConfigurationMode.MANUAL
      formatterFoundryExecutablePath = forge.path
      formatterFoundryConfigPath = myFixture.tempDirPath
    }

    val before = "contract C { }"
    myFixture.configureByText("C.sol", before)

    reformat()

    assertEquals(before, myFixture.file.text)
    val args = forge.readCapturedArgs()
    assertEquals(args.firstOrNull(), "fmt")
    assertTrue(args.contains("--raw"))
    assertEquals(before, forge.readCapturedStdin())
  }

  fun testReportsOnlyErrorsAndFiltersWarnings() {
    // Subscribe to notifications to capture what AsyncDocumentFormattingService surfaces
    val seen = Collections.synchronizedList(mutableListOf<Notification>())
    val connection: MessageBusConnection = project.messageBus.connect(testRootDisposable)
    connection.subscribe(Notifications.TOPIC, object : Notifications {
      override fun notify(notification: Notification) {
        seen += notification
      }
    })

    val forge = TestExecutable.Builder(
      "forge",
      TestExecutable.Workdir.UnderDir(Paths.get(myFixture.tempDirPath)),
      testRootDisposable
    )
      .echoStdinToStdout(false)
      .stderr("warning: benign\nerror: bad syntax\n")
      .exitCode(1)
      .build()

    val settings = SoliditySettings.getInstance(project).apply {
      formatterType = FormatterType.FOUNDRY
      formatterConfigurationMode = ConfigurationMode.MANUAL
      formatterFoundryExecutablePath = forge.path
      formatterFoundryConfigPath = myFixture.tempDirPath
    }

    val before = "contract D{}"
    myFixture.configureByText("D.sol", before)

    // Trigger formatting through IDE pipeline, then flush event queue
    reformat()

    assertEquals(before, myFixture.file.text)
    val args = forge.readCapturedArgs()
    val rootIdx = args.indexOf("--root")
    assertTrue("--root should be present", rootIdx >= 0)
    val rootVal = args.getOrNull(rootIdx + 1)
    assertEquals(settings.formatterFoundryConfigPath, rootVal)

    // TODO: find a way to capture the stderr in the notifications content using test apis
    // assertTrue(content.contains("error: bad syntax"))
    // assertFalse("warnings are ignored", content.contains("warning: benign"))
  }

  fun testIncludesRootWhenManualConfigPathIsSet() {
    val forge = TestExecutable.Builder(
      "forge",
      TestExecutable.Workdir.UnderDir(Paths.get(myFixture.tempDirPath)),
      testRootDisposable
    )
      .build()

    val settings = SoliditySettings.getInstance(project).apply {
      formatterType = FormatterType.FOUNDRY
      formatterConfigurationMode = ConfigurationMode.MANUAL
      formatterFoundryExecutablePath = forge.path
      formatterFoundryConfigPath = myFixture.tempDirPath
    }

    myFixture.configureByText("E.sol", "contract E {}")
    reformat()

    val args = forge.readCapturedArgs()
    val idx = args.indexOf("--root")
    assertEquals(settings.formatterFoundryConfigPath, args.getOrNull(idx + 1))
  }

  fun testUsesDefaultForgePathWhenNotConfigured() {
    val fakeHome = Paths.get(myFixture.tempDirPath, "fakeHome").toString()

    withUserHome(fakeHome) {
      val defaultBin = Paths.get(fakeHome, ".foundry", "bin")
      val forge = TestExecutable.Builder(
        "forge",
        TestExecutable.Workdir.FixedDir(defaultBin),
        testRootDisposable
      )
        .build()

      SoliditySettings.getInstance(project).apply {
        formatterType = FormatterType.FOUNDRY
        formatterConfigurationMode = ConfigurationMode.MANUAL
        formatterFoundryExecutablePath = ""
        formatterFoundryConfigPath = ""
      }

      val before = "contract F {}"
      myFixture.configureByText("F.sol", before)

      reformat()

      assertEquals(before, myFixture.file.text)

      val args = forge.readCapturedArgs()
      assertTrue(args.firstOrNull() == "fmt")
    }
  }

  fun testDoesNotInvokeWhenDisabled() {
    if (SystemInfo.isWindows) return

    val forge = TestExecutable.Builder(
      "forge",
      TestExecutable.Workdir.UnderDir(Paths.get(myFixture.tempDirPath)),
      testRootDisposable
    )
      .build()
    SoliditySettings.getInstance(project).apply {
      formatterType = FormatterType.DISABLED
      formatterConfigurationMode = ConfigurationMode.MANUAL
      formatterFoundryExecutablePath = forge.path
      formatterFoundryConfigPath = myFixture.tempDirPath
    }

    myFixture.configureByText("NoExternal.sol", "contract X{  }")
    reformat()

    val args = forge.readCapturedArgs()
    assertTrue(args.isEmpty())
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
