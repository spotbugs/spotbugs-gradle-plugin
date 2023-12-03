plugins {
    id("com.diffplug.spotless")
}

spotless {
    groovy {
        licenseHeaderFile(rootProject.file("gradle/HEADER.txt"))
        target("**/*.groovy")
        greclipse()
        indentWithSpaces()
    }
    kotlin {
        ktlint()
    }
    kotlinGradle {
        ktlint()
    }
}
