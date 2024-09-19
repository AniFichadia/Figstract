plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.anifichadia.figstract.lib.ios"

dependencies {
    implementation(project(":library-core"))

    implementation(libs.bundles.kotlin)

    implementation(libs.bundles.scrimage)

    testImplementation(libs.bundles.unitTest.jvm)
}
