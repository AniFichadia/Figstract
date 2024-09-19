plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "com.anifichadia.figstract.cli.core"

dependencies {
    api(project(":library-core"))
    api(project(":library-android"))
    api(project(":library-ios"))

    api(libs.bundles.kotlin)
    api(libs.kotlinx.serialization)
    api(libs.clikt)

    implementation(libs.kotlinLogging)
    implementation(libs.logback)

    testImplementation(libs.bundles.unitTest.jvm)
}
