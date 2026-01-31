plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "8.2.1"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.2.1")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:5.0.0")
    implementation("com.gradle.publish:plugin-publish-plugin:2.0.0")
    implementation("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:7.2.2.6593")
}

spotless {
    kotlinGradle {
        ktlint()
    }
}
