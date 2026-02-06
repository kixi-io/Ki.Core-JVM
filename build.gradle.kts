import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.0.20"
    `java-library`
    `maven-publish`
    id("org.jetbrains.dokka") version "1.9.20"
    signing
}

group = "io.kixi"
version = "2.3.1"
description = "ki-core"

repositories {
    mavenCentral()
}

dependencies {
    // Kotest for testing (v2 choice - comprehensive BDD-style testing)
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")

    // Optional: Property-based testing (great for testing Range, Version, etc.)
    testImplementation("io.kotest:kotest-property:5.9.1")
}

kotlin {
    jvmToolchain(21)
}

// ============================================================================
// Testing
// ============================================================================

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

// ============================================================================
// Java & Kotlin Configuration
// ============================================================================

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        allWarningsAsErrors.set(false) // Set to true for stricter builds
    }
}

// ============================================================================
// Documentation (Dokka)
// ============================================================================

tasks.register<Jar>("dokkaHtmlJar") {
    dependsOn(tasks.dokkaHtml)
    from(tasks.dokkaHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-docs")
}

tasks.register<Jar>("dokkaJavadocJar") {
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

// ============================================================================
// Testing (Kotest runs on JUnit Platform)
// ============================================================================

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

/// /// ///

// ============================================================================
// Publishing
// ============================================================================

val javadocJar by tasks.named("dokkaJavadocJar")

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/kixi-io/Ki.Core-JVM")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(javadocJar)

            pom {
                groupId = "${project.group}"
                artifactId = "ki-core"
                name.set("Ki.Core")
                description.set("A JVM implementation of the Ki.Core library - foundation for the Ki ecosystem")
                url.set("https://kixi.io")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://raw.githubusercontent.com/kixi-io/Ki.Core-JVM/master/LICENSE")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("dleuck")
                        name.set("Daniel Leuck")
                    }
                    developer {
                        id.set("singingbush")
                        name.set("Samael")
                    }
                    developer {
                        id.set("alessiostalla")
                        name.set("Alessio Stalla")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/kixi-io/Ki.Core-JVM.git")
                    developerConnection.set("scm:git:ssh://github.com/kixi-io/Ki.Core-JVM.git")
                    url.set("https://github.com/kixi-io/Ki.Core-JVM/")
                }
            }
        }
    }
}

// Uncomment when ready to publish to Maven Central (requires GPG key setup)
// signing {
//     sign(publishing.publications["mavenJava"])
// }

// ============================================================================
// Convenience Tasks
// ============================================================================

tasks.register("buildAll") {
    description = "Builds JAR, sources JAR, and Javadoc JAR"
    dependsOn("jar", "sourcesJar", "dokkaJavadocJar")
}