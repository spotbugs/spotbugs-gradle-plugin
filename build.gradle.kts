plugins {
    groovy
    `java-gradle-plugin`
    jacoco
    signing
    kotlin("jvm") version "1.9.21"
    id("org.jetbrains.dokka") version "1.9.10"
    id("com.github.spotbugs.gradle-plugin")
    id("com.github.spotbugs.plugin-publish")
    id("com.github.spotbugs.test")
    id("org.sonarqube")
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

group = "com.github.spotbugs.snom"

val spotBugsVersion = "4.8.2"
val slf4jVersion = "2.0.0"
val androidGradlePluginVersion = "8.2.0"

dependencies {
    compileOnly(localGroovy())
    compileOnly("com.github.spotbugs:spotbugs:$spotBugsVersion")
    compileOnly("com.android.tools.build:gradle:$androidGradlePluginVersion")
    testImplementation("com.tngtech.archunit:archunit:1.2.0")
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
    named<io.gitlab.arturbosch.detekt.Detekt>("detekt") {
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
    javadoc {
        enabled = false
    }
}

defaultTasks(tasks.spotlessApply.name, tasks.build.name)
