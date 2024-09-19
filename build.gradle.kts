// Make sure these are always in sync
val javaVersion = JavaVersion.VERSION_17
val javaLanguageVersion: JavaLanguageVersion = JavaLanguageVersion.of(17)

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.dokka)
}

java {
    toolchain {
        languageVersion.set(javaLanguageVersion)
    }
}

subprojects {
    version = property("VERSION_NAME") as String
    apply(plugin = "org.jetbrains.dokka")

    tasks.withType(Test::class.java) {
        useJUnitPlatform()
    }

    tasks.register("allDeps", DependencyReportTask::class)
}
