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

/**
 * https://github.com/lazystone/spotbugs-bugs/blob/master/build.gradle.kts
 * https://github.com/spotbugs/spotbugs-gradle-plugin/issues/196
 */
class Issue196 extends BaseFunctionalTest {
    File buildFile

    def setup() {
        buildFile = new File(rootDir, 'build.gradle.kts')
        buildFile << """
plugins {
  id("com.github.spotbugs")
  java
}
repositories {
  mavenCentral()
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

    def "can run analysis when check task is triggered"() {
        when:
        BuildResult result = gradleRunner
                .withArguments("check")
                .build()

        then:
        result.task(":spotbugsMain").outcome == TaskOutcome.SUCCESS
    }
}
