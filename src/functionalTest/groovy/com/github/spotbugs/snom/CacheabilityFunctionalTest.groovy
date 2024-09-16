/*
 * Copyright 2021 SpotBugs team
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.spotbugs.snom

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import spock.lang.IgnoreIf

import java.nio.file.Files
import java.time.Instant

class CacheabilityFunctionalTest extends BaseFunctionalTest {
    /**
     * @see <a href="https://github.com/spotbugs/spotbugs-gradle-plugin/issues/662">GitHub Issues</a>
     */
    @IgnoreIf({
        def current = System.getProperty('gradleVersion', GradleVersion.current().version)
        return GradleVersion.version(current) < GradleVersion.version("8.1")
    })
    def 'spotbugsMain task runs with configuration cache'() {
        given:
        initializeBuildFile(rootDir, Instant.now())

        when:
        BuildResult result = gradleRunner
                .withArguments(':spotbugsMain')
                .build()

        then:
        !result.output.contains("Configuration cache problems found in this build")
        result.output.contains("Configuration cache entry stored.")

        when:
        BuildResult resultOfCachedBuild = gradleRunner
                .withArguments(':spotbugsMain')
                .build()
        then:
        resultOfCachedBuild.task(":spotbugsMain").outcome == TaskOutcome.UP_TO_DATE
        resultOfCachedBuild.output.contains("Configuration cache entry reused.")
    }

    /**
     * Verifies the cacheability of {@link SpotBugsTask} by invoking the same code
     * in two different, uniquely-named folders.
     *
     * Thanks to the presence of sysprop {@code org.gradle.caching.debug=true}, the
     * build cache key for the task is echoed to stdout.
     *
     * If we compare these keys for both builds and they are equal, we can now trust the
     * task is cacheable.
     *
     */
    def 'spotbugsMain task is cacheable'() {
        given:
        def buildDir1 = Files.createTempDirectory(null).toFile()
        def buildDir2 = Files.createTempDirectory(null).toFile()
        def now = Instant.now()

        initializeBuildFile(buildDir1, now)
        initializeBuildFile(buildDir2, now)

        when:
        BuildResult result1 = gradleRunner
                .withProjectDir(buildDir1)
                .withArguments(':spotbugsMain')
                .build()
        def hashKeyLine1 = getHashKeyLine(result1)

        then:
        hashKeyLine1
        new File(buildDir1, "build/reports/spotbugs/main.txt").exists()

        when:
        BuildResult result2 = gradleRunner
                .withProjectDir(buildDir2)
                .withArguments(':spotbugsMain', '--scan')
                .build()
        def hashKeyLine2 = getHashKeyLine(result2)

        then:
        hashKeyLine2
        hashKeyLine1 == hashKeyLine2
        new File(buildDir2, "build/reports/spotbugs/main.txt").exists()
    }

    def 'spotbugsMain is cacheable even when no report is configured'() {
        given:
        def buildFile = new File(rootDir, "build.gradle")

        initializeBuildFile(rootDir, Instant.now())
        buildFile.write """
            |plugins {
            |    id 'java'
            |    id 'com.github.spotbugs'
            |}
            |
            |version = 1.0
            |
            |repositories {
            |    mavenCentral()
            |}
            |""".stripMargin()

        when:
        gradleRunner
                .withArguments(':spotbugsMain', '--no-configuration-cache', '--build-cache')
                .build()
        BuildResult result = gradleRunner
                .withArguments(':spotbugsMain', '--build-cache')
                .build()

        then:
        result.task(":spotbugsMain").outcome == TaskOutcome.UP_TO_DATE
    }

    /**
     * @see <a href="https://github.com/spotbugs/spotbugs-gradle-plugin/issues/914">GitHub Issues</a>
     */
    @IgnoreIf({ !jvm.java11 })
    def 'spotbugsMain is cacheable even if a stylesheet is set as String for the HTML report'() {
        given:
        def buildFile = new File(rootDir, "build.gradle")

        initializeBuildFile(rootDir, Instant.now())
        buildFile.delete()
        new File(rootDir, "build.gradle.kts") << """
            |import com.github.spotbugs.snom.SpotBugsTask
            |plugins {
            |    `java`
            |    id("com.github.spotbugs")
            |}
            |version = "1.0"
            |repositories {
            |    mavenCentral()
            |}
            |tasks.withType<SpotBugsTask>().configureEach {
            |    reports {
            |        create("html") {
            |            setStylesheet("fancy-hist.xsl")
            |        }
            |    }
            |}
            |""".stripMargin()

        when:
        def result = gradleRunner
                .withArguments(':spotbugsMain')
                .build()

        then:
        !result.output.contains("Configuration cache problems found in this build")
        result.output.contains("Configuration cache entry stored.")
    }

    private static String getHashKeyLine(BuildResult result) {
        return result.output.find('Build cache key for task \':spotbugsMain\' is .*')
    }

    private static void initializeBuildFile(File buildDir, Instant now) {
        File buildFile = new File(buildDir, 'build.gradle')
        File settingsFile = new File(buildDir, 'settings.gradle')
        File propertiesFile = new File(buildDir, 'gradle.properties')

        buildFile << '''
            |plugins {
            |    id 'java'
            |    id 'com.github.spotbugs'
            |}
            |
            |version = 1.0
            |
            |repositories {
            |    mavenCentral()
            |}
            |spotbugsMain {
            |    reports {
            |        text.required = true
            |    }
            |}
            |'''.stripMargin()

        settingsFile << '''
            |plugins {
            |    id "com.gradle.enterprise" version "3.16"
            |}
            |gradleEnterprise {
            |    buildScan {
            |        termsOfServiceUrl = "https://gradle.com/terms-of-service"
            |        termsOfServiceAgree = "yes"
            |    }
            |}
            '''.stripMargin()
        File sourceDir = buildDir.toPath().resolve('src').resolve('main').resolve('java').toFile()
        sourceDir.mkdirs()
        File sourceFile = new File(sourceDir, 'Foo.java')
        sourceFile << """
            |public class Foo {
            |    public static void main(String... args) {
            |        System.out.println("Hello, SpotBugs! ${now}");
            |    }
            |}
            |""".stripMargin()

        propertiesFile << '''
            |org.gradle.caching = true
            |org.gradle.caching.debug = true
            |'''.stripMargin()
    }
}
