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
import org.gradle.util.GradleVersion
import org.junit.jupiter.api.BeforeEach
import spock.lang.Ignore
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.junit.jupiter.api.Assertions.assertEquals

class AndroidFunctionalTest extends Specification {
    File rootDir
    File buildFile
    String version = System.getProperty('snom.test.functional.gradle', GradleVersion.current().version)

    @BeforeEach
    def setup() {
        rootDir = Files.createTempDir()
        buildFile = new File(rootDir, 'build.gradle')
    }

    @Ignore("need to install Android SDK")
    def "can generate spotbugsMain depending on classes task"() {
        given: "a Gradle project to build an Android app"
        GradleRunner runner =
                GradleRunner.create()
                .withProjectDir(rootDir)
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)

        buildFile << """
buildscript {
    repositories {
        google()
        jcenter()
    }

    dependencies {
        classpath 'com.android.tools.build:gradle:3.5.3'
"""
        runner.pluginClasspath.forEach({ file ->
            buildFile << """
        classpath '${file.absolutePath}'
"""
        })
        buildFile << """
    }
}

apply plugin: 'com.android.application'
apply plugin: 'com.github.spotbugs'

repositories {
    google()
    jcenter()
}

android {
    compileSdkVersion 29
    buildToolsVersion '29.0.2'
    buildTypes {
        release {
            minifyEnabled false
        }
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

        when: "the spotbugsMain task is executed"
        BuildResult result = runner
                .withArguments(":spotbugsMain")
                .withGradleVersion(version)
                .build()

        then: "gradle runs spotbugsMain successfully"
        assertEquals(SUCCESS, result.task(":spotbugsMain").outcome)
    }
}
