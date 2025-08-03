plugins {
    alias(libs.plugins.spotless)
}

spotless {
    groovy {
        licenseHeaderFile(rootProject.file("gradle/HEADER.txt"))
        target("**/*.groovy")
        greclipse()
        leadingTabsToSpaces()
    }
    kotlin {
        ktlint()
    }
    kotlinGradle {
        ktlint()
    }
}
