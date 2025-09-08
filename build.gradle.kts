import org.jetbrains.intellij.platform.gradle.IntelliJPlatform
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask

val kotlin_version: String by project
val sentry_version: String by project

plugins {
    java
    idea
    jacoco
    kotlin("jvm") version "1.9.25"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
    id("org.jetbrains.intellij.platform") version "2.7.0"
    id("net.researchgate.release") version "3.0.2"
}

val jvmTarget = "17"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = jvmTarget
}

idea {
    module {
        generatedSourceDirs.add(file("gen"))
    }
}

sourceSets {
    main {
        java.srcDir("gen")
    }
}

tasks.named<Delete>("clean") {
    delete("gen")
}

intellijPlatform {
    pluginConfiguration {
        version = project.version.toString()
    }

    pluginVerification {
        // disable the intellij-solidity plugin name warning
        freeArgs.addAll(listOf("-mute", "TemplateWordInPluginName"))
        ides {
            ide(IntelliJPlatformType.IntellijIdeaUltimate, "2024.2.6")
        }
    }
}

grammarKit {
    // intellijRelease.set("2022.2.5")
}

tasks.named<JavaExec>("runIde") {
    maxHeapSize = "1G"
}

tasks.named<VerifyPluginTask>("verifyPlugin") {
    offline.set(true)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

configurations {
    create("lexer")
    create("parser")
    getByName("runtimeClasspath").exclude(group = "org.slf4j")
    getByName("implementation").exclude(group = "org.slf4j")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    implementation("io.sentry:sentry:$sentry_version")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:2.13.0")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.6")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.opentest4j:opentest4j:1.3.0")

    intellijPlatform {
        intellijIdeaUltimate("2024.2.6")
        bundledPlugins("JavaScript")
        testFramework(TestFrameworkType.Platform)
    }
}

release {
    newVersionCommitMessage.set("[Intellij-Solidity Release] - ")
    preTagCommitMessage.set("[Intellij-Solidity Release] - pre tag commit: ")
    buildTasks.set(listOf("buildPlugin"))
    git {
        requireBranch.set("master")
    }
}

val codegen = tasks.register("codegen") {
    dependsOn("generateLexer", "generateParser")
}

tasks.named("compileKotlin") {
    dependsOn(codegen)
}

tasks.named<org.jetbrains.grammarkit.tasks.GenerateLexerTask>("generateLexer") {
    val solLexerName = "_SolidityLexer"
    sourceFile.set(file("src/main/grammars/${solLexerName}.flex"))
    targetOutputDir.set(file("gen/me/serce/solidity"))
    purgeOldFiles.set(true)
}

tasks.named<org.jetbrains.grammarkit.tasks.GenerateParserTask>("generateParser") {
    val pkg = "me/serce/solidity"
    sourceFile.set(file("src/main/grammars/solidity.bnf"))
    targetRootOutputDir.set(file("gen"))
    pathToParser.set("$pkg/SolidityParser.java")
    pathToPsiRoot.set("$pkg/psi")
    purgeOldFiles.set(true)
    outputs.dir(file("gen/$pkg"))
}

// codecov
// The below configuration fixes the 0% coverage issue, see
// https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin-faq.html#jacoco-reports-0-coverage

tasks.test {
    extensions.configure(JacocoTaskExtension::class) {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    // relies on default classDirectories from the IntelliJ plugin instrumentation
}

tasks.named<JacocoReport>("jacocoTestReport") {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.check { dependsOn(tasks.named("jacocoTestReport")) }
