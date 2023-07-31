package com.github.spotbugs.snom

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

class Issue910 extends Specification {
    Path rootDir
    Path buildFile

    def setup() {
        rootDir = Files.createTempDirectory("spotbugs-gradle-plugin")
        buildFile = rootDir.resolve("build.gradle.kts")
        buildFile.toFile() << """
plugins {
  id("com.github.spotbugs")
  java
}
repositories {
  mavenCentral()
}
dependencies {
  spotbugsPlugins("com.mebigfatguy.sb-contrib:sb-contrib:7.6.0")
}
        """
        Path sourceDir = rootDir.resolve("src").resolve("main").resolve("java")
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

    def "can build with the sb-contrib 7-6-0"() {
        when:
        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(rootDir.toFile())
                        .withArguments("check")
                        .withPluginClasspath()
                        .forwardOutput()
                        .build()

        then:
        result.task(":spotbugsMain").outcome == TaskOutcome.SUCCESS
    }
}
