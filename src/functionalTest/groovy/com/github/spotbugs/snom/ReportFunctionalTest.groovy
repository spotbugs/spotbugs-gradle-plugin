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

import static org.gradle.testkit.runner.TaskOutcome.FAILED

import java.nio.file.Path

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class ReportFunctionalTest extends BaseFunctionalTest {
    File buildFile

    def setup() {
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

    def "generate console report by default"() {
        given:
        def badCode = new File(rootDir, 'src/main/java/Bar.java')
        badCode << '''
        |public class Bar {
        |  public int unreadField = 42; // warning: URF_UNREAD_FIELD
        |}
        |'''.stripMargin()
        when:
        def result = gradleRunner
                .withArguments('spotbugsMain')
                .buildAndFail()

        then:
        FAILED == result.task(":spotbugsMain").outcome
        result.output.contains("M D UrF: Unread public/protected field: Bar.unreadField  At Bar.java:[line 3]")
        !rootDir.toPath().resolve("build").toFile().list().contains("reports")
    }

    def "can generate spotbugs.txt"() {
        buildFile << """
spotbugsMain {
    reports {
        text.required = true
    }
}"""
        when:
        def result = gradleRunner
                .withArguments('spotbugsMain', '-is')
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome
        File report = rootDir.toPath().resolve("build").resolve("reports").resolve("spotbugs").resolve("main.txt").toFile()
        report.isFile()
    }

    def "can generate spotbugs.txt in configured buildDir"() {
        buildFile << """
spotbugsMain {
    reports {
        text.required = true
    }
}
buildDir = 'new-build-dir'
"""
        when:
        def result = gradleRunner
                .withArguments('spotbugsMain')
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome
        File report = rootDir.toPath().resolve("new-build-dir").resolve("reports").resolve("spotbugs").resolve("main.txt").toFile()
        report.isFile()
    }

    def "prints reports location when stacktrace is suppressed"() {
        buildFile << """
spotbugsMain {
    showStackTraces = false
    reports {
        text.required = true
    }
}
buildDir = 'new-build-dir'
"""
        given:
        def badCode = new File(rootDir, 'src/main/java/Bar.java')
        badCode << '''
        |public class Bar {
        |  public int unreadField = 42; // warning: URF_UNREAD_FIELD
        |}
        |'''.stripMargin()

        when:
        def result = gradleRunner
                .withArguments('spotbugsMain')
                .buildAndFail()

        then:
        //issue 284 - information on where the report should still be printed even if suppressing stack traces.
        result.output.contains('See the report at')
    }

    def "can generate spotbugs.html in configured buildDir"() {
        buildFile << """
spotbugsMain {
    showStackTraces = false
    reports {
        html.required = true
    }
}
buildDir = 'new-build-dir'
"""
        when:
        def result = gradleRunner
                .withArguments('spotbugsMain')
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome
        File report = rootDir.toPath().resolve("new-build-dir").resolve("reports").resolve("spotbugs").resolve("main.html").toFile()
        report.isFile()
    }

    def "can generate spotbugs.xml in configured buildDir"() {
        buildFile << """
spotbugsMain {
    reports {
        xml.required = true
    }
}
buildDir = 'new-build-dir'
"""
        when:
        def result = gradleRunner
                .withArguments('spotbugsMain')
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome
        File report = rootDir.toPath().resolve("new-build-dir").resolve("reports").resolve("spotbugs").resolve("main.xml").toFile()
        report.isFile()
    }

    def "can generate spotbugs.html"() {
        buildFile << """
spotbugsMain {
    reports {
        html.required = true
    }
}"""
        when:
        def result = gradleRunner
                .withArguments('spotbugsMain', '--debug')
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome
        File report = rootDir.toPath().resolve("build").resolve("reports").resolve("spotbugs").resolve("main.html").toFile()
        report.isFile()
        result.getOutput().contains("-html=")
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
            required = true
            stylesheet = resources.text.fromArchiveEntry(configurations.spotbugsStylesheets, 'fancy-hist.xsl')
        }
    }
}"""
        when:
        def result = gradleRunner
                .withArguments('spotbugsMain', "--debug")
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome
        File report = rootDir.toPath().resolve("build").resolve("reports").resolve("spotbugs").resolve("main.html").toFile()
        report.isFile()
        result.getOutput().contains("-html:")
        // confirm -projectName is working
        report.readLines("utf-8").find({line -> line.contains("sample-project (spotbugsMain)")}) != null
        // confirm -release is working
        report.readLines("utf-8").find({line -> line.contains("1.2.3")}) != null
    }

    def "can generate spotbugs.html with the path of stylesheet"() {
        buildFile << """
