plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.20"
    id("org.jetbrains.intellij.platform") version "2.13.1"
}

group = "io.identificator"
version = "0.0.1"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        datagrip("2026.1.1")
        bundledPlugin("com.intellij.database")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "261"
        }

        changeNotes = """
            Initial skeleton for DataGrip column sorter plugin.
        """.trimIndent()
    }
}