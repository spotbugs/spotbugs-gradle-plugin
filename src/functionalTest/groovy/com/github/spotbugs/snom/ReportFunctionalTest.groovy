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
import org.gradle.util.GradleVersion
import org.junit.jupiter.api.BeforeEach
import spock.lang.Specification

import org.gradle.testkit.runner.GradleRunner

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue

class ReportFunctionalTest extends Specification {
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
    id 'jp.skypencil.spotbugs.snom'
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

    def "do not generate reports by default"() {
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('spotbugsMain')
                .withPluginClasspath()
                .withGradleVersion(version)
                .build()

        then:
        assertEquals(SUCCESS, result.task(":spotbugsMain").outcome)
        File reportsDir = rootDir.toPath().resolve("build").resolve("reports").resolve("spotbugs").toFile()
        assertFalse(reportsDir.isDirectory())
    }

    def "can generate spotbugs.txt"() {
        buildFile << """
spotbugsMain {
    reports {
        text.enabled = true
    }
}"""
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('spotbugsMain', '-is')
                .withPluginClasspath()
                .withGradleVersion(version)
                .build()

        then:
        assertEquals(SUCCESS, result.task(":spotbugsMain").outcome)
        File report = rootDir.toPath().resolve("build").resolve("reports").resolve("spotbugs").resolve("main.txt").toFile()
        assertTrue(report.isFile())
    }

    def "can generate spotbugs.txt in configured buildDir"() {
        buildFile << """
spotbugsMain {
    reports {
        text.enabled = true
    }
}
buildDir = 'new-build-dir'
"""
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('spotbugsMain')
                .withPluginClasspath()
                .build()

        then:
        assertEquals(SUCCESS, result.task(":spotbugsMain").outcome)
        File report = rootDir.toPath().resolve("new-build-dir").resolve("reports").resolve("spotbugs").resolve("main.txt").toFile()
        assertTrue(report.isFile())
    }

    def "can generate spotbugs.html in configured buildDir"() {
        buildFile << """
spotbugsMain {
    reports {
        html.enabled = true
    }
}
buildDir = 'new-build-dir'
"""
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('spotbugsMain')
                .withPluginClasspath()
                .build()

        then:
        assertEquals(SUCCESS, result.task(":spotbugsMain").outcome)
        File report = rootDir.toPath().resolve("new-build-dir").resolve("reports").resolve("spotbugs").resolve("main.html").toFile()
        assertTrue(report.isFile())
    }

    def "can generate spotbugs.xml in configured buildDir"() {
        buildFile << """
spotbugsMain {
    reports {
        xml.enabled = true
    }
}
buildDir = 'new-build-dir'
"""
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('spotbugsMain')
                .withPluginClasspath()
                .build()

        then:
        assertEquals(SUCCESS, result.task(":spotbugsMain").outcome)
        File report = rootDir.toPath().resolve("new-build-dir").resolve("reports").resolve("spotbugs").resolve("main.xml").toFile()
        assertTrue(report.isFile())
    }

    def "can generate spotbugs.html"() {
        buildFile << """
spotbugsMain {
    reports {
        html.enabled = true
    }
}"""
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('spotbugsMain', '--debug')
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)
                .build()

