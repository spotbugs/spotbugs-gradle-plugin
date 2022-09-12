import net.ltgt.gradle.errorprone.errorprone

plugins {
    jacoco
    `java-library`
    `java-gradle-plugin`
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
    id("org.sonarqube")
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
            dependencies {
                implementation(gradleTestKit())
            }
            targets {
                all {
                    testTask.configure {
                        maxParallelForks = Runtime.getRuntime().availableProcessors()
                    }
                }
            }
        }
        val functionalTest by registering(JvmTestSuite::class) {
            useSpock()
            targets {
                all {
                    testTask.configure {
                        description = "Runs the functional tests."
                        group = "verification"
                        testClassesDirs = functionalTest.output.classesDirs
                        classpath = functionalTest.runtimeClasspath
                        systemProperty("snom.test.functional.gradle", System.getProperty("snom.test.functional.gradle", gradle.gradleVersion))
                    }
                }
            }
        }
    }
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])

val functionalTest: SourceSet by sourceSets.getting {
    compileClasspath += sourceSets["main"].output
    runtimeClasspath += output + compileClasspath
}

gradlePlugin {
    testSourceSets(functionalTest)
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
