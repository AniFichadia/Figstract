import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import java.util.*

val VERSION = "0.0.1-alpha01"

// Make sure these are always in sync
val javaVersion = JavaVersion.VERSION_17
val javaLanguageVersion: JavaLanguageVersion = JavaLanguageVersion.of(17)

val githubAuthor = "AniFichadia"
val githubRepo = "Figstract"

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
    `maven-publish`
    signing
}

java {
    toolchain {
        languageVersion.set(javaLanguageVersion)
    }
}

allprojects {
    version = resolveVersion(VERSION)
}

subprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "com.vanniktech.maven.publish")

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

    //region Property remappings
    remapSystemPropertyProperty("GPG_PRIVATE_KEY", "ORG_GRADLE_PROJECT_signingInMemoryKey")
    remapSystemPropertyProperty("GPG_PRIVATE_KEY", "signingInMemoryKey")
    remapSystemPropertyProperty("GPG_KEY_PASSWORD", "ORG_GRADLE_PROJECT_signingInMemoryKeyPassword")
    remapSystemPropertyProperty("GPG_KEY_PASSWORD", "signingInMemoryKeyPassword")
    //endregion

    mavenPublishing {
        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

        signAllPublications()

        pom { configure() }
    }

    afterEvaluate {
        publishing {
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/AniFichadia/Figstract")
                    credentials {
                        username = System.getenv("GITHUB_ACTOR")
                        password = System.getenv("GITHUB_TOKEN")
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

fun resolveVersion(originalVersionName: String): String {
    val snapshotVersion = System.getenv("SNAPSHOT_VERSION")

    return if (!snapshotVersion.isNullOrBlank()) {
        buildString {
            append(originalVersionName)
            append("-")
            append(snapshotVersion)
            append("-SNAPSHOT")
        }
    } else {
        originalVersionName
    }
}

fun Project.remapSystemPropertyProperty(
    originalName: String,
    newName: String,
    map: (String) -> String = { it },
) {
    val project = this

    System.getenv(originalName)
        ?.let(map)
        ?.let {
            System.setProperty(newName, it)
//            project.setProperty(newName, it)
            project.extraProperties.set(newName, it)
            project.extra[newName] = it
//            project.extensions.extraProperties.set(newName, it)
//            project.extensions.extraProperties[newName] = it
        }
}

fun MavenPom.configure() {
    name.set(project.name)
    description.set("Figstract bridges the process for maintaining design systems (in Figma) between frontend engineers and designers and helps automate mundane upkeep tasks.")
    url.set("https://github.com/$githubAuthor/$githubRepo")

    developers {
        developer {
            id = "AniFichadia"
            name = "Aniruddh Fichadia"
            email = "Ani.Fichadia@gmail.com"
        }
    }

    licenses {
        license {
            name = "$githubRepo license"
            url = "https://github.com/$githubAuthor/$githubRepo/blob/main/LICENSE"
        }
    }

    scm {
        connection.set("scm:git:git://github.com/$githubAuthor/$githubRepo.git")
        developerConnection.set("scm:git:ssh://github.com/$githubAuthor/$githubRepo.git")
        url.set("https://github.com/$githubAuthor/$githubRepo")
    }
}