// https://github.com/spotbugs/spotbugs-gradle-plugin/issues/107#issue-408724750
spotbugsMain {
    reports {
        html {
            required = true
            stylesheet = 'fancy-hist.xsl'
        }
    }
}"""
        when:
        def result = gradleRunner
                .withArguments('spotbugsMain', '--debug')
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome
        File report = rootDir.toPath().resolve("build").resolve("reports").resolve("spotbugs").resolve("main.html").toFile()
        report.isFile()
        result.getOutput().contains("-html:")
    }

    def "can generate spotbugs.xml"() {
        buildFile << """
spotbugsMain {
    reports {
        xml.required = true
    }
}"""
        when:
        def result = gradleRunner
                .withArguments('spotbugsMain')
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome
        File report = rootDir.toPath().resolve("build").resolve("reports").resolve("spotbugs").resolve("main.xml").toFile()
        report.isFile()
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
        def result = gradleRunner
                .withArguments('spotbugsMain')
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome
        File reportsDir = rootDir.toPath().resolve("build").resolve("spotbugs").toFile()
        reportsDir.isDirectory()
        File report = reportsDir.toPath().resolve("main.txt").toFile()
        report.isFile()
    }

    def "reports error when set unknown report type"() {
        buildFile << """
spotbugsMain {
    reports {
        unknown.required = true
    }
}"""
        when:
        def result = gradleRunner
                .withArguments('spotbugsMain')
                .buildAndFail()

        then:
        result.getTasks().isEmpty()
        result.getOutput().contains("unknown is invalid as the report name")
    }

    def "can run task by Worker Process"() {
        when:
        def result = gradleRunner
                .withArguments('spotbugsMain')
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome
        result.output.contains("Running SpotBugs by Gradle no-isolated Worker...") || result.output.contains("Running SpotBugs by Gradle process-isolated Worker...")
    }

    def "can run task by JavaExec by gradle.properties"() {
        new File(rootDir, "gradle.properties") << """
com.github.spotbugs.snom.worker=false
"""
        when:
        def result = gradleRunner
                .withArguments('spotbugsMain')
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome
        result.getOutput().contains("Running SpotBugs by JavaExec...")
    }

    def "can run task by JavaExec by commandline option"() {
        when:
        def result = gradleRunner
                .withArguments('spotbugsMain', '-Pcom.github.spotbugs.snom.worker=false')
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome
        result.getOutput().contains("Running SpotBugs by JavaExec...")
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
        def result = gradleRunner
                .withArguments('spotbugsMain')
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome
    }

    def "can use configuration configured via reporting extension"() {
        setup:
        buildFile << """
spotbugsMain {
    reports {
        text.required = true
    }
}
reporting {
    baseDir "\$buildDir/our-reports"
}"""
        when:
        def result = gradleRunner
                .withArguments('spotbugsMain')
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome
        File reportsDir = rootDir.toPath().resolve("build").resolve("our-reports").resolve("spotbugs").toFile()
        reportsDir.isDirectory()
        File report = reportsDir.toPath().resolve("main.txt").toFile()
        report.isFile()
    }

    def "can generate spotbugs.sarif"() {
        buildFile << """
spotbugsMain {
    reports {
        sarif.required = true
    }
}"""
        when:
        def result = gradleRunner
                .withArguments('spotbugsMain')
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome
        File report = rootDir.toPath().resolve("build").resolve("reports").resolve("spotbugs").resolve("main.sarif").toFile()
        report.isFile()
    }

    def "can generate XML and SARIF reports"() {
        buildFile << """
spotbugsMain {
    reports {
        xml.required = true
        sarif.required = true
    }
}"""
        when:
        def result = gradleRunner
                .withArguments('spotbugsMain')
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome

        Path reportDir = rootDir.toPath().resolve("build").resolve("reports").resolve("spotbugs")
        reportDir.resolve("main.xml").toFile().isFile()
        reportDir.resolve("main.sarif").toFile().isFile()
    }

    def "can disable XML report"() {
        buildFile << """
spotbugsMain {
    reports {
        xml.required = false
    }
}"""
        when:
        def result = gradleRunner
                .withArguments('spotbugsMain')
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome

        Path reportDir = rootDir.toPath().resolve("build").resolve("reports").resolve("spotbugs")
        !reportDir.resolve("main.xml").toFile().isFile()
    }

    def "can reconfigure a report"() {
        buildFile << """
spotbugsMain {
    reports {
        xml.required = true
    }

    reports {
        xml.required = false
    }
}"""
        when:
        def result = gradleRunner
                .withArguments('spotbugsMain')
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome

        Path reportDir = rootDir.toPath().resolve("build").resolve("reports").resolve("spotbugs")
        !reportDir.resolve("main.xml").toFile().isFile()
    }
}
