/*
 * Copyright 2019-2021 SpotBugs team
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

import org.gradle.internal.impldep.com.google.common.io.Files
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.junit.jupiter.api.BeforeEach
import spock.lang.Ignore
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

class MultiProjectFunctionalTest extends Specification {
    File rootDir
    File buildFile
    String version = System.getProperty('snom.test.functional.gradle', GradleVersion.current().version)
    File subBuildFile

    @BeforeEach
    def setup() {
        rootDir = Files.createTempDir()
        buildFile = new File(rootDir, 'build.gradle')
        buildFile << """
plugins {
    id 'java'
    id 'com.github.spotbugs'
}
        """

        File settings = new File(rootDir, "settings.gradle")
        settings << """
include ':sub'
"""

        File subProject = new File(rootDir, "sub")

        File sourceDir = subProject.toPath().resolve("src").resolve("main").resolve("java").toFile()
        sourceDir.mkdirs()
        File sourceFile = new File(sourceDir, "Foo.java")
        sourceFile << """
public class Foo {
    public static void main(String... args) {
        System.out.println("Hello, SpotBugs!");
    }
}
"""
        subBuildFile = new File(subProject, "build.gradle")
        subBuildFile << """
apply plugin: 'java'
apply plugin: 'com.github.spotbugs'
version = 1.0

repositories {
    mavenCentral()
}
"""
    }

    def "can create spotbugsMain task depending on classes task"() {
        when:
        BuildResult result =
                GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments(":sub:spotbugsMain")
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)
                .build()

        then:
        assertEquals(TaskOutcome.SUCCESS, result.task(":sub:classes").outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":sub:spotbugsMain").outcome)
    }

    def "can use project name of sub project"() {
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments(':sub:spotbugsMain', '--debug')
                .withPluginClasspath()
                .withGradleVersion(version)
                .forwardOutput()
                .build()

        then:
        assertEquals(SUCCESS, result.task(":sub:spotbugsMain").outcome)
        assertTrue(result.output.contains("-projectName, sub (spotbugsMain)"))
    }

    @Ignore("Gradle does not support this type of configuration. See https://git.io/JvOVT#issuecomment-580239267")
    def "can use toolVersion in subprojects block"() {
        setup:
        buildFile << """
subprojects {
    spotbugs {
        toolVersion = '4.0.0-RC1'
    }
}
"""

        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments(':sub:spotbugsMain', '--debug')
                .withPluginClasspath()
                .withGradleVersion(version)
                .forwardOutput()
                .build()

        then:
        assertEquals(SUCCESS, result.task(":sub:spotbugsMain").outcome)
        assertTrue(result.output.contains("SpotBugs 4.0.0-RC1"))
    }

    def "can use toolVersion in the subproject"() {
        setup:
        subBuildFile << """
spotbugs {
    toolVersion = '4.0.0-RC1'
}
"""

        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments(':sub:spotbugsMain', '--debug')
                .withPluginClasspath()
                .withGradleVersion(version)
                .forwardOutput()
                .build()

        then:
        assertEquals(SUCCESS, result.task(":sub:spotbugsMain").outcome)
        assertTrue(result.output.contains("SpotBugs 4.0.0-RC1"))
    }
}
