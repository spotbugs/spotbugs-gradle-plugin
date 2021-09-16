plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "5.15.0"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:5.14.3")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:2.0.2")
}

spotless {
    kotlinGradle {
        ktlint()
    }
}
