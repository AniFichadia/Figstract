import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.anifichadia.figstract.lib.ios"

dependencies {
    implementation(project(":library-core"))

    implementation(libs.bundles.kotlin)

    implementation(libs.bundles.scrimage)

    implementation(libs.swiftPoet)

    testImplementation(libs.bundles.unitTest.jvm)
}

mavenPublishing {
    configure(
        KotlinJvm(
            javadocJar = JavadocJar.Dokka(tasks.dokkaGenerateModuleJavadoc.name),
            sourcesJar = true,
        )
    )
}
