import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

group = "io.kixi"
version = "1.1.0-SNAPSHOT"
description = "ki-core"
val jpmsModuleName = "kixi.ki.core"

plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm") version "2.1.+"
    id("org.jetbrains.dokka") version "2.0.0"
    signing
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    modularity.inferModulePath.set(true)
}
tasks.named("compileJava", JavaCompile::class.java) {
    options.compilerArgumentProviders.add(CommandLineArgumentProvider {
        // Provide compiled Kotlin classes to javac – needed for module-info.java to get compiled
        // due to requirement of mixed Java/Kotlin sources
        listOf("--patch-module", "${jpmsModuleName}=${sourceSets["main"].output.asPath}")
    })
}
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        languageVersion.set(KotlinVersion.KOTLIN_1_9)
        moduleName = jpmsModuleName
        allWarningsAsErrors = true
    }
}

repositories { mavenCentral() }

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(platform("org.junit:junit-bom:5.13.+"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Ensure that artifacts for jar, kotlinSourcesJar, and dokkaJavadocJar
// tasks all start with same text (should be lowercase)
tasks.withType<org.gradle.jvm.tasks.Jar>() {
    archiveBaseName.set("ki-core")
    manifest {
        attributes["Build-Jdk-Spec"] = JavaVersion.current().majorVersion
        attributes["Package"] = "io.kixi.core"
        attributes["Created-By"] = "kixi.io"
    }
}

// use Dokka for generating a javadoc jar
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
tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}

// Allows all artifacts to be built using: "./gradlew buildAll"
tasks.register("buildAll") {
    dependsOn(":jar", ":kotlinSourcesJar", ":dokkaJavadocJar")
}

// todo: publishing to maven central will require signed artifacts
//signing {
//    sign(publishing.publications["mavenJava"])
//}

val sourcesJar by tasks.named("kotlinSourcesJar")
val javadocJar by tasks.named("dokkaJavadocJar")

publishing {
    repositories {
        maven {
            if (System.getenv().containsKey("CI")) {
                // todo: Rather than use GitHub Packages it would be better to publish to maven central (requires signed artifacts)
                name = "GitHub-Packages"
                url = uri("https://maven.pkg.github.com/kixi-io/Ki.Core-JVM/")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            } else {
                val releasesRepoUrl = layout.buildDirectory.dir("repos/releases")
                val snapshotsRepoUrl = layout.buildDirectory.dir("repos/snapshots")
                val localReleasePath = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
                url = uri(localReleasePath)
                name = "local-build-dir"
            }
        }
    }
    publications {
        // This section configures all the required fields that are needed when releasing to a maven repo
        // see: https://docs.gradle.org/current/userguide/publishing_maven.html
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)

            pom {
                groupId = "${project.group}"
                artifactId = "ki-core"
                name = "Ki.Core"
                description = "A JVM implementation of the Ki.Core library"
                url = "http://kixi.io"
                properties = mapOf(
                    "project.build.sourceEncoding" to "UTF-8",
                    "project.reporting.outputEncoding" to "UTF-8",
                    "maven.compiler.source" to JavaVersion.VERSION_11.majorVersion
                )
                licenses {
                    license {
                        name = "MIT License"
                        url = "https://raw.githubusercontent.com/kixi-io/Ki.Core-JVM/master/LICENSE"
                        distribution = "repo"
                    }
                }
                developers {
                    developer {
                        id = "dleuck"
                        name = "Daniel Leuck"
                    }
                    developer {
                        id = "singingbush"
                        name = "Samael"
                    }
                    developer {
                        id = "alessiostalla"
                        name = "Alessio Stalla"
                    }
                }
                issueManagement {
                    system = "Pivotal-Tracker"
                    url = "https://www.pivotaltracker.com/n/projects/2462351"
                }
                scm {
                    connection = "scm:git:git://github.com:kixi-io/Ki.Core-JVM.git"
                    developerConnection = "scm:git:ssh://github.com:kixi-io/Ki.Core-JVM.git"
                    url = "http://github.com/kixi-io/Ki.Core-JVM/"
                }
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()

    val failedTests = mutableListOf<TestDescriptor>()
    val skippedTests = mutableListOf<TestDescriptor>()

    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) {}
        override fun beforeTest(testDescriptor: TestDescriptor) {}
        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
            when (result.resultType) {
                TestResult.ResultType.FAILURE -> failedTests.add(testDescriptor)
                TestResult.ResultType.SKIPPED -> skippedTests.add(testDescriptor)
                else -> Unit
            }
        }
        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent == null) { // root suite
                logger.lifecycle("----")
                logger.lifecycle("Test result: ${result.resultType}")
                logger.lifecycle(
                    "Test summary: ${result.testCount} tests, " +
                            "${result.successfulTestCount} succeeded, " +
                            "${result.failedTestCount} failed, " +
                            "${result.skippedTestCount} skipped")
            }
        }
    })
}
