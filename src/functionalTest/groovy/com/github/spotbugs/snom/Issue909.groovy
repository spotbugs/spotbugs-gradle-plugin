/*
 * Copyright 2023 SpotBugs team
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
import java.nio.file.Path

/**
 * @see <a href="https://github.com/spotbugs/spotbugs-gradle-plugin/issues/909">GitHub Issues</a>
 */
class Issue909 extends BaseFunctionalTest {
    Path buildFile

    def setup() {
        buildFile = rootDir.toPath().resolve("build.gradle")
        buildFile.toFile() << """
plugins {
  id "java"
  id "com.github.spotbugs"
}
repositories {
  mavenCentral()
}
tasks.spotbugsMain {
    reports {
        html {
            required = true
            stylesheet = resources.text.fromString("I am not valid XSL")
        }
    }
}
        """
        Path sourceDir = rootDir.toPath().resolve("src").resolve("main").resolve("java")
        sourceDir.toFile().mkdirs()
        Path sourceFile = sourceDir.resolve("Foo.java")
        sourceFile.toFile() << """
public class Foo {
    public static void main(String... args) {
        System.out.println("Hello, SpotBugs!");
    }
}
"""
    }

    def "cannot generate HTML report if invalid XSL is provided"() {
        when:
        def result = gradleRunner
                .withArguments('spotbugsMain')
                .buildAndFail()

        then:
        TaskOutcome.FAILED == result.task(":spotbugsMain").outcome
    }
}
