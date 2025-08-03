import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    groovy
    jacoco
    signing
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.dokka)
    id("com.github.spotbugs.gradle-plugin")
    id("com.github.spotbugs.plugin-publish")
    id("com.github.spotbugs.test")
    id("org.sonarqube")
    alias(libs.plugins.detekt)
    alias(libs.plugins.binary.compatibility.validator)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

group = "com.github.spotbugs.snom"

dependencies {
    compileOnly(localGroovy())
    compileOnly(libs.spotbugs)
    compileOnly(libs.android.gradle.plugin)
    testImplementation(libs.archunit)
    lintChecks(libs.androidx.lint.gradle)
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
        options.release.set(11)
    }
    withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    named<Detekt>("detekt") {
        reports {
            sarif.required = true
        }
    }
    val processVersionFile by registering(WriteProperties::class) {
        destinationFile = file("src/main/resources/spotbugs-gradle-plugin.properties")

        property("slf4j-version", libs.versions.slf4j.get())
        property("spotbugs-version", libs.versions.spotbugs.get())
    }
    processResources {
        dependsOn(processVersionFile)
    }
    withType<Jar>().configureEach {
        dependsOn(processResources)
    }
    javadoc {
        enabled = false
    }
}

defaultTasks(tasks.spotlessApply.name, tasks.build.name)
