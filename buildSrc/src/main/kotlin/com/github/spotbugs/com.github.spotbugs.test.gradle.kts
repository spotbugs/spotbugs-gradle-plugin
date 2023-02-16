import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    `jacoco`
    java
    `java-gradle-plugin`
    id("org.sonarqube")
}

val junitVersion = "5.8.1"
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
        @Suppress("UNUSED_VARIABLE")
        val functionalTest by registering(JvmTestSuite::class) {
            useSpock()
            testType.set(TestSuiteType.FUNCTIONAL_TEST)
            targets {
                all {
                    testTask.configure {
                        description = "Runs the functional tests."
                        systemProperty("snom.test.functional.gradle", System.getProperty("snom.test.functional.gradle", gradle.gradleVersion))
                    }
                }
            }
        }
    }
}

gradlePlugin {
    testSourceSets(sourceSets["functionalTest"])
}

val jacocoTestReport = tasks.named<JacocoReport>("jacocoTestReport") {
    reports.named("xml") {
            required.set(true)
    }
}

sonarqube {
    properties {
        property("sonar.projectKey", "com.github.spotbugs.gradle")
        property("sonar.organization", "spotbugs")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.coverage.jacoco.xmlReportPaths", jacocoTestReport.flatMap { it.reports.xml.outputLocation })
    }
}

tasks {
    named("sonarqube") {
        mustRunAfter(jacocoTestReport)
    }
}
tasks.check {
    dependsOn(jacocoTestReport)
}
