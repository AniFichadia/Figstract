plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.anifichadia.figstract"
version = "0.0.1-alpha01"

dependencies {
    implementation(libs.bundles.kotlin)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinLogging)

    api(libs.bundles.ktor.client)

    implementation(libs.bundles.scrimage)

    implementation(libs.jsonPath)

    testImplementation(libs.bundles.unitTest.jvm)
}
