plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "7.0.3"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.25.0")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:4.2.0")
    implementation("com.gradle.publish:plugin-publish-plugin:1.3.1")
    implementation("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:6.2.0.5505")
}

spotless {
    kotlinGradle {
        ktlint()
    }
}
