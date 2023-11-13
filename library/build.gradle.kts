plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    application
}

group = "com.anifichadia.figmaimporter"
version = "1.0-SNAPSHOT"

dependencies {
    api(libs.bundles.kotlin)
    api(libs.kotlinx.serialization)
    implementation(libs.kotlinLogging)

    api(libs.bundles.ktor.client)

    implementation(libs.arrow.core)
    implementation(libs.arrow.optics)
    ksp(libs.arrow.optics.ksp.plugin)

    implementation(libs.bundles.androidTools)

    testImplementation(libs.bundles.unitTest.jvm)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(16)
}
