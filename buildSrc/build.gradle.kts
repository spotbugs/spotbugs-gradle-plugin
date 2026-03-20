plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "8.4.0"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.4.0")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:5.1.0")
    implementation("com.gradle.publish:plugin-publish-plugin:2.1.1")
    implementation("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:7.2.3.7755")
}

spotless {
    kotlinGradle {
        ktlint()
    }
}
