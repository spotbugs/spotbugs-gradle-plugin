/*
 * Copyright 2019 SpotBugs team
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
import org.junit.jupiter.api.BeforeEach
import spock.lang.Specification

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertEquals

class MultiProjectFunctionalTest extends Specification {
    File rootDir
    File buildFile

    @BeforeEach
    def setup() {
        rootDir = Files.createTempDir()
        buildFile = new File(rootDir, 'build.gradle')
        buildFile << """
plugins {
    id 'java'
    id 'jp.skypencil.spotbugs.snom'
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
        File subBuildFile = new File(subProject, "build.gradle")
        subBuildFile << """
apply plugin: 'java'
apply plugin: 'jp.skypencil.spotbugs.snom'
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
                .build()

        then:
        assertEquals(TaskOutcome.SUCCESS, result.task(":sub:classes").getOutcome())
        assertEquals(TaskOutcome.SUCCESS, result.task(":sub:spotbugsMain").getOutcome())
    }
}
