pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

plugins {
    id("com.gradle.develocity") version "3.17.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositories {
        // To download the Android Gradle Plugin
        google {
            content {
                includeGroupByRegex(".*android.*")
            }
        }
        // To download trove4j required by the Android Gradle Plugin
        mavenCentral()
    }
}

rootProject.name = "spotbugs-gradle-plugin"

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
        // TODO: workaround for https://github.com/gradle/gradle/issues/22879.
        val isCI = providers.environmentVariable("CI").isPresent
        publishing.onlyIf { isCI }
    }
}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
