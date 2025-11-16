package me.serce.solidity.ide.run

import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import me.serce.solidity.resolveForgeExecutable
import me.serce.solidity.settings.ConfigurationMode
import me.serce.solidity.settings.SoliditySettings
import me.serce.solidity.settings.SoliditySettingsState
import me.serce.solidity.testutil.TestExecutable
import java.nio.file.Paths

class ForgeTestSettingsTest : BasePlatformTestCase() {

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
            settings.testFoundryExecutablePath = ""

            val resolved = resolveForgeExecutable(settings.testFoundryExecutablePath, false)

            val expected = "TEST_HOME/.foundry/bin/forge"
            assertEquals(expected, resolved)
        }
    }

    fun testResolveForgeExecutableWindows() {
        withUserHome("TEST_HOME") {
            val settings = SoliditySettings()
            settings.formatterFoundryExecutablePath = ""

            val resolved = resolveForgeExecutable(settings.testFoundryExecutablePath, true)

            // Ideally, this test would verify the win separator, but the Paths.get behaviour isn't mockable.
            val expected = "TEST_HOME/.foundry/bin/forge.exe"
            assertEquals(expected, resolved)
        }
    }

    fun testExecuteFoundryTestGutterWithAutomaticPath() =
        checkPathWithForgeTestCommandLineState(ConfigurationMode.AUTOMATIC, false, "")

    fun testExecuteFoundryTestGutterWithManualPath() =
        checkPathWithForgeTestCommandLineState(ConfigurationMode.MANUAL, false, "")

    fun testExecuteFoundryTestGutterWithAutomaticPathWindows() =
        checkPathWithForgeTestCommandLineState(ConfigurationMode.AUTOMATIC, true, "")

    fun testExecuteFoundryTestGutterWithManualPathWindows() =
        checkPathWithForgeTestCommandLineState(ConfigurationMode.MANUAL, true, "")

    fun testExecuteFoundryTestGutterWithAutomaticPathAndConfigPath() =
        checkPathWithForgeTestCommandLineState(ConfigurationMode.AUTOMATIC, false, "not/blank/path")

    fun testExecuteFoundryTestGutterWithManualPathAndConfigPath() =
        checkPathWithForgeTestCommandLineState(ConfigurationMode.MANUAL, false, "not/blank/path")

    fun testExecuteFoundryTestGutterWithAutomaticPathWindowsAndConfigPath() =
        checkPathWithForgeTestCommandLineState(ConfigurationMode.AUTOMATIC, true, "not/blank/path")

    fun testExecuteFoundryTestGutterWithManualPathWindowsAndConfigPath() =
        checkPathWithForgeTestCommandLineState(ConfigurationMode.MANUAL, true, "not/blank/path")


    private fun checkPathWithForgeTestCommandLineState(
        configurationMode: ConfigurationMode, isWindows: Boolean, configPath: String
    ) {
        val forge = TestExecutable.Builder(
            "forge", TestExecutable.Workdir.UnderDir(Paths.get(myFixture.tempDirPath)), testRootDisposable
        ).exitCode(0).build()

        val settings = SoliditySettings.getInstance(project).apply {
            testFoundryConfigurationMode = configurationMode
            testFoundryExecutablePath = if (configurationMode == ConfigurationMode.AUTOMATIC) "" else forge.path
            testFoundryConfigPath = configPath
        }

        val configurationType = ForgeTestRunConfigurationType()
        val factory = ForgeTestRunConfigurationFactory(configurationType)
        val configuration = ForgeTestRunConfiguration(project, factory, "Test Configuration")

//        configuration.workingDirectory = myFixture.tempDirPath
        configuration.testName = "testIncrement"
        configuration.contractName = "CounterTest"

        val executor = DefaultRunExecutor.getRunExecutorInstance()
        val configurationSettings = RunManager.getInstance(project).createConfiguration(configuration, factory)
        val env = ExecutionEnvironmentBuilder.create(executor, configurationSettings).build()

        val commandLineState = ForgeTestCommandLineState(configuration, env)

        val generatedCommandLine = commandLineState.generateCommandLine(isWindows)

        val resolved = resolveForgeExecutable(settings.testFoundryExecutablePath, isWindows)
        assertEquals(resolved, generatedCommandLine.toString().split(" ").first())
        assertTrue(
            if (configPath.isBlank()) {
                !generatedCommandLine.toString().contains("--root")
            } else {
                generatedCommandLine.parametersList.parameters.contains("--root")
                    .and(generatedCommandLine.parametersList.parameters.contains(configPath))
            }
        )
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
