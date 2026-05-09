import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SourcesJar

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

mavenPublishing {
    configure(
        KotlinJvm(
            javadocJar = JavadocJar.Dokka(tasks.dokkaGenerateModuleJavadoc),
            sourcesJar = SourcesJar.Sources(),
        )
    )
}
