val VERSION = "0.0.1-alpha01"

// Make sure these are always in sync
val javaVersion = JavaVersion.VERSION_17
val javaLanguageVersion: JavaLanguageVersion = JavaLanguageVersion.of(17)

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow) apply false
}

java {
    toolchain {
        languageVersion.set(javaLanguageVersion)
    }
}

allprojects {
    version = resolveVersion(VERSION)
}

subprojects {
    tasks.withType(Test::class.java) {
        useJUnitPlatform()
    }

    tasks.register("allDeps", DependencyReportTask::class)
}

fun resolveVersion(originalVersionName: String): String {
    val snapshotVersion = System.getenv("SNAPSHOT_VERSION")

    return if (!snapshotVersion.isNullOrBlank()) {
        buildString {
            append(originalVersionName)
            append("-")
            append(snapshotVersion)
            append("-SNAPSHOT")
        }
    } else {
        originalVersionName
    }
}
