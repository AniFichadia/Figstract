plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

group = "com.anifichadia.figmaimporter.cli"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":library"))
    implementation(libs.kotlinx.cli)

    testImplementation(libs.bundles.unitTest.jvm)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(16)
}

application {
    mainClass.set("com.anifichadia.figmaimporter.cli.MainKt")
}