        then:
        assertEquals(SUCCESS, result.task(":spotbugsMain").outcome)
        File report = rootDir.toPath().resolve("build").resolve("reports").resolve("spotbugs").resolve("main.html").toFile()
        assertTrue(report.isFile())
        assertTrue(result.getOutput().contains("-html,"))
    }

    def "can generate spotbugs.html with stylesheet"() {
        new File(rootDir, "settings.gradle") << """
rootProject.name = 'sample-project'
"""
        buildFile << """
// https://github.com/spotbugs/spotbugs-gradle-plugin/issues/107#issue-408724750
version = '1.2.3'
configurations { spotbugsStylesheets { transitive false } }
dependencies { spotbugsStylesheets 'com.github.spotbugs:spotbugs:3.1.10' }

spotbugsMain {
    reports {
        html {
            enabled = true
            stylesheet = resources.text.fromArchiveEntry(configurations.spotbugsStylesheets, 'fancy-hist.xsl')
        }
    }
}"""
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('spotbugsMain', "--debug")
                .withPluginClasspath()
                .withGradleVersion(version)
                .build()

        then:
        assertEquals(SUCCESS, result.task(":spotbugsMain").outcome)
        File report = rootDir.toPath().resolve("build").resolve("reports").resolve("spotbugs").resolve("main.html").toFile()
        assertTrue(report.isFile())
        assertTrue(result.getOutput().contains("-html:"))
        // confirm -projectName is working
        assertNotNull(report.readLines("utf-8").find({line -> line.contains("sample-project (spotbugsMain)")}))
        // confirm -release is working
        assertNotNull(report.readLines("utf-8").find({line -> line.contains("1.2.3")}))
    }

    def "can generate spotbugs.html with the path of stylesheet"() {
        buildFile << """
// https://github.com/spotbugs/spotbugs-gradle-plugin/issues/107#issue-408724750
spotbugsMain {
    reports {
        html {
            enabled = true
            stylesheet = 'fancy-hist.xsl'
        }
    }
}"""
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('spotbugsMain', '--debug')
                .withPluginClasspath()
                .withGradleVersion(version)
                .build()

        then:
        assertEquals(SUCCESS, result.task(":spotbugsMain").outcome)
        File report = rootDir.toPath().resolve("build").resolve("reports").resolve("spotbugs").resolve("main.html").toFile()
        assertTrue(report.isFile())
        assertTrue(result.getOutput().contains("-html:"))
    }

    def "can generate spotbugs.xml"() {
        buildFile << """
spotbugsMain {
    reports {
        xml.enabled = true
    }
}"""
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('spotbugsMain')
                .withPluginClasspath()
                .withGradleVersion(version)
                .build()

        then:
        assertEquals(SUCCESS, result.task(":spotbugsMain").outcome)
        File report = rootDir.toPath().resolve("build").resolve("reports").resolve("spotbugs").resolve("main.xml").toFile()
        assertTrue(report.isFile())
    }

    def "can generate a report in specified reportsDir"() {
        buildFile << """
spotbugs {
    reportsDir = file("\$buildDir/spotbugs")
}
spotbugsMain {
    reports {
        text {}
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
        File reportsDir = rootDir.toPath().resolve("build").resolve("spotbugs").toFile();
        assertTrue(reportsDir.isDirectory())
        File report = reportsDir.toPath().resolve("main.txt").toFile()
        assertTrue(report.isFile())
    }

    def "reports error when set unknown report type"() {
        buildFile << """
spotbugsMain {
    reports {
        unknown.enabled = true
    }
}"""
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('spotbugsMain')
                .withPluginClasspath()
                .withGradleVersion(version)
                .buildAndFail()

        then:
        assertTrue(result.getTasks().isEmpty())
        assertTrue(result.getOutput().contains("unknown is invalid as the report name"))
    }

    def "can run task by Worker Process"() {
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('spotbugsMain', '--info')
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)
                .build()

        then:
        assertEquals(SUCCESS, result.task(":spotbugsMain").outcome)
        assertTrue(result.getOutput().contains("Running SpotBugs by Gradle Worker..."));
    }

    def "can run task by JavaExec by gradle.properties"() {
        new File(rootDir, "gradle.properties") << """
com.github.spotbugs.snom.worker=false
"""
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('spotbugsMain', '--info')
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)
                .build()

        then:
        assertEquals(SUCCESS, result.task(":spotbugsMain").outcome)
        assertTrue(result.getOutput().contains("Running SpotBugs by JavaExec..."))
    }

    def "can run task by JavaExec by commandline option"() {
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('spotbugsMain', '-Pcom.github.spotbugs.snom.worker=false', '--info')
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)
                .build()

        then:
        assertEquals(SUCCESS, result.task(":spotbugsMain").outcome)
        assertTrue(result.getOutput().contains("Running SpotBugs by JavaExec..."))
    }

    def "does not resolve spotbugs configuration by setting stylesheet"() {
        given:
        buildFile << """
spotbugsMain {
    reports {
        html {
            stylesheet = 'fancy-hist.xsl'
        }
    }
}

configurations.spotbugs {
    exclude group: 'org.slf4j', module: 'slf4j-log4j12'
}
"""
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('spotbugsMain')
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(version)
                .build()

        then:
        assertEquals(SUCCESS, result.task(":spotbugsMain").outcome)
    }
}