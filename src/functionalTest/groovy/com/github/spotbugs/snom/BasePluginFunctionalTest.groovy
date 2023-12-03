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
import org.gradle.util.GradleVersion
import spock.lang.IgnoreIf
import spock.lang.Unroll

@IgnoreIf({
    def current = System.getProperty('gradleVersion', GradleVersion.current().version)
    return GradleVersion.version(current) < GradleVersion.version("8.1")
})
class BasePluginFunctionalTest extends BaseFunctionalTest {
    File buildFile

    def setup() {
        buildFile = new File(rootDir, 'build.gradle')
        buildFile << """
plugins {
    id 'java'
    id 'com.github.spotbugs-base'
}

version = 1.0

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

    def "does not create SpotBugsTask by default"() {
        when:
        BuildResult result = gradleRunner
                .withArguments(":check")
                .build()

        then:
        result.task(":classes").outcome == TaskOutcome.SUCCESS
        !result.tasks.contains(":spotbugsMain")
    }

    def "can create spotbugsMain task manually"() {
        setup:
        buildFile << """
task spotbugsMain(type: com.github.spotbugs.snom.SpotBugsTask) {
    dependsOn 'classes'
    classDirs = sourceSets.main.output
}
"""
        when:
        BuildResult result = gradleRunner
                .withArguments(":spotbugsMain")
                .build()

        then:
        result.task(":classes").outcome == TaskOutcome.SUCCESS
        result.task(":spotbugsMain").outcome == TaskOutcome.SUCCESS
    }

    def "SpotBugsTask does not create report by default"() {
        setup:
        buildFile << """
task spotbugsMain(type: com.github.spotbugs.snom.SpotBugsTask) {
    dependsOn 'classes'
    classDirs = sourceSets.main.output
}
"""
        when:
        BuildResult result = gradleRunner
                .withArguments(":spotbugsMain")
                .build()

        then:
        result.task(":spotbugsMain").outcome == TaskOutcome.SUCCESS
        File report = rootDir.toPath().resolve("build").resolve("reports").resolve("spotbugs").resolve("main.xml").toFile()
        !report.isFile()
    }

    @Unroll
    def 'shows suggestion to enable report when failures are found (Worker API? #isWorkerApi)'() {
        given:
        def badCode = new File(rootDir, 'src/main/java/Bar.java')
        badCode << '''
        |public class Bar {
        |  public int unreadField = 42; // warning: URF_UNREAD_FIELD
        |}
        |'''.stripMargin()
        buildFile << """
task spotbugsMain(type: com.github.spotbugs.snom.SpotBugsTask) {
    dependsOn 'classes'
    classDirs = sourceSets.main.output
}
"""
        when:
        def arguments = [':spotbugsMain']
        if (!isWorkerApi) {
            arguments.add('-Pcom.github.spotbugs.snom.worker=false')
        }
        def runner = gradleRunner
                .withArguments(arguments)

        def result = runner.buildAndFail()

        then:
        result.task(':spotbugsMain').outcome == TaskOutcome.FAILED
        !result.output.contains('SpotBugs report can be found in null')

        where:
        // Each SpotBugsRunner has code to handle report, so test both SpotBugsRunnerForWorker and SpotBugsRunnerForJavaExec
        isWorkerApi << [true, false]
    }
}
