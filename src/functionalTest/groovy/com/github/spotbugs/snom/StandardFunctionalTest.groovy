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
import spock.lang.Ignore
import spock.lang.IgnoreIf
import spock.lang.Unroll

import java.nio.file.Paths

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class StandardFunctionalTest extends BaseFunctionalTest {
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

    def "can create spotbugsMain task depending on classes task"() {
        when:
        BuildResult result = gradleRunner
                .withArguments(":spotbugsMain")
                .build()

        then:
        SUCCESS == result.task(":classes").outcome
        SUCCESS == result.task(":spotbugsMain").outcome
    }

    def "can be listed in the task list"() {
        when:
        BuildResult result = gradleRunner
                .withArguments(":tasks")
                .build()

        then:
        result.task(":tasks").outcome == SUCCESS
        result.output.contains("spotbugsMain - Run SpotBugs analysis for the source set 'main'")
        result.output.contains("spotbugsTest - Run SpotBugs analysis for the source set 'test'")
    }

    @IgnoreIf({ !jvm.java11 })
    def "can use the specified SpotBugs version"() {
        setup:
        buildFile << """
dependencies {
    spotbugs "com.github.spotbugs:spotbugs:4.0.0-beta4"
}"""
        when:
        BuildResult result = gradleRunner
                .withArguments(":spotbugsMain")
                .build()

        then:
        SUCCESS == result.task(":classes").outcome
        result.output.contains("SpotBugs 4.0.0-beta4") || result.output.contains("spotbugs-4.0.0-beta4.jar")
    }

    def "can skip analysis when no class file we have"() {
        setup:
        File sourceDir = rootDir.toPath().resolve("src").resolve("main").resolve("java").toFile()
        File sourceFile = new File(sourceDir, "Foo.java")
        sourceFile.delete()

        when:
        BuildResult result = gradleRunner
                .withArguments(":spotbugsMain")
                .build()

        then:
        TaskOutcome.NO_SOURCE == result.task(":spotbugsMain").outcome
    }

    def "can use effort and reportLevel"() {
        buildFile << """
import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
spotbugsMain {
    // https://discuss.kotlinlang.org/t/bug-cannot-use-kotlin-enum-from-groovy/1521
    // https://touk.pl/blog/2018/05/28/testing-kotlin-with-spock-part-2-enum-with-instance-method/
    effort = Effort.valueOf('MIN')
    reportLevel = Confidence.valueOf('HIGH')
}"""
        when:
        def result = gradleRunner
                .withArguments('spotbugsMain')
                .build()

        then:
        result.task(":spotbugsMain").outcome == SUCCESS
        result.getOutput().contains("-effort:min")
        result.getOutput().contains("-high")
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
        def result = gradleRunner
                .withArguments('spotbugsMain')
                .build()

        then:
        SUCCESS == result.task(":spotbugsMain").outcome
        result.getOutput().contains("SpotBugsMain ignores failures? true")
    }

    def "is cache-able"() {
        buildFile << """
spotbugsMain {
    reports {
        text.required = true
    }
}"""

        when:
        gradleRunner
                .withArguments(":spotbugsMain", "--no-configuration-cache")
                .build()
        def result = gradleRunner
                .withArguments(":spotbugsMain")
                .build()

        then:
        TaskOutcome.UP_TO_DATE == result.task(":classes").outcome
        TaskOutcome.UP_TO_DATE == result.task(":spotbugsMain").outcome
    }

    def 'ignore missing classes'() {
        given:
        def code = new File(rootDir, 'src/main/java/Bar.java')
        code << '''
        |public class Bar {
        |  Foo test() {
        |    return new Foo();
        |  }
        |}
        |'''.stripMargin()

        buildFile << '''
        |task removeFoo(type: Delete) {
        |  delete file('build/classes/java/main/Foo.class')
        |}
        |'''.stripMargin()

        when:
        def arguments = [
            ':compileJava',
            ':removeFoo',
            ':spotbugsMain',
            '-is'
        ]
        def runner = gradleRunner
                .withArguments(arguments)

        def result = runner.build()

        then:
        result.task(':spotbugsMain').outcome == SUCCESS
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
        def runner = gradleRunner
                .withArguments(arguments)

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
    showStackTraces = true
}"""
        when:
        def arguments = [':spotbugsMain', '-is']
        if(!isWorkerApi) {
            arguments.add('-Pcom.github.spotbugs.snom.worker=false')
        }
        def runner = gradleRunner
                .withArguments(arguments)

        def result = runner. build()

        then:
        result.task(':spotbugsMain').outcome == SUCCESS
        result.output.contains('\tat ') || result.output.contains('SpotBugs ended with exit code 1')

        where:
        isWorkerApi << [true, false]
    }

    @Unroll
    def 'build does not show stack traces when bugs are found with `showStacktraces = false` (Worker API? #isWorkerApi)'() {
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
    showStackTraces = false
}"""
        when:
        def arguments = [':spotbugsMain', '-is']
        if(!isWorkerApi) {
            arguments.add('-Pcom.github.spotbugs.snom.worker=false')
        }
        def runner = gradleRunner
                .withArguments(arguments)

        def result = runner. build()

        then:
        result.task(':spotbugsMain').outcome == SUCCESS
        !(result.output.contains('\tat '))

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
        def runner = gradleRunner
                .withArguments(':spotbugsMain', '-is')

        def result = runner. build()

        then:
        result.task(':spotbugsMain').outcome == SUCCESS
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
        def runner = gradleRunner
                .withArguments(':spotbugsMain', ':spotbugsTest')

        def result = runner.build()

        then:
        SUCCESS == result.task(':spotbugsMain').outcome
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
        text.required = true
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
        def runner = gradleRunner
                .withArguments(':spotbugsAnother')

        def result = runner.build()

        then:
        SUCCESS == result.task(':spotbugsAnother').outcome
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
        BuildResult result = gradleRunner
                .withArguments(":spotbugsMain")
                .build()

        then:
        result.task(":classes").outcome == SUCCESS
        result.task(":spotbugsMain").outcome == TaskOutcome.NO_SOURCE
    }

    def "can run analysis when check task is triggered"() {
        when:
        BuildResult result = gradleRunner
                .withArguments("clean", "check")
                .build()

        then:
        result.task(":spotbugsMain").outcome == SUCCESS
    }

    @Ignore("because output does not contain the message generated by SpotBugs")
    def "can apply plugin"() {
        given:
        buildFile << """
dependencies{
  spotbugsPlugins 'com.h3xstream.findsecbugs:findsecbugs-plugin:1.11.0'
}"""
        when:
        BuildResult result = gradleRunner
                .withArguments("spotbugsMain")
                .build()

        then:
        result.task(":spotbugsMain").outcome == SUCCESS
        result.output.contains("Applying com.h3xstream.findsecbugs.PredictableRandomDetector to Foo")
        !result.output.contains("Trying to add already registered factory")
    }

    @Ignore("because output does not contain the message generated by SpotBugs")
    def "can apply plugin to multiple tasks"() {
        given:
        buildFile << """
spotbugs {
  ignoreFailures = true
}
dependencies {
  spotbugsPlugins 'com.h3xstream.findsecbugs:findsecbugs-plugin:1.11.0'
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
        BuildResult result = gradleRunner
                .withArguments("spotbugsMain", "spotbugsTest")
                .build()

        then:
        result.task(":spotbugsMain").outcome == SUCCESS
        result.task(":spotbugsTest").outcome == SUCCESS
        result.output.contains("Applying com.h3xstream.findsecbugs.PredictableRandomDetector to Foo")
        result.output.contains("Applying com.h3xstream.findsecbugs.PredictableRandomDetector to FooTest")
        !result.output.contains("Trying to add already registered factory")
    }

    def "can apply plugin using useAuxclasspathFile flag by default"() {
        given:
        buildFile << """
dependencies {
  implementation 'com.google.guava:guava:19.0'
}"""

        File sourceDir = rootDir.toPath().resolve(Paths.get("src", "main", "java")).toFile()
        sourceDir.mkdirs()
        File sourceFile = new File(sourceDir, "MyFoo.java")
        sourceFile << """
public class MyFoo {
    public static void main(String... args) {
        java.util.Map items = com.google.common.collect.ImmutableMap.of("coin", 3, "glass", 4, "pencil", 1);

                        items.entrySet()
                                .stream()
                                .forEach(System.out::println);
    }
}
"""

        when:
        BuildResult result = gradleRunner
                .withArguments('--debug', "--no-configuration-cache", "spotbugsMain")
                .build()

        then:
        result.task(":spotbugsMain").outcome == SUCCESS
        result.output.contains("Using auxclasspath file")
        def expectedOutput = File.separator + "build" + File.separator + "spotbugs" + File.separator + "auxclasspath" + File.separator + "spotbugsMain"
        result.output.contains(expectedOutput)

        when:
        BuildResult repeatedResult = gradleRunner
                .withArguments("spotbugsMain", '--rerun-tasks', '-s')
                .build()

        then:
        repeatedResult.task(":spotbugsMain").outcome == SUCCESS
    }

    def "can apply plugin using useAuxclasspathFile flag in parallel"() {
        given:
        buildFile << """
spotbugs {
  useAuxclasspathFile = true
}
dependencies {
  implementation 'com.google.guava:guava:19.0'
  testImplementation 'junit:junit:4.12'
}"""

        File sourceDir = rootDir.toPath().resolve(Paths.get("src", "main", "java")).toFile()
        sourceDir.mkdirs()
        File sourceFile = new File(sourceDir, "MyFoo.java")
        sourceFile << """
public class MyFoo {
    public static void main(String... args) {
        java.util.Map items = com.google.common.collect.ImmutableMap.of("coin", 3, "glass", 4, "pencil", 1);

                        items.entrySet()
                                .stream()
                                .forEach(System.out::println);
    }
}
"""

        File testSourceDir = rootDir.toPath().resolve(Paths.get("src", "test", "java")).toFile()
        testSourceDir.mkdirs()
        File testSourceFile = new File(testSourceDir, "SimpleTest.java")
        testSourceFile << """
import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;

public class SimpleTest {
    @Test
    public void testEmptyCollection() {
        Collection collection = new ArrayList();
        assertTrue(collection.isEmpty());
    }
}
"""

        when:
        BuildResult result = gradleRunner
                .withArguments('--debug', '--parallel', "spotbugsMain", "spotbugsTest")
                .build()

        then:
        result.task(":spotbugsMain").outcome == SUCCESS
        result.output.contains("Using auxclasspath file")
        def expectedOutputMain = File.separator + "build" + File.separator + "spotbugs" + File.separator + "auxclasspath" + File.separator + "spotbugsMain"
        result.output.contains(expectedOutputMain)
        def expectedOutputTest = File.separator + "build" + File.separator + "spotbugs" + File.separator + "auxclasspath" + File.separator + "spotbugsTest"
        result.output.contains(expectedOutputTest)
    }

    @Unroll
    def 'shows report path when failures are found (Worker API? #isWorkerApi)'() {
        given:
        buildFile << """
spotbugsMain {
    reports {
        xml.required = true
    }
}"""

        def badCode = rootDir.toPath().resolve(Paths.get("src", "main", "java", "Bar.java")).toFile()
        badCode << '''
        |public class Bar {
        |  public int unreadField = 42; // warning: URF_UNREAD_FIELD
        |}
        |'''.stripMargin()

        when:
        def arguments = [':spotbugsMain']
        if(!isWorkerApi) {
            arguments.add('-Pcom.github.spotbugs.snom.worker=false')
        }
        def runner = gradleRunner
                .withArguments(arguments)

        def result = runner.buildAndFail()

        then:
        result.task(':spotbugsMain').outcome == TaskOutcome.FAILED
        result.output.contains('See the report at')
        def expectedOutput = rootDir.toPath().toRealPath().resolve(Paths.get("build", "reports", "spotbugs", "main.xml")).toUri().toString()
        result.output.contains(expectedOutput)

        where:
        isWorkerApi << [true, false]
    }

    @Unroll
    def 'ignore bugs from baseline file (Worker API? #isWorkerApi)'() {
        given:
        def badCode = new File(rootDir, 'src/main/java/Bar.java')
        badCode << '''
        |public class Bar {
        |  public int unreadField = 42; // warning: URF_UNREAD_FIELD
        |}
        |'''.stripMargin()
        def baseline = new File(rootDir, 'baseline.xml')
        baseline << '''
        <BugCollection version="4.1.1" sequence="0" timestamp="1602489053934" analysisTimestamp="1602489053968" release="1.0">
            <BugInstance type="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD" priority="2" rank="18" abbrev="UrF" category="STYLE" instanceHash="94edf310851e6a92f2c3f91d60450ae9" instanceOccurrenceNum="0" instanceOccurrenceMax="0">
                <ShortMessage>Unread public/protected field</ShortMessage>
                <LongMessage>Unread public/protected field: Bar.unreadField</LongMessage>
                <Class classname="Bar" primary="true">
                    <SourceLine classname="Bar" start="2" end="3" sourcefile="Bar.java" sourcepath="Bar.java" relSourcepath="java/Bar.java">
                        <Message>At Bar.java:[lines 2-3]</Message>
                    </SourceLine>
                    <Message>In class Bar</Message>
                </Class>
                <Field classname="Bar" name="unreadField" signature="I" isStatic="false" primary="true">
                    <SourceLine classname="Bar" sourcefile="Bar.java" sourcepath="Bar.java" relSourcepath="java/Bar.java">
                        <Message>In Bar.java</Message>
                    </SourceLine>
                    <Message>Field Bar.unreadField</Message>
                </Field>
                <SourceLine classname="Bar" primary="true" start="3" end="3" startBytecode="7" endBytecode="7" sourcefile="Bar.java" sourcepath="Bar.java" relSourcepath="java/Bar.java">
                    <Message>At Bar.java:[line 3]</Message>
                </SourceLine>
            </BugInstance>
            <BugCategory category="STYLE">
                <Description>Dodgy code</Description>
            </BugCategory>
            <BugPattern type="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD" abbrev="UrF" category="STYLE">
                <ShortDescription>Unread public/protected field</ShortDescription>
            </BugPattern>
            <BugCode abbrev="UrF">
                <Description>Champ non lu</Description>
            </BugCode>
            <Errors errors="0" missingClasses="0"></Errors>
        </BugCollection>'''.stripMargin()
        buildFile << """
spotbugs {
  baselineFile = file('baseline.xml')
}"""

        when:
        def arguments = [':spotbugsMain']
        if(!isWorkerApi) {
            arguments.add('-Pcom.github.spotbugs.snom.worker=false')
        }
        def runner = gradleRunner
                .withArguments(arguments)

        def result = runner.build()

        then:
        result.task(':spotbugsMain').outcome == SUCCESS

        where:
        isWorkerApi << [true, false]
    }

    def "can analyse classes when reportLevel = DEFAULT"() {
        given:
        buildFile << """
spotbugs {
    // https://discuss.kotlinlang.org/t/bug-cannot-use-kotlin-enum-from-groovy/1521
    // https://touk.pl/blog/2018/05/28/testing-kotlin-with-spock-part-2-enum-with-instance-method/
    reportLevel = com.github.spotbugs.snom.Confidence.valueOf('DEFAULT')
}
"""

        when:
        BuildResult result = gradleRunner
                .withArguments("build")
                .build()

        then:
        result.task(":compileJava").outcome == SUCCESS
        result.task(":spotbugsMain").outcome == SUCCESS
    }
}
