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

import org.gradle.testkit.runner.TaskOutcome

/**
 * Verifies that SpotBugs analysis still succeeds when an external dependency management tool
 * (such as the Spring Boot BOM) downgrades {@code spotbugs-annotations} to an older version that
 * is missing classes required by the SpotBugs engine (e.g. {@code SuppressMatchType}).
 *
 * @see <a href="https://github.com/spotbugs/spotbugs-gradle-plugin/issues/1436">GitHub issue #1436</a>
 */
class Issue1436 extends BaseFunctionalTest {
    File buildFile

    def setup() {
        buildFile = new File(rootDir, 'build.gradle')
        buildFile << """
plugins {
    id 'java'
    id 'com.github.spotbugs'
}

repositories {
    mavenCentral()
}

// Simulate what Spring Boot BOM does: force spotbugs-annotations to an older version
// across ALL configurations via a global resolution strategy.
configurations.configureEach {
    resolutionStrategy.eachDependency { details ->
        if (details.requested.group == 'com.github.spotbugs' && details.requested.name == 'spotbugs-annotations') {
            details.useVersion '4.8.6'
            details.because 'simulating Spring Boot BOM downgrade (issue #1436)'
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
    }

    def "analysis succeeds even when spotbugs-annotations is downgraded globally by an external BOM"() {
        when:
        def result = gradleRunner
                .withArguments('spotbugsMain')
                .build()

        then:
        result.task(":spotbugsMain").outcome == TaskOutcome.SUCCESS
    }
}
