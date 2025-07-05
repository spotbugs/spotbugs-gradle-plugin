import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    `jacoco`
    java
    `java-gradle-plugin`
    id("org.sonarqube")
}

val junitVersion = "5.13.3"
dependencies {
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

testing {
    suites {
        @Suppress("UNUSED_VARIABLE")
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
            dependencies {
                val kotestVersion = "5.9.1"
                implementation(gradleTestKit())
                implementation("io.kotest:kotest-runner-junit5:$kotestVersion")
                implementation("io.kotest:kotest-assertions-core:$kotestVersion")
                implementation("io.kotest:kotest-property:$kotestVersion")
            }
            targets {
                all {
                    testTask.configure {
                        maxParallelForks = Runtime.getRuntime().availableProcessors()
                    }
                }
            }
        }
        @Suppress("UNUSED_VARIABLE")
        val functionalTest by registering(JvmTestSuite::class) {
            useSpock()
            targets {
                all {
                    testTask.configure {
                        description = "Runs the functional tests."
                        var testGradleVersion = providers.gradleProperty("snom.test.functional.gradle").getOrElse("current")
                        if (testGradleVersion == "current") {
                            testGradleVersion = gradle.gradleVersion
                        }
                        systemProperty("gradleVersion", testGradleVersion)
                    }
                }
            }
        }
    }
}

gradlePlugin {
    testSourceSets(sourceSets["functionalTest"])
}

tasks.jacocoTestReport {
    reports.named("xml") {
            required = true
    }
}

sonarqube {
    properties {
        property("sonar.projectKey", "com.github.spotbugs.gradle")
        property("sonar.organization", "spotbugs")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.coverage.jacoco.xmlReportPaths", tasks.jacocoTestReport.flatMap { it.reports.xml.outputLocation })
    }
}

tasks {
    named("sonarqube") {
        mustRunAfter(jacocoTestReport)
    }
}
tasks.check {
    dependsOn(tasks.named("functionalTest"))
    dependsOn(tasks.jacocoTestReport)
}
