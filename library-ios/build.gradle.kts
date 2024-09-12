plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.anifichadia.figstract.ios"
version = "0.0.1-alpha01"

dependencies {
    implementation(project(":library-core"))

    implementation(libs.bundles.kotlin)

    implementation(libs.bundles.scrimage)

    testImplementation(libs.bundles.unitTest.jvm)
}
