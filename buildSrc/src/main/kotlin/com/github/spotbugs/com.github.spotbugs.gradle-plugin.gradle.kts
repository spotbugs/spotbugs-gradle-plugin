plugins {
    id("com.diffplug.spotless")
}

// TODO introduce KDoc

spotless {
    java {
        licenseHeaderFile("gradle/HEADER.txt")
        removeUnusedImports()
        googleJavaFormat()
    }
    groovy {
        licenseHeaderFile("gradle/HEADER.txt")
        target("**/*.groovy")
        greclipse()
        indentWithSpaces()
    }
    groovyGradle {
        target("**/*.gradle")
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
