plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    application
}

group = "com.anifichadia.figmaimporter"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(libs.bundles.kotlin)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.cli)
    implementation(libs.kotlinLogging)

    implementation(libs.bundles.ktor.client)

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

application {
    mainClass.set("com.anifichadia.figmaimporter.MainKt")
}
