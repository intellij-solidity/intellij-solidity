import org.jetbrains.intellij.platform.gradle.TestFrameworkType

val JVM_TARGET = 17

buildscript {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.properties["kotlin_version"]}")
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm").version("1.8.10")
    id("org.jetbrains.grammarkit").version("2022.3.1")
    id("org.jetbrains.intellij.platform") version "2.3.0"
    id("org.jetbrains.intellij.platform.migration").version("2.3.0")
    id("net.researchgate.release").version("3.0.2")
    id("jacoco")
}

repositories {
    mavenCentral()
    maven("https://www.jetbrains.com/intellij-repository/releases")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaUltimate("2023.1")
//        webstorm("2023.1")
        bundledPlugin("JavaScript")
        testFramework(TestFrameworkType.Platform)
    }
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${project.properties["kotlin_version"]}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${project.properties["kotlin_version"]}")
    implementation("io.sentry:sentry:${project.properties["sentry_version"]}")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:2.13.0")

    testImplementation("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(JVM_TARGET)
}

idea {
    module {
        generatedSourceDirs.add(file("gen"))
    }
}

sourceSets {
    main {
        java {
            srcDirs("gen")
        }
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "Intellij-Solidity"
    }
}

configurations {
    implementation {
        exclude("org.slf4j")
    }
    runtimeOnly {
        exclude("org.slf4j")
    }
}

tasks {
    clean {
        doFirst {
            delete("gen")
        }
    }

    runIde {
        maxHeapSize = "1G"
    }

    release {
        newVersionCommitMessage = "[Intellij-Solidity Release] - "
        preTagCommitMessage = "[Intellij-Solidity Release] - pre tag commit: "
        buildTasks.set(listOf("buildPlugin"))
        git {
            requireBranch.set("master")
        }
    }
    generateLexer {
        val solLexerName = "_SolidityLexer"
        sourceFile.set(file("${project.projectDir}/src/main/grammars/${solLexerName}.flex"))
        targetDir.set("gen/me/serce/solidity")
        targetClass.set(solLexerName)
        skeleton.set(file(("src/main/grammars/idea-flex.skeleton")))
        purgeOldFiles.set(true)
    }

    generateParser {
        val pkg = "me/serce/solidity"
        sourceFile.set(file("${project.projectDir}/src/main/grammars/solidity.bnf"))
        targetRoot.set("gen")
        pathToParser.set("$pkg/SolidityParser.java")
        pathToPsiRoot.set("$pkg/psi")
        purgeOldFiles.set(true)

        outputs.dir(file("$targetRoot/$pkg"))
    }

    val codegenTask = register("codegen") {
        dependsOn(generateLexer, generateParser)
    }

    compileKotlin {
        kotlinOptions.jvmTarget = JVM_TARGET.toString()
        dependsOn(codegenTask)
    }

    compileTestKotlin {
        kotlinOptions.jvmTarget = JVM_TARGET.toString()
    }

    // codecov
// The below configuration fixes the 0% coverage issue, see
// https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin-faq.html#jacoco-reports-0-coverage

    test {
        configure<JacocoTaskExtension> {
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }
    }

    jacocoTestCoverageVerification {
        classDirectories.setFrom(instrumentCode)
    }

    jacocoTestReport {
        classDirectories.setFrom(instrumentCode)
        reports {
            xml.required = true
            html.required = true
        }
    }

    check {
        dependsOn(jacocoTestReport)
    }
}
