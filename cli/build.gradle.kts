import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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
