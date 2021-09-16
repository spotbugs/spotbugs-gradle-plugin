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

import org.gradle.internal.impldep.com.google.common.io.Files
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.junit.jupiter.api.BeforeEach
import spock.lang.Specification
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class GradleJavaToolchainsSupportFunctionalTest extends Specification {
    File rootDir
    File buildFile
    String version = System.getProperty('snom.test.functional.gradle', GradleVersion.current().version)

    @BeforeEach
    def setup() {
        rootDir = Files.createTempDir()
        buildFile = new File(rootDir, 'build.gradle')
        buildFile << """
plugins {
    id 'java'
    id 'com.github.spotbugs'
}

version = 1.0

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(16)
    }
}
        """
        File sourceDir = rootDir.toPath().resolve("src").resolve("main").resolve("java").toFile()
        sourceDir.mkdirs()
        File sourceFile = new File(sourceDir, "Foo.java")
        sourceFile << """
public class Foo {
    public static void main(String... args) {
        System.out.println("Hello, SpotBugs!");
    }
}
"""
    }

    @Unroll
    def 'Supports Gradle Java Toolchains (#processConfiguration)'() {
        setup:
        buildFile << """
        spotbugs {
          useJavaToolchains = true
        }"""

        when:
        def arguments = [':spotbugsMain', '-is']
        arguments.add(processConfigurationArgument)

        def runner = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments(arguments)
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)

        def result = runner.build()

        then:
        result.task(':spotbugsMain').outcome == SUCCESS
        result.output.contains('Spotbugs will be executed using Java Toolchain configuration')

        where:
        processConfiguration | processConfigurationArgument
        'javaexec'           | '-Pcom.github.spotbugs.snom.worker=false'
        'worker-api'         | '-Pcom.github.spotbugs.snom.worker=true'
        'javaexec-in-worker' | '-Pcom.github.spotbugs.snom.javaexec-in-worker=true'
    }

    @Unroll
    def 'Do not use Gradle Java Toolchains if extension is not configured (#processConfiguration)'() {
        when:
        def arguments = [':spotbugsMain', '-is']
        arguments.add(processConfigurationArgument)

        def runner = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments(arguments)
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)

        def result = runner.build()

        then:
        result.task(':spotbugsMain').outcome == SUCCESS
        !result.output.contains('Spotbugs will be executed using Java Toolchain configuration')


        where:
        processConfiguration | processConfigurationArgument
        'javaexec'           | '-Pcom.github.spotbugs.snom.worker=false'
        'worker-api'         | '-Pcom.github.spotbugs.snom.worker=true'
        'javaexec-in-worker' | '-Pcom.github.spotbugs.snom.javaexec-in-worker=true'
    }
}
