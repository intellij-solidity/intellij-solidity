package me.serce.solidity.lang.core.resolve

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.command.WriteCommandAction
import me.serce.solidity.lang.psi.SolNamedElement

class SolImportResolveFoundryTest : SolResolveTestBase() {

    private val testcases = arrayListOf(
        Pair("lib/forge-std/src/Test.sol", "contracts/ImportUsageFoundryStd.sol"),
        Pair("lib/solmate/src/tokens/ERC721.sol", "contracts/ImportUsageFoundrySolmate.sol"),
        Pair(
            "lib/openzeppelin-contracts/contracts/token/ERC20/ERC20.sol", "contracts/ImportUsageFoundryOpenzeppelin.sol"
        ),
    );

    fun testImportPathResolveFoundryRemappings() {
        testImportPathResolveWithConfig("remappings.txt")
    }

    fun testImportPathResolveFoundryFoundryFile() {
        testImportPathResolveWithConfig("foundry.toml")
    }

    fun testImportPathResolveRemappingsNoTrailingSlash() {
        myFixture.addFileToProject(
            "vendor/@chainlink/contracts/src/Token.sol",
            "contract Token {}"
        )
        myFixture.addFileToProject(
            "remappings.txt",
            "@chainlink/contracts/=vendor/@chainlink/contracts"
        )
        val usage = myFixture.addFileToProject(
            "contracts/ImportChainlink.sol",
            """
            import "@chainlink/contracts/src/Token.sol";
                   //^
            contract ImportUsage {}
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(usage.virtualFile)
        val (refElement, _) = findElementAndDataInEditor<SolNamedElement>()
        val resolved = checkNotNull(refElement.reference?.resolve()) {
            "Failed to resolve import @chainlink/contracts/src/Token.sol via remappings.txt"
        }
        assertEquals("Token.sol", resolved.containingFile.name)
    }

    fun testImportPathResolveFoundryFoundryFileNoError() {
        testcases.forEach { (_, contractFile) ->
            myFixture.configureByText("foundry.toml", "")
            myFixture.configureByFile(contractFile)
            val (refElement) = findElementAndDataInEditor<SolNamedElement>("^")
            check(refElement.reference?.resolve() == null) {
                "Should failed to resolve ${refElement.text}"
            }
        }
    }

    fun testImportPathResolveFoundryTomlCacheInvalidation() {
        myFixture.addFileToProject("lib/a/src/Test.sol", "contract A {}")
        myFixture.addFileToProject("lib/b/src/Test.sol", "contract B {}")
        val foundryToml = myFixture.addFileToProject(
            "foundry.toml",
            """
            [profile.default]
            remappings = ["forge-std/=lib/a/src/"]
            """.trimIndent()
        )
        val usage = myFixture.addFileToProject(
            "contracts/ImportUsageFoundryCache.sol",
            """
            import "forge-std/Test.sol";
                   //^
            
            contract ImportUsage {}
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(usage.virtualFile)

        val (firstRef) = findElementAndDataInEditor<SolNamedElement>("^")
        val firstResolved = checkNotNull(firstRef.reference?.resolve()) {
            "Failed to resolve ${firstRef.text}"
        }
        assertTrue(firstResolved.containingFile.virtualFile.path.replace("\\", "/").endsWith("lib/a/src/Test.sol"))

        WriteCommandAction.runWriteCommandAction(project) {
            VfsUtil.saveText(
                foundryToml.virtualFile,
                """
                [profile.default]
                remappings = ["forge-std/=lib/b/src/"]
                """.trimIndent()
            )
        }

        val (secondRef) = findElementAndDataInEditor<SolNamedElement>("^")
        val secondResolved = checkNotNull(secondRef.reference?.resolve()) {
            "Failed to resolve ${secondRef.text}"
        }
        assertTrue(secondResolved.containingFile.virtualFile.path.replace("\\", "/").endsWith("lib/b/src/Test.sol"))
    }

    fun testImportPathResolveFoundryRemappingsCacheInvalidation() {
        myFixture.addFileToProject("lib/first/src/Test.sol", "contract A {}")
        myFixture.addFileToProject("lib/second/src/Test.sol", "contract B {}")
        val remappings = myFixture.addFileToProject(
            "remappings.txt",
            "forge-std/=lib/first/src/"
        )
        val usage = myFixture.addFileToProject(
            "contracts/ImportUsageFoundryCacheRemappings.sol",
            """
            import "forge-std/Test.sol";
                   //^
            
            contract ImportUsage {}
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(usage.virtualFile)

        val (firstRef) = findElementAndDataInEditor<SolNamedElement>("^")
        val firstResolved = checkNotNull(firstRef.reference?.resolve()) {
            "Failed to resolve ${firstRef.text}"
        }
        assertTrue(firstResolved.containingFile.virtualFile.path.replace("\\", "/").endsWith("lib/first/src/Test.sol"))

        WriteCommandAction.runWriteCommandAction(project) {
            VfsUtil.saveText(remappings.virtualFile, "forge-std/=lib/second/src/")
        }

        val (secondRef) = findElementAndDataInEditor<SolNamedElement>("^")
        val secondResolved = checkNotNull(secondRef.reference?.resolve()) {
            "Failed to resolve ${secondRef.text}"
        }
        assertTrue(secondResolved.containingFile.virtualFile.path.replace("\\", "/").endsWith("lib/second/src/Test.sol"))
    }

    fun testImportPathResolveFoundryUsesLongestPrefixRemapping() {
        myFixture.addFileToProject("lib/base/src/My.sol", "contract BaseLib {}")
        myFixture.addFileToProject("lib/specific/src/My.sol", "contract SpecificLib {}")
        myFixture.addFileToProject(
            "foundry.toml",
            """
            [profile.default]
            remappings = [
                "foo/=lib/base/src/",
                "foo/bar/=lib/specific/src/"
            ]
            """.trimIndent()
        )
        val usage = myFixture.addFileToProject(
            "contracts/ImportUsageFoundryLongestPrefix.sol",
            """
            import "foo/bar/My.sol";
                   //^
            
            contract ImportUsage {}
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(usage.virtualFile)

        val (refElement) = findElementAndDataInEditor<SolNamedElement>("^")
        val resolved = checkNotNull(refElement.reference?.resolve()) {
            "Failed to resolve ${refElement.text}"
        }
        assertTrue(resolved.containingFile.virtualFile.path.replace("\\", "/").endsWith("lib/specific/src/My.sol"))
    }

    fun testImportPathResolveFoundryDoesNotRemapSubstring() {
        myFixture.addFileToProject("pkg/lib/foo/src/Test.sol", "contract Wrong {}")
        myFixture.addFileToProject("remappings.txt", "foo/=lib/foo/src/")
        val usage = myFixture.addFileToProject(
            "contracts/ImportUsageFoundryNoContains.sol",
            """
            import "pkg/foo/Test.sol";
                   //^ unresolved
            
            contract ImportUsage {}
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(usage.virtualFile)

        val (refElement) = findElementAndDataInEditor<SolNamedElement>("^")
        check(refElement.reference?.resolve() == null) {
            "Should fail to resolve ${refElement.text}"
        }
    }

    fun testImportPathResolveFoundryConfigCreationAndDeletionInvalidatesCache() {
        myFixture.addFileToProject("vendor/src/Test.sol", "contract Target {}")
        val usage = myFixture.addFileToProject(
            "contracts/ImportUsageFoundryConfigLifecycle.sol",
            """
            import "x/Test.sol";
                   //^
            
            contract ImportUsage {}
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(usage.virtualFile)

        val (beforeRef) = findElementAndDataInEditor<SolNamedElement>("^")
        check(beforeRef.reference?.resolve() == null) {
            "Should fail to resolve ${beforeRef.text}"
        }

        val remappings = myFixture.addFileToProject("remappings.txt", "x/=vendor/src/")
        val (afterCreateRef) = findElementAndDataInEditor<SolNamedElement>("^")
        val afterCreateResolved = checkNotNull(afterCreateRef.reference?.resolve()) {
            "Failed to resolve ${afterCreateRef.text}"
        }
        assertTrue(afterCreateResolved.containingFile.virtualFile.path.replace("\\", "/").endsWith("vendor/src/Test.sol"))

        WriteCommandAction.runWriteCommandAction(project) {
            remappings.virtualFile.delete(this)
        }

        val (afterDeleteRef) = findElementAndDataInEditor<SolNamedElement>("^")
        check(afterDeleteRef.reference?.resolve() == null) {
            "Should fail to resolve ${afterDeleteRef.text} after deleting remappings"
        }
    }

    override fun getTestDataPath() = "src/test/resources/fixtures/importRemappings/"

    private fun testImportPathResolveWithConfig(configFileName: String) {
        testcases.forEach { (targetFile, contractFile) ->
            val expectedFile = myFixture.configureByFile(targetFile)
            myFixture.configureByFile(configFileName)
            myFixture.configureByFile(contractFile)

            val (refElement) = findElementAndDataInEditor<SolNamedElement>("^")
            val resolved = checkNotNull(refElement.reference?.resolve()) {
                "Failed to resolve ${refElement.text}"
            }

            assertEquals(expectedFile.name, resolved.containingFile.name)
        }
    }

}
