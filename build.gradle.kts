import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    groovy
    jacoco
    signing
    kotlin("jvm") version "2.0.21"
    id("com.android.lint") version "8.8.1"
    id("org.jetbrains.dokka") version "2.0.0"
    id("com.github.spotbugs.gradle-plugin")
    id("com.github.spotbugs.plugin-publish")
    id("com.github.spotbugs.test")
    id("org.sonarqube")
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.17.0"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

group = "com.github.spotbugs.snom"

val spotBugsVersion = "4.8.6"
val slf4jVersion = "2.0.0"
val androidGradlePluginVersion = "8.8.2"

dependencies {
    compileOnly(localGroovy())
    compileOnly("com.github.spotbugs:spotbugs:$spotBugsVersion")
    compileOnly("com.android.tools.build:gradle:$androidGradlePluginVersion")
    testImplementation("com.tngtech.archunit:archunit:1.4.0")
    lintChecks("androidx.lint:lint-gradle:1.0.0-alpha03")
}

val signingKey: String? = providers.environmentVariable("SIGNING_KEY").orNull
val signingPassword: String? = providers.environmentVariable("SIGNING_PASSWORD").orNull

signing {
    if (!signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(configurations.archives.get())
    } else {
        logger.warn("The signing key and password are null. This can be ignored if this is a pull request.")
    }
}

tasks {
    withType<JavaCompile> {
        options.release.set(8)
    }
    withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_1_8
        }
    }
    named<Detekt>("detekt") {
        reports {
            sarif.required = true
        }
    }
    val processVersionFile by registering(WriteProperties::class) {
        destinationFile = file("src/main/resources/spotbugs-gradle-plugin.properties")

        property("slf4j-version", slf4jVersion)
        property("spotbugs-version", spotBugsVersion)
    }
    processResources {
        dependsOn(processVersionFile)
    }
    withType<Jar>().configureEach {
        dependsOn(processResources)
    }
    withType<DokkaTask>().configureEach {
        notCompatibleWithConfigurationCache("https://github.com/Kotlin/dokka/issues/2231")
    }
    javadoc {
        enabled = false
    }
}

defaultTasks(tasks.spotlessApply.name, tasks.build.name)
