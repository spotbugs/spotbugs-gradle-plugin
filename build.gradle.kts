plugins {
    groovy
    `java-gradle-plugin`
    jacoco
    signing
    id("com.github.spotbugs.gradle-plugin")
    id("com.github.spotbugs.plugin-publish")
    id("com.github.spotbugs.test")
    id("org.sonarqube")
    id("com.github.spotbugs") version "5.1.3"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
group = "com.github.spotbugs.snom"

repositories {
    // To download the Android Gradle Plugin
    google()
    // To download trove4j required by the Android Gradle Plugin
    mavenCentral()
}

val errorproneVersion = "2.21.1"
val spotBugsVersion = "4.7.3"
val slf4jVersion = "2.0.0"
val androidGradlePluginVersion = "8.1.1"

dependencies {
    errorprone("com.google.errorprone:error_prone_core:$errorproneVersion")
    compileOnly(localGroovy())
    compileOnly("com.github.spotbugs:spotbugs:$spotBugsVersion")
    compileOnly("com.android.tools.build:gradle:$androidGradlePluginVersion")
    testImplementation("com.tngtech.archunit:archunit:1.1.0")
}

val signingKey: String? = System.getenv("SIGNING_KEY")
val signingPassword: String? = System.getenv("SIGNING_PASSWORD")

signing {
    if (!signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(configurations.archives.get())
    } else {
        logger.warn("The signing key and password are null. This can be ignored if this is a pull request.")
    }
}

spotbugs {
    ignoreFailures.set(true)
}
tasks {
    named<com.github.spotbugs.snom.SpotBugsTask>("spotbugsMain") {
        reports {
            register("sarif") {
                required.set(true)
            }
        }
    }
    val processVersionFile by registering(WriteProperties::class) {
        outputFile = file("src/main/resources/spotbugs-gradle-plugin.properties")

        property("slf4j-version", slf4jVersion)
        property("spotbugs-version", spotBugsVersion)
    }
    named("processResources") {
        dependsOn(processVersionFile)
    }
    withType<Jar> {
        dependsOn(processResources)
    }
}

defaultTasks("spotlessApply", "build")
