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
import org.gradle.util.GradleVersion
import org.junit.jupiter.api.BeforeEach
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Paths

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

class StandardFunctionalTest extends Specification {
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

    def "can create spotbugsMain task depending on classes task"() {
        when:
        BuildResult result =
                GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments(":spotbugsMain")
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)
                .build()

        then:
        assertEquals(TaskOutcome.SUCCESS, result.task(":classes").outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":spotbugsMain").outcome)
    }

    def "can use the specified SpotBugs version"() {
        setup:
        buildFile << """
dependencies {
    spotbugs "com.github.spotbugs:spotbugs:4.0.0-beta4"
}"""
        when:
        BuildResult result =
                GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments(":spotbugsMain", "--info")
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)
                .build()

        then:
        assertEquals(TaskOutcome.SUCCESS, result.task(":classes").outcome)
        assertTrue(result.output.contains("SpotBugs 4.0.0-beta4"))
    }

    def "can skip analysis when no class file we have"() {
        setup:
        File sourceDir = rootDir.toPath().resolve("src").resolve("main").resolve("java").toFile()
        File sourceFile = new File(sourceDir, "Foo.java")
        sourceFile.delete()

        when:
        BuildResult result =
                GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments(":spotbugsMain")
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)
                .build()

        then:
        assertEquals(TaskOutcome.NO_SOURCE, result.task(":spotbugsMain").outcome)
    }

    def "can use effort and reportLevel"() {
        buildFile << """
spotbugsMain {
    effort = 'min'
    reportLevel = 'high'
}"""
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('spotbugsMain', '--debug')
                .withPluginClasspath()
                .withGradleVersion(version)
                .build()

        then:
        result.task(":spotbugsMain").outcome == SUCCESS
        assertTrue(result.getOutput().contains("-effort:min"))
        assertTrue(result.getOutput().contains("-high"))
    }

    def "can be cancelled by withType(VerificationTask)"() {
        buildFile << """
tasks.withType(VerificationTask).configureEach {
    ignoreFailures = true
}
spotbugsMain {
    doLast {
        print "SpotBugsMain ignores failures? \${ignoreFailures}"
    }
}
"""
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('spotbugsMain')
                .withPluginClasspath()
                .withGradleVersion(version)
                .build()

        then:
        assertEquals(SUCCESS, result.task(":spotbugsMain").outcome)
        assertTrue(result.getOutput().contains("SpotBugsMain ignores failures? true"))
    }

    def "is cache-able"() {
        buildFile << """
spotbugsMain {
    reports {
        text.enabled = true
    }
}"""

        when:
        GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments(":spotbugsMain")
                .withPluginClasspath()
                .withGradleVersion(version)
                .build()
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments(":spotbugsMain", "--info")
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)
                .build()

        then:
        assertEquals(TaskOutcome.UP_TO_DATE, result.task(":classes").outcome)
        assertEquals(TaskOutcome.UP_TO_DATE, result.task(":spotbugsMain").outcome)
    }

    @Unroll
    def 'build fails when bugs are found (Worker API? #isWorkerApi)'() {
        given:
        def badCode = new File(rootDir, 'src/main/java/Bar.java')
        badCode << '''
        |public class Bar {
        |  public int unreadField = 42; // warning: URF_UNREAD_FIELD
        |}
        |'''.stripMargin()

        when:
        def arguments = [':spotbugsMain', '-is']
        if(!isWorkerApi) {
            arguments.add('-Pcom.github.spotbugs.snom.worker=false')
        }
        def runner = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments(arguments)
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)

        def result = runner.buildAndFail()

        then:
        result.task(':spotbugsMain').outcome == TaskOutcome.FAILED

        where:
        isWorkerApi << [true, false]
    }

    @Unroll
    def 'build does not fail when bugs are found with `ignoreFailures = true` (Worker API? #isWorkerApi)'() {
        given:
        def badCode = new File(rootDir, 'src/main/java/Bar.java')
        badCode << '''
        |public class Bar {
        |  public int unreadField = 42; // warning: URF_UNREAD_FIELD
        |}
        |'''.stripMargin()

        buildFile << """
spotbugs {
    ignoreFailures = true
}"""
        when:
        def arguments = [':spotbugsMain', '-is']
        if(!isWorkerApi) {
            arguments.add('-Pcom.github.spotbugs.snom.worker=false')
        }
        def runner = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments(arguments)
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)

        def result = runner. build()

        then:
        result.task(':spotbugsMain').outcome == TaskOutcome.SUCCESS

        where:
        isWorkerApi << [true, false]
    }

    def 'PatternFilterable methods can work for classes props'() {
        given:
        def badCode = new File(rootDir, 'src/main/java/Bar.java')
        badCode << '''
        |public class Bar {
        |  public int unreadField = 42; // warning: URF_UNREAD_FIELD
        |}
        |'''.stripMargin()

        buildFile << """
spotbugsMain {
    classes = classes.filter { it.name.contains 'Foo' }
}"""
        when:
        def runner = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments(':spotbugsMain', '-is')
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)

        def result = runner. build()

        then:
        result.task(':spotbugsMain').outcome == TaskOutcome.SUCCESS
    }
    def 'analyse main sourceset only'() {
        given:
        buildFile << """
tasks.withType(com.github.spotbugs.snom.SpotBugsTask).configureEach {
  it.enabled = it.name == 'spotbugsMain'
}
"""
        File testDir = rootDir.toPath().resolve("src").resolve("test").resolve("java").toFile()
        testDir.mkdirs()
        File sourceFile = new File(testDir, "Foo.java")
        sourceFile << """
public class Foo {
    public static void main(String... args) {
        System.out.println("Hello, SpotBugs!");
    }
}
"""
        when:
        def runner = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments(':spotbugsMain', ':spotbugsTest')
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)

        def result = runner.build()

        then:
        TaskOutcome.SUCCESS == result.task(':spotbugsMain').outcome
        TaskOutcome.SKIPPED == result.task(':spotbugsTest').outcome
    }

    def 'can analyse the sourceSet added by user'() {
        given:
        buildFile << """
sourceSets {
    another {
        java {
            srcDir 'src/another'
        }
    }
}
spotbugsAnother {
    reports {
        text.enabled = true
    }
}"""
        File sourceDir = rootDir.toPath().resolve(Paths.get("src", "another", "java")).toFile()
        sourceDir.mkdirs()
        File sourceFile = new File(sourceDir, "Foo.java")
        sourceFile << """
public class Foo {
    public static void main(String... args) {
        System.out.println("Hello, SpotBugs!");
    }
}
"""

        when:
        def runner = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments(':spotbugsAnother')
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)

        def result = runner.build()

        then:
        TaskOutcome.SUCCESS == result.task(':spotbugsAnother').outcome
    }

    def "can pass the analysis when classDirs contain no .class file"() {
        setup:
        File sourceDir = rootDir.toPath().resolve("src").resolve("main").resolve("java").toFile()
        File sourceFile = new File(sourceDir, "Foo.java")
        sourceFile.delete()
        File resourceDir = rootDir.toPath().resolve("src").resolve("main").resolve("resources").toFile()
        resourceDir.mkdir()
        File xml = new File(resourceDir, "bar.xml")
        xml << "<!-- I am not .class file -->"
        when:
        BuildResult result =
                GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments(":spotbugsMain")
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)
                .build()

        then:
        result.task(":classes").outcome == TaskOutcome.SUCCESS
        result.task(":spotbugsMain").outcome == TaskOutcome.NO_SOURCE
    }

    def "can run analysis when check task is triggered"() {
        when:
        BuildResult result =
                GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments("clean", "check")
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)
                .build()

        then:
        result.task(":spotbugsMain").outcome == TaskOutcome.SUCCESS
    }

    def "can apply plugin"() {
        given:
        buildFile << """
dependencies{
  spotbugsPlugins 'com.h3xstream.findsecbugs:findsecbugs-plugin:1.10.1'
}"""
        when:
        BuildResult result =
                GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments("spotbugsMain", "--debug")
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)
                .build()

        then:
        result.task(":spotbugsMain").outcome == TaskOutcome.SUCCESS
        result.output.contains("Applying com.h3xstream.findsecbugs.PredictableRandomDetector to Foo")
        !result.output.contains("Trying to add already registered factory")
    }

    def "can apply plugin to multiple tasks"() {
        given:
        buildFile << """
spotbugs {
  ignoreFailures = true
}
dependencies {
  spotbugsPlugins 'com.h3xstream.findsecbugs:findsecbugs-plugin:1.10.1'
}"""

        File testDir = rootDir.toPath().resolve(Paths.get("src", "test", "java")).toFile()
        testDir.mkdirs()
        File testFile = new File(testDir, "FooTest.java")
        testFile << """
public class FooTest {
    public static void main(String... args) {
        System.out.println("Hello, SpotBugs!");
    }
}"""
        when:
        BuildResult result =
                GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments("spotbugsMain", "spotbugsTest", "--debug")
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)
                .build()

        then:
        result.task(":spotbugsMain").outcome == TaskOutcome.SUCCESS
        result.task(":spotbugsTest").outcome == TaskOutcome.SUCCESS
        result.output.contains("Applying com.h3xstream.findsecbugs.PredictableRandomDetector to Foo")
        result.output.contains("Applying com.h3xstream.findsecbugs.PredictableRandomDetector to FooTest")
        !result.output.contains("Trying to add already registered factory")
    }
}
