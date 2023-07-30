plugins {
    id("com.gradle.plugin-publish")
    `java-gradle-plugin`
    `maven-publish`
}

gradlePlugin {
    website.set("https://github.com/spotbugs/spotbugs-gradle-plugin")
    vcsUrl.set("https://github.com/spotbugs/spotbugs-gradle-plugin")
    plugins {
        create("spotbugsGradleBasePlugin") {
            id = "com.github.spotbugs-base"
            displayName = "Official SpotBugs Gradle Base Plugin"
            description = "A base Gradle plugin that runs static bytecode analysis by SpotBugs"
            implementationClass = "com.github.spotbugs.snom.SpotBugsBasePlugin"
            tags.set(listOf(
                "spotbugs",
                "static analysis",
                "code quality"
            ))
        }
        create("spotbugsGradlePlugin") {
            id = "com.github.spotbugs"
            displayName = "Official SpotBugs Gradle Plugin"
            description = "A Gradle plugin that runs static bytecode analysis by SpotBugs"
            implementationClass = "com.github.spotbugs.snom.SpotBugsPlugin"
            tags.set(listOf(
                "spotbugs",
                "static analysis",
                "code quality"
            ))
        }
    }
}

publishing.publications.withType<MavenPublication> {
    pom {
        scm {
            connection.set("scm:git:git@github.com:spotbugs/spotbugs-gradle-plugin.git")
            developerConnection.set("scm:git:git@github.com:spotbugs/spotbugs-gradle-plugin.git")
            url.set("https://github.com/spotbugs/spotbugs-gradle-plugin/")
        }
        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("https://spdx.org/licenses/Apache-2.0.html")
            }
        }
    }
}
