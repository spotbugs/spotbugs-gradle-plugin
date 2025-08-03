plugins {
    `kotlin-dsl`
    alias(libs.plugins.spotless)
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.spotless.plugin.gradle)
    implementation(libs.gradle.errorprone.plugin)
    implementation(libs.plugin.publish.plugin)
    implementation(libs.sonarqube.gradle.plugin)
}

spotless {
    kotlinGradle {
        ktlint()
    }
}

enableFeaturePreview("VERSION_CATALOGS")
