import java.util.*

// Make sure these are always in sync
val javaVersion = JavaVersion.VERSION_17
val javaLanguageVersion: JavaLanguageVersion = JavaLanguageVersion.of(17)

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.dokka)
    `maven-publish`
    signing
}

java {
    toolchain {
        languageVersion.set(javaLanguageVersion)
    }
}

subprojects {
    version = resolveVersion()

    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "org.jetbrains.dokka")

    tasks.withType(Test::class.java) {
        useJUnitPlatform()
    }

    tasks.register("allDeps", DependencyReportTask::class)

    //region Publishing
    tasks.register<Jar>("sourceJar") {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }

    tasks.register<Jar>("javadocJar") {
        archiveClassifier.set("javadoc")
        from(tasks.javadoc)
    }

    tasks.register<Jar>("dokkaJar") {
        archiveClassifier.set("javadoc")
        from(tasks.dokkaHtml)
    }

    afterEvaluate {
        publishing {
            repositories {
                maven {
                    name = "SonatypeSnapshots"
                    url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                    credentials {
                        username = System.getenv("SONATYPE_USERNAME")
                        password = System.getenv("SONATYPE_PASSWORD")
                    }
                }

                repositories {
                    maven {
                        name = "GitHubPackages"
                        url = uri("https://maven.pkg.github.com/AniFichadia/Figstract")
                        credentials {
                            username = System.getenv("GITHUB_MAVEN_USERNAME")
                            password = System.getenv("GITHUB_MAVEN_PASSWORD")
                        }
                    }
                }
            }

            publications {
                create<MavenPublication>("mavenJava") {
                    groupId = property("GROUP") as String
                    version = resolveVersion()

                    from(components["java"])

                    artifact(tasks["sourceJar"])
                    artifact(tasks["dokkaJar"])

                    pom {
                        name.set(project.name)
                        // TODO: set up POM description
                        // description.set("TODO")
                        url.set("https://github.com/AniFichadia/Figstract")

                        // TODO: configure POM licenses

                        scm {
                            connection.set("scm:git:git://github.com/AniFichadia/Figstract.git")
                            developerConnection.set("scm:git:ssh://github.com/AniFichadia/Figstract.git")
                            url.set("https://github.com/AniFichadia/Figstract")
                        }
                    }
                }
            }
        }

        signing {
            val encodedKey = System.getenv("GPG_PRIVATE_KEY")
            val signingKey: String? = encodedKey?.let { String(Base64.getDecoder().decode(it)) }
            val signingPassword: String? = System.getenv("GPG_KEY_PASSWORD")

            if (!signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
                useInMemoryPgpKeys(
                    signingKey,
                    signingPassword,
                )
                sign(publishing.publications)
            }
        }
    }
    //endregion
}

fun Project.resolveVersion(): String {
    val originalVersionName = property("VERSION_NAME") as String
    val snapshotVersion = System.getenv("SNAPSHOT_VERSION")

    return if (snapshotVersion != null) {
        buildString {
            append(originalVersionName)
            append("-SNAPSHOT-")
            append(snapshotVersion)
        }
    } else {
        originalVersionName
    }
}
