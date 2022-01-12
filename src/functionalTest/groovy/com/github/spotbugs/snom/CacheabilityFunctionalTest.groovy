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
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import spock.lang.Specification

import java.nio.file.Files

class CacheabilityFunctionalTest extends Specification {
    /**
     * @see <a href="https://github.com/spotbugs/spotbugs-gradle-plugin/issues/662">GitHub Issues</a>
     */
    def 'spotbugsMain task runs with configuration cache'() {
        given:
        def buildDir = Files.createTempDirectory(null).toFile()
        def version = System.getProperty('snom.test.functional.gradle', GradleVersion.current().version)

        initializeBuildFile(buildDir)

        when:
        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(buildDir)
                        .withArguments(':spotbugsMain', '--configuration-cache')
                        .withPluginClasspath()
                        .forwardOutput()
                        .withGradleVersion(version)
                        .build()

        then:
        !result.output.contains("Configuration cache problems found in this build")
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

        def version = System.getProperty('snom.test.functional.gradle', GradleVersion.current().version)

        initializeBuildFile(buildDir1)
        initializeBuildFile(buildDir2)

        when:
        BuildResult result1 =
                GradleRunner.create()
                .withProjectDir(buildDir1)
                .withArguments(':spotbugsMain')
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)
                .build()
        def hashKeyLine1 = getHashKeyLine(result1)

        then:
        hashKeyLine1

        when:
        BuildResult result2 =
                GradleRunner.create()
                .withProjectDir(buildDir2)
                .withArguments(':spotbugsMain', '--scan')
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)
                .build()
        def hashKeyLine2 = getHashKeyLine(result2)

        then:
        hashKeyLine2
        hashKeyLine1 == hashKeyLine2
    }

    private static String getHashKeyLine(BuildResult result) {
        return result.output.find('Build cache key for task \':spotbugsMain\' is .*')
    }

    private static void initializeBuildFile(File buildDir) {
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
            |    id "com.gradle.enterprise" version "3.6.4"
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
        sourceFile << '''
            |public class Foo {
            |    public static void main(String... args) {
            |        System.out.println("Hello, SpotBugs!");
            |    }
            |}
            |'''.stripMargin()

        propertiesFile << '''
            |org.gradle.caching = true
            |org.gradle.caching.debug = true
            |'''.stripMargin()
    }
}
