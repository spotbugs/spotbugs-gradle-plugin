import net.ltgt.gradle.errorprone.errorprone

plugins {
    jacoco
    `java-library`
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
    id("org.sonarqube")
}

val junitVersion = "5.8.1"

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
            dependencies {
                implementation(gradleTestKit())
                implementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
                implementation("org.junit.jupiter:junit-jupiter-params:${junitVersion}")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
            }
            targets {
                all {
                    testTask.configure {
                        maxParallelForks = Runtime.getRuntime().availableProcessors()
                    }
                }
            }
        }
    }
}

val jacocoTestReport = tasks.named("jacocoTestReport", JacocoReport::class) {
    reports {
        xml.required.set(true)
    }
}

tasks.sonarqube {
    mustRunAfter(tasks.jacocoTestReport)
}

tasks.check {
    dependsOn(jacocoTestReport)
}

tasks.withType<JavaCompile>().configureEach {
    // disable warnings in generated code by immutables
    // https://github.com/google/error-prone/issues/329
    options.errorprone.disableWarningsInGeneratedCode.set(true)
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
}

sonarqube {
    properties {
        property("sonar.projectKey", "com.github.spotbugs.gradle")
        property("sonar.organization", "spotbugs")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.coverage.jacoco.xmlReportPaths", jacocoTestReport.map { it.reports.xml.outputLocation })
    }
}
