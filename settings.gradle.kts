pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("com.gradle.enterprise") version "3.16"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
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

val isCiBuild = providers.environmentVariable("CI").isPresent

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        publishAlwaysIf(isCiBuild)
    }
}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
