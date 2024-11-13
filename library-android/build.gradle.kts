plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "com.anifichadia.figstract.lib.android"

dependencies {
    implementation(project(":library-core"))
    implementation(libs.bundles.kotlin)

    implementation(libs.bundles.androidTools)
    implementation(libs.bundles.scrimage)

    implementation(libs.kotlinPoet)

    testImplementation(libs.bundles.unitTest.jvm)
}
