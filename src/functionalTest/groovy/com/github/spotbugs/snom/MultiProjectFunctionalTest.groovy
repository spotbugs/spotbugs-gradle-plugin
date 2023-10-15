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
import spock.lang.Ignore

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class MultiProjectFunctionalTest extends BaseFunctionalTest {
    File buildFile

    File subBuildFile

    def setup() {
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
        BuildResult result = gradleRunner
                .withArguments(":sub:spotbugsMain")
                .build()

        then:
        SUCCESS == result.task(":sub:classes").outcome
        SUCCESS == result.task(":sub:spotbugsMain").outcome
    }

    def "can use project name of sub project"() {
        when:
        def result = gradleRunner
                .withArguments(':sub:spotbugsMain')
                .build()

        then:
        SUCCESS == result.task(":sub:spotbugsMain").outcome
        result.output.contains("-projectName, sub (spotbugsMain)")
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
        def result = gradleRunner
                .withArguments(':sub:spotbugsMain')
                .build()

        then:
        SUCCESS == result.task(":sub:spotbugsMain").outcome
        result.output.contains("spotbugs-4.0.0-RC1.jar")
    }

    def "can use toolVersion in the subproject"() {
        setup:
        subBuildFile << """
spotbugs {
    toolVersion = '4.0.0-RC1'
}
"""

        when:
        def result = gradleRunner
                .withArguments(':sub:spotbugsMain')
                .build()

        then:
        SUCCESS == result.task(":sub:spotbugsMain").outcome
        result.output.contains("spotbugs-4.0.0-RC1.jar")
    }
}
