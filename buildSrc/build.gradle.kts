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
}

spotless {
    kotlinGradle {
        ktlint()
    }
}
