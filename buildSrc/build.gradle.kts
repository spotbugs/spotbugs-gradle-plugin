plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "6.20.0"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.20.0")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:3.1.0")
    implementation("com.gradle.publish:plugin-publish-plugin:1.2.1")
    implementation("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:4.3.0.3225")
}

spotless {
    kotlinGradle {
        ktlint()
    }
}
