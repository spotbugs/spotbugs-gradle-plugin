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
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import spock.lang.Requires
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.junit.jupiter.api.Assertions.assertEquals

class AndroidFunctionalTest extends Specification {
    static String version = System.getProperty('snom.test.functional.gradle', GradleVersion.current().version)

    /**
     * AGP 4.2 is only supported by Gradle 6.7.1 and up
     * @see <a href="https://developer.android.com/studio/releases/gradle-plugin#updating-gradle">Android Gradle plugin release notes</a>
     */
    private static boolean supportsAGP42() {
        GradleVersion.version(version) >= GradleVersion.version("6.7.1")
    }

    File rootDir

    @BeforeEach
    def setup() {
        rootDir = Files.createTempDir()
    }

    @AfterEach
    void cleanup() {
        rootDir.deleteDir()
    }

    @Requires({env['ANDROID_SDK_ROOT']})
    @Requires({AndroidFunctionalTest.supportsAGP42()})
    def "can generate spotbugsRelease depending on app variant compilation task with AGP 4.2.0"() {
        given: "a Gradle project to build an Android app"
        GradleRunner runner = getGradleRunner()
        writeAppBuildFile(runner, '4.2.0')
        writeSourceFile()
        writeManifestFile()

        when: "the spotbugsRelease task is executed"
        BuildResult result = build(runner)

        then: "gradle runs spotbugsRelease successfully"
        assertEquals(SUCCESS, result.task(":spotbugsRelease").outcome)
    }

    @Requires({env['ANDROID_SDK_ROOT']})
    @Requires({AndroidFunctionalTest.supportsAGP42()})
    def "can generate spotbugsRelease depending on library variant compilation task with AGP 4.2.0"() {
        given: "a Gradle project to build an Android library"
        GradleRunner runner = getGradleRunner()
        writeLibraryBuildFile(runner, '4.2.0')
        writeSourceFile()
        writeManifestFile()

        when: "the spotbugsRelease task is executed"
        BuildResult result = build(runner)

        then: "gradle runs spotbugsRelease successfully"
        assertEquals(SUCCESS, result.task(":spotbugsRelease").outcome)
    }

    private BuildResult build(GradleRunner runner) {
        runner.withArguments(":spotbugsRelease", '-s')
                .withGradleVersion(version)
                .build()
    }

    private GradleRunner getGradleRunner() {
        GradleRunner runner =
                GradleRunner.create()
                .withProjectDir(rootDir)
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)
        runner
    }

    def writeAppBuildFile(runner, agpVersion) {
        File buildFile = new File(rootDir, 'build.gradle')
        buildFile << """
buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath 'com.android.tools.build:gradle:$agpVersion'
"""
        runner.pluginClasspath.forEach({ file ->
            buildFile << """
        classpath files("${file.absolutePath}")
"""
        })
        buildFile << """
    }
}

apply plugin: 'com.android.application'
apply plugin: 'com.github.spotbugs'

repositories {
    google()
    mavenCentral()
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
    }

    def writeLibraryBuildFile(runner, agpVersion) {
        File buildFile = new File(rootDir, 'build.gradle')
        buildFile << """
buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath 'com.android.tools.build:gradle:$agpVersion'
"""
        runner.pluginClasspath.forEach({ file ->
            buildFile << """
        classpath files('${file.absolutePath}')
"""
        })
        buildFile << """
    }
}

apply plugin: 'com.android.library'
apply plugin: 'com.github.spotbugs'

repositories {
    google()
    mavenCentral()
}

android {
    compileSdkVersion 29
    buildToolsVersion '29.0.2'
}
"""
    }

    void writeSourceFile() {
        File sourceFile = new File(rootDir, "src/main/java/Foo.java")
        sourceFile.parentFile.mkdirs()
        sourceFile << """
public class Foo {
    public static void main(String... args) {
        System.out.println("Hello, SpotBugs!");
    }
}
"""
    }

    void writeManifestFile() {
        File manifestFile = new File(rootDir, "src/main/AndroidManifest.xml")
        manifestFile.parentFile.mkdirs()
        manifestFile << """
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="test.spotbugs" />
"""
    }
}
