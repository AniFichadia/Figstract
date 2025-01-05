import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    application
}

group = "com.anifichadia.figstract.cli"

dependencies {
    implementation(project(":cli-core"))

    implementation(libs.kotlinLogging)

    testImplementation(libs.bundles.unitTest.jvm)
}

application {
    mainClass.set("com.anifichadia.figstract.cli.MainKt")
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()
    // TODO: enable minimize()
}

mavenPublishing {
    configure(
        KotlinJvm(
            javadocJar = JavadocJar.Dokka(tasks.dokkaGenerateModuleJavadoc.name),
            sourcesJar = true,
        )
    )
}
