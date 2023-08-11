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
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import spock.lang.Ignore
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class ExtensionFunctionalTest extends Specification {
    File rootDir
    File buildFile
    String version = System.getProperty('snom.test.functional.gradle', GradleVersion.current().version)

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

    def "can use includeFilter"() {
        setup:
        File filter = new File(rootDir, "include.xml")
        buildFile << """
spotbugs {
    includeFilter = file('include.xml')
}"""
        filter << """
<FindBugsFilter></FindBugsFilter>
"""
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('spotbugsMain', '--debug')
                .withPluginClasspath()
                .withGradleVersion(version)
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome
        result.getOutput().contains("-include")
        result.getOutput().contains(filter.canonicalPath)
    }

    def "can use excludeFilter"() {
        setup:
        File filter = new File(rootDir, "exclude.xml")
        buildFile << """
spotbugs {
    excludeFilter = file('exclude.xml')
}"""
        filter << """
<FindBugsFilter></FindBugsFilter>
"""
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('spotbugsMain', '--debug')
                .withPluginClasspath()
                .withGradleVersion(version)
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome
        result.getOutput().contains("-exclude")
        result.getOutput().contains(filter.canonicalPath)
    }

    def "can use baselineFile"() {
        setup:
        File baseline = new File(rootDir, "baseline.xml")
        buildFile << """
spotbugs {
    baselineFile = file('baseline.xml')
}"""
        baseline << """
<BugCollection></BugCollection>
"""
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('spotbugsMain', '--debug')
                .withPluginClasspath()
                .withGradleVersion(version)
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome
        result.getOutput().contains("-excludeBugs")
        result.getOutput().contains(baseline.canonicalPath)
    }

    def "can use visitors"() {
        setup:
        buildFile << """
spotbugs {
    visitors = [ 'FindSqlInjection', 'SwitchFallthrough' ]
}"""
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('spotbugsMain', '--debug')
                .withPluginClasspath()
                .withGradleVersion(version)
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome
        result.getOutput().contains("-visitors, FindSqlInjection,SwitchFallthrough,")
    }

    def "can use omitVisitors"() {
        buildFile << """
spotbugs {
    omitVisitors = [ 'FindSqlInjection', 'SwitchFallthrough' ]
}"""
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('spotbugsMain', '--debug')
                .withPluginClasspath()
                .withGradleVersion(version)
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome
        result.getOutput().contains("-omitVisitors, FindSqlInjection,SwitchFallthrough,")
    }

    def "can use onlyAnalyze"() {
        buildFile << """
spotbugs {
    onlyAnalyze = ['com.foobar.MyClass', 'com.foobar.mypkg.*']
}"""
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('spotbugsMain', '--debug')
                .withPluginClasspath()
                .withGradleVersion(version)
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome
        result.getOutput().contains("-onlyAnalyze, com.foobar.MyClass,com.foobar.mypkg.*,")
    }

    def "can use extraArgs and jvmArgs"() {
        buildFile << """
spotbugs {
    extraArgs = ['-nested:false']
    jvmArgs = ['-Duser.language=ja']
}"""
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('spotbugsMain', '--debug')
                .withPluginClasspath()
                .withGradleVersion(version)
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome
        result.getOutput().contains("-nested:false")
        result.getOutput().contains("-Duser.language=ja")
    }

    def "can use maxHeapSize"() {
        buildFile << """
spotbugs {
    maxHeapSize = '256m'
}"""
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('spotbugsMain', '--debug')
                .withPluginClasspath()
                .withGradleVersion(version)
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome
        result.getOutput().contains("-Xmx256m")
    }

    def "can use effort and reportLevel"() {
        buildFile << """
import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
spotbugs {
    // https://discuss.kotlinlang.org/t/bug-cannot-use-kotlin-enum-from-groovy/1521
    // https://touk.pl/blog/2018/05/28/testing-kotlin-with-spock-part-2-enum-with-instance-method/
    effort = Effort.valueOf('LESS')
    reportLevel = Confidence.valueOf('HIGH')
}"""
        when:
        def result = GradleRunner.create()
                .withProjectDir(rootDir)
                .withArguments('spotbugsMain', '--debug')
                .withPluginClasspath()
                .withGradleVersion(version)
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome
        result.getOutput().contains("-effort:less")
        result.getOutput().contains("-high")
    }

    def "can use toolVersion to set the SpotBugs version"() {
        setup:
        buildFile << """
spotbugs {
    toolVersion = "4.0.0-beta4"
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
        TaskOutcome.SUCCESS == result.task(":spotbugsMain").outcome
        result.output.contains("SpotBugs 4.0.0-beta4") || result.output.contains("spotbugs-4.0.0-beta4.jar")
    }

    def "can use toolVersion to get the SpotBugs version"() {
        setup:
        buildFile << """
spotbugs {
    toolVersion = "4.0.2"
}
dependencies {
    compileOnly "com.github.spotbugs:spotbugs-annotations:\${spotbugs.toolVersion.get()}"
}"""
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
        result.task(":spotbugsMain").outcome == TaskOutcome.SUCCESS
        result.output.contains("com.github.spotbugs:spotbugs-annotations:4.0.2")
    }
}
