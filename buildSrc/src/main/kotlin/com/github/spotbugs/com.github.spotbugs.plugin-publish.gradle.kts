plugins {
    id("com.gradle.plugin-publish")
    `java-gradle-plugin`
    `maven-publish`
}

gradlePlugin {
    website = "https://github.com/spotbugs/spotbugs-gradle-plugin"
    vcsUrl = "https://github.com/spotbugs/spotbugs-gradle-plugin"
    plugins {
        create("spotbugsGradleBasePlugin") {
            id = "com.github.spotbugs-base"
            displayName = "Official SpotBugs Gradle Base Plugin"
            description = "A base Gradle plugin that runs static bytecode analysis by SpotBugs"
            implementationClass = "com.github.spotbugs.snom.SpotBugsBasePlugin"
            tags = listOf(
                "spotbugs",
                "static analysis",
                "code quality"
            )
        }
        create("spotbugsGradlePlugin") {
            id = "com.github.spotbugs"
            displayName = "Official SpotBugs Gradle Plugin"
            description = "A Gradle plugin that runs static bytecode analysis by SpotBugs"
            implementationClass = "com.github.spotbugs.snom.SpotBugsPlugin"
            tags = listOf(
                "spotbugs",
                "static analysis",
                "code quality"
            )
        }
    }
}

publishing.publications.withType<MavenPublication> {
    pom {
        scm {
            connection = "scm:git:git@github.com:spotbugs/spotbugs-gradle-plugin.git"
            developerConnection = "scm:git:git@github.com:spotbugs/spotbugs-gradle-plugin.git"
            url = "https://github.com/spotbugs/spotbugs-gradle-plugin/"
        }
        licenses {
            license {
                name = "Apache License 2.0"
                url = "https://spdx.org/licenses/Apache-2.0.html"
            }
        }
    }
}
