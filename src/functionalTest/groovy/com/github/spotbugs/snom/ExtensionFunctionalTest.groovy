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
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.junit.jupiter.api.Assertions.assertTrue

class ExtensionFunctionalTest extends Specification {
    File rootDir
    File buildFile

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
                .build()

        then:
        result.task(":spotbugsMain").outcome == SUCCESS
        assertTrue(result.getOutput().contains("-include"))
        assertTrue(result.getOutput().contains(filter.getAbsolutePath()))
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
                .build()

        then:
        result.task(":spotbugsMain").outcome == SUCCESS
        assertTrue(result.getOutput().contains("-exclude"))
        assertTrue(result.getOutput().contains(filter.getAbsolutePath()))
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
                .build()

        then:
        result.task(":spotbugsMain").outcome == SUCCESS
        assertTrue(result.getOutput().contains("-visitors, FindSqlInjection,SwitchFallthrough,"))
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
                .build()

        then:
        result.task(":spotbugsMain").outcome == SUCCESS
        assertTrue(result.getOutput().contains("-omitVisitors, FindSqlInjection,SwitchFallthrough,"))
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
                .build()

        then:
        result.task(":spotbugsMain").outcome == SUCCESS
        assertTrue(result.getOutput().contains("-onlyAnalyze, com.foobar.MyClass,com.foobar.mypkg.*,"))
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
                .build()

        then:
        result.task(":spotbugsMain").outcome == SUCCESS
        assertTrue(result.getOutput().contains("-nested:false"))
        assertTrue(result.getOutput().contains("-Duser.language=ja"))
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
                .build()

        then:
        result.task(":spotbugsMain").outcome == SUCCESS
        assertTrue(result.getOutput().contains("-Xmx256m"))
    }
}
