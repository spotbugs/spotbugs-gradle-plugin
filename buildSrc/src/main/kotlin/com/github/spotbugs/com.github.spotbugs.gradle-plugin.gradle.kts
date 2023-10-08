import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
}

tasks.withType<JavaCompile>().configureEach {
    // disable warnings in generated code by immutables
    // https://github.com/google/error-prone/issues/329
    options.errorprone.disableWarningsInGeneratedCode = true
}

tasks.withType<Groovydoc>().configureEach {
    docTitle = "SpotBugs Gradle Plugin"
    link("https://docs.gradle.org/current/javadoc/", "org.gradle.api.")
    link("https://docs.oracle.com/en/java/javase/11/docs/api/", "java.")
    link("https://docs.groovy-lang.org/latest/html/gapi/", "groovy.", "org.codehaus.groovy.")
}

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
    kotlinGradle {
        ktlint()
    }
}
