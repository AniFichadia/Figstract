// Make sure these are always in sync
val javaVersion = JavaVersion.VERSION_16
val javaLanguageVersion: JavaLanguageVersion = JavaLanguageVersion.of(16)

val kotlinLanguageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9
val kotlinLanguageVersionString = kotlinLanguageVersion.version

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.anifichadia.figmaimporter"
version = "0.0.1-alpha01"

java {
    toolchain {
        languageVersion.set(javaLanguageVersion)
    }
}

kotlin {
    jvmToolchain(16)

    sourceSets.all {
        languageSettings {
            languageVersion = kotlinLanguageVersionString
        }
    }
}

subprojects {
    tasks.withType(Test::class.java) {
        useJUnitPlatform()
    }

    tasks.register("allDeps", DependencyReportTask::class)
}
