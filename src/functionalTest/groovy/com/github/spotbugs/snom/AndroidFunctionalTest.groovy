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
import spock.lang.Ignore
import spock.lang.Requires

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@Ignore
class AndroidFunctionalTest extends BaseFunctionalTest {

    @Requires({ env['ANDROID_SDK_ROOT'] })
    def "can generate spotbugsRelease depending on app variant compilation task with AGP 4.2.0"() {
        given: "a Gradle project to build an Android app"
        GradleRunner runner = gradleRunner
        writeAppBuildFile(runner, '4.2.0')
        writeSourceFile()
        writeManifestFile()

        when: "the spotbugsRelease task is executed"
        BuildResult result = build(runner)

        then: "gradle runs spotbugsRelease successfully"
        SUCCESS == result.task(":spotbugsRelease").outcome
    }

    @Requires({env['ANDROID_SDK_ROOT']})
    def "can generate spotbugsRelease depending on library variant compilation task with AGP 4.2.0"() {
        given: "a Gradle project to build an Android library"
        GradleRunner runner = gradleRunner
        writeLibraryBuildFile(runner, '4.2.0')
        writeSourceFile()
        writeManifestFile()

        when: "the spotbugsRelease task is executed"
        BuildResult result = build(runner)

        then: "gradle runs spotbugsRelease successfully"
        SUCCESS == result.task(":spotbugsRelease").outcome
    }

    private BuildResult build(GradleRunner runner) {
        runner.withArguments(":spotbugsRelease", '-s')
                .build()
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
        classpath files('${file.absolutePath}')
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
