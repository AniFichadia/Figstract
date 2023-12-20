plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

group = "com.anifichadia.figmaimporter.cli"
version = "0.0.1-alpha01"

dependencies {
    implementation(project(":cli-core"))
    implementation(project(":library-android"))
    implementation(project(":library-ios"))

    implementation(libs.kotlinLogging)

    testImplementation(libs.bundles.unitTest.jvm)
}

application {
    mainClass.set("com.anifichadia.figmaimporter.cli.MainKt")
}
