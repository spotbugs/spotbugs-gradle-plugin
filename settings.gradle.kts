pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

plugins {
    alias(libs.plugins.develocity)
    alias(libs.plugins.foojay.resolver)
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
