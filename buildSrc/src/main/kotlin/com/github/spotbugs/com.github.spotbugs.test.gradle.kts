import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    `jacoco`
    java
    id("org.sonarqube")
}

val junitVersion = "5.8.1"
dependencies {
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
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
    withType<Test> {
        useJUnitPlatform()
        maxParallelForks = Runtime.getRuntime().availableProcessors()
    }
    named("sonarqube") {
        mustRunAfter(jacocoTestReport)
    }
}
tasks.check {
    dependsOn(jacocoTestReport)
}