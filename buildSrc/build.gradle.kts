plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "8.0.0"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.0.0")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:4.3.0")
    implementation("com.gradle.publish:plugin-publish-plugin:2.0.0")
    implementation("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:7.0.1.6134")
}

spotless {
    kotlinGradle {
        ktlint()
    }
}
