plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:5.14.3")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:2.0.2")
}
