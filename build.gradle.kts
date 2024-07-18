// Make sure these are always in sync
val javaVersion = JavaVersion.VERSION_17
val javaLanguageVersion: JavaLanguageVersion = JavaLanguageVersion.of(17)

val kotlinLanguageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9
val kotlinLanguageVersionString = kotlinLanguageVersion.version

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow) apply false
}

group = "com.anifichadia.figmaimporter"
version = "0.0.1-alpha01"

java {
    toolchain {
        languageVersion.set(javaLanguageVersion)
    }
}

kotlin {
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
