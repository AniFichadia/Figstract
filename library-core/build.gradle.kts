import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.anifichadia.figstract.lib.core"

dependencies {
    implementation(libs.bundles.kotlin)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinLogging)

    api(libs.bundles.ktor.client)

    implementation(libs.bundles.scrimage)

    implementation(libs.jsonPath)

    testImplementation(libs.bundles.unitTest.jvm)
}

mavenPublishing {
    configure(
        KotlinJvm(
            javadocJar = JavadocJar.Dokka("dokkaHtml"),
            sourcesJar = true,
        )
    )
}
