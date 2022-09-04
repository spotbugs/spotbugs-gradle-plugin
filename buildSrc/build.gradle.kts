plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "5.16.0"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:5.16.0")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:2.0.2")
    implementation("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:3.4.0.2513")
}

spotless {
    kotlinGradle {
        ktlint()
    }
}
