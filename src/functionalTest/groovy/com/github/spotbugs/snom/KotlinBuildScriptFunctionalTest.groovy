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
import org.gradle.util.GradleVersion
import org.junit.jupiter.api.BeforeEach
import spock.lang.Specification

import java.nio.file.Files

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class KotlinBuildScriptFunctionalTest extends Specification {
    File rootDir
    File buildFile
    String version = System.getProperty('snom.test.functional.gradle', GradleVersion.current().version)

    @BeforeEach
    def setup() {
        rootDir = Files.createTempDirectory("KotlinBuildScriptFunctionalTest").toFile()
        buildFile = new File(rootDir, 'build.gradle.kts')
        buildFile << """
plugins {
  `java`
  id("com.github.spotbugs")
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

    def "can set params to SpotBugsExtension"() {
        setup:
        buildFile << """
spotbugs {
    ignoreFailures.set(false)
    showStackTraces.set(true)
    showProgress.set(true)
    effort.set(com.github.spotbugs.snom.Effort.DEFAULT)
    reportLevel.set(com.github.spotbugs.snom.Confidence.DEFAULT)
    visitors.set(listOf("FindSqlInjection", "SwitchFallthrough"))
    omitVisitors.set(listOf("FindNonShortCircuit"))
    reportsDir.set(file("\$buildDir/spotbugs"))
    includeFilter.set(file("include.xml"))
    excludeFilter.set(file("exclude.xml"))
    baselineFile.set(file("baseline.xml"))
    onlyAnalyze.set(listOf("com.foobar.MyClass", "com.foobar.mypkg.*"))
    maxHeapSize.set("1g")
    extraArgs.set(listOf("-nested:false"))
    jvmArgs.set(listOf("-Duser.language=ja"))
}
"""
        new File(rootDir, "include.xml") << "<FindBugsFilter />"
        new File(rootDir, "exclude.xml") << "<FindBugsFilter />"
        new File(rootDir, "baseline.xml") << "<BugCollection />"

        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('check', '--info')
                .withPluginClasspath()
                .withGradleVersion(version)
                .build()

        then:
        result.task(":spotbugsMain").outcome == SUCCESS
    }

    def "can add plugins by spotbugsPlugins configuration"() {
        setup:
        buildFile << """
dependencies {
    spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.11.0")
}
"""

        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('check', '--info')
                .withPluginClasspath()
                .withGradleVersion(version)
                .build()

        then:
        result.task(":spotbugsMain").outcome == SUCCESS
        if (GradleVersion.version(version) >= GradleVersion.version("6.0")) {
            result.output.contains("Applying com.h3xstream.findsecbugs.PredictableRandomDetector to Foo")
        }
        !result.output.contains("Trying to add already registered factory")
    }

    def "can use the specified SpotBugs version"() {
        setup:
        buildFile << """
dependencies {
    spotbugs("com.github.spotbugs:spotbugs:4.0.0-beta4")
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
        result.task(":classes").outcome == SUCCESS
        result.output.contains("SpotBugs 4.0.0-beta4") || result.output.contains("spotbugs-4.0.0-beta4.jar")
    }

    def "can generate spotbugs.html in configured destination"() {
        buildFile << """
tasks.spotbugsMain {
    reports.create("html") {
        isEnabled = true
        setDestination(file("\$buildDir/reports/spotbugs.html"))
        setStylesheet("fancy-hist.xsl")
    }
}
"""
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('check')
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)
                .build()

        then:
        result.task(":spotbugsMain").outcome == SUCCESS
        File report = rootDir.toPath().resolve("build").resolve("reports").resolve("spotbugs.html").toFile()
        report.isFile()
    }

    def "can use toolVersion to get the SpotBugs version"() {
        setup:
        buildFile << """
dependencies {
    spotbugs("com.github.spotbugs:spotbugs:4.0.2")
    compileOnly("com.github.spotbugs:spotbugs-annotations:\${spotbugs.toolVersion.get()}")
}
"""
        when:
        BuildResult result =
                GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments(":spotbugsMain", "--debug")
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)
                .build()

        then:
        result.task(":spotbugsMain").outcome == SUCCESS
        result.output.contains("com.github.spotbugs:spotbugs-annotations:4.0.2")
    }
}