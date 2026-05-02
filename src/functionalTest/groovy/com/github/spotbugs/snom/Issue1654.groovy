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

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * Regression test for GitHub issue #1654.
 *
 * <p>Verifies that {@code reports.register()} is compatible with Gradle's parallel
 * configuration-cache serializer in a multi-project build, regardless of whether the
 * report is registered inside a {@code reports { }} block or directly on the container.
 *
 * <p>Previously, lazily-registered reports left entries in the container's internal
 * {@code pendingMap} ({@code DefaultNamedDomainObjectCollection.UnfilteredIndex}).
 * {@code reports.toList()} iterates only <em>realized</em> elements (via the unfiltered
 * {@code DefaultDomainObjectCollection.iterator()}) and therefore did <em>not</em> drain
 * that map.  When Gradle's parallel configuration-cache serializer iterated the
 * {@code pendingMap} to write it out, the serialization of each pending provider value
 * simultaneously triggered its realization — modifying the {@code LinkedHashMap} while
 * it was being iterated — causing a {@link java.util.ConcurrentModificationException}.
 *
 * @see <a href="https://github.com/spotbugs/spotbugs-gradle-plugin/issues/1654">GitHub issue #1654</a>
 */
class Issue1654 extends BaseFunctionalTest {

    /**
     * Creates the shared multi-project scaffolding (settings, root build, gradle.properties,
     * and Java source files) and writes {@code subprojectBuildContent} into each sub-project's
     * {@code build.gradle}.
     *
     * @param subprojectBuildContent the Groovy DSL to write into sub1/build.gradle and
     *     sub2/build.gradle
     */
    private void setupMultiProjectBuild(String subprojectBuildContent) {
        new File(rootDir, 'settings.gradle') << """\
rootProject.name = 'issue-1654-root'
include ':sub1'
include ':sub2'
"""

        // Root build file that applies the SpotBugs plugin.  This is required so that
        // TestKit's withPluginClasspath() injection loads the plugin into the shared
        // classloader hierarchy before the subprojects try to use 'apply plugin:'.
        new File(rootDir, 'build.gradle') << """\
plugins {
    id 'com.github.spotbugs'
}
"""

        // Enable Gradle's parallel configuration-cache serializer, which is the specific
        // incubating feature that triggered the ConcurrentModificationException.
        new File(rootDir, 'gradle.properties') << "org.gradle.configuration-cache.parallel=true\n"

        ['sub1', 'sub2'].each { sub ->
            def subDir = new File(rootDir, sub)
            subDir.mkdirs()
            new File(subDir, 'build.gradle') << subprojectBuildContent

            def sourceDir = new File(subDir, 'src/main/java')
            sourceDir.mkdirs()
            new File(sourceDir, 'Foo.java') << """\
public class Foo {
    public static void main(String... args) {
        System.out.println("Hello, SpotBugs!");
    }
}
"""
        }
    }

    /**
     * Before the fix, storing the configuration cache for a multi-project build whose
     * SpotBugs tasks use {@code reports { register() }} (the {@code reports(Action)} DSL)
     * would throw a {@link java.util.ConcurrentModificationException} during config-cache
     * serialization.  After the fix the cache is stored successfully and reused.
     */
    def "reports.register() inside reports{} does not cause ConcurrentModificationException with parallel config cache"() {
        given: "each subproject registers an XML report via the reports { } DSL block"
        // The SpotBugsTask class is not imported here: using the task-by-name DSL avoids the
        // classpath resolution issue that would occur if the root project tried to reference
        // the type directly.
        setupMultiProjectBuild("""\
apply plugin: 'java'
apply plugin: 'com.github.spotbugs'

repositories {
    mavenCentral()
}

spotbugsMain {
    reports {
        register('xml') {
            required = true
        }
    }
}
""")

        when: "first run — configuration cache is stored"
        BuildResult firstRun = gradleRunner
                .withArguments('--parallel', ':sub1:spotbugsMain', ':sub2:spotbugsMain')
                .build()

        then:
        firstRun.task(":sub1:spotbugsMain").outcome == SUCCESS
        firstRun.task(":sub2:spotbugsMain").outcome == SUCCESS
        !firstRun.output.contains("Configuration cache problems found in this build")
        firstRun.output.contains("Configuration cache entry stored.")

        when: "second run — configuration cache must be reused without errors"
        BuildResult secondRun = gradleRunner
                .withArguments('--parallel', ':sub1:spotbugsMain', ':sub2:spotbugsMain')
                .build()

        then:
        secondRun.output.contains("Configuration cache entry reused.")
    }

    /**
     * Follow-up regression test: verifies that calling {@code reports.register()} directly on
     * the task's {@code reports} property (i.e., bypassing the {@code reports(Action)} method)
     * also works correctly with the parallel configuration cache.
     *
     * <p>This pattern — {@code tasks.withType(SpotBugsTask).configureEach { task -> task.reports.register(...) }}
     * — was not covered by the original fix, which only drained {@code pendingMap} inside the
     * {@code reports(Action)} method.  The additional {@code taskGraph.whenReady} drain added
     * as a follow-up to issue #1654 ensures that {@code pendingMap} is always empty before
     * the configuration-cache serializer visits the container.
     */
    def "reports.register() directly on the container does not cause ConcurrentModificationException with parallel config cache"() {
        given: "each subproject registers an XML report directly via configureEach, bypassing reports(Action)"
        setupMultiProjectBuild("""\
apply plugin: 'java'
apply plugin: 'com.github.spotbugs'

repositories {
    mavenCentral()
}

tasks.withType(com.github.spotbugs.snom.SpotBugsTask).configureEach { task ->
    task.reports.register('xml') {
        required = true
    }
}
""")

        when: "first run — configuration cache is stored"
        BuildResult firstRun = gradleRunner
                .withArguments('--parallel', ':sub1:spotbugsMain', ':sub2:spotbugsMain')
                .build()

        then:
        firstRun.task(":sub1:spotbugsMain").outcome == SUCCESS
        firstRun.task(":sub2:spotbugsMain").outcome == SUCCESS
        !firstRun.output.contains("Configuration cache problems found in this build")
        firstRun.output.contains("Configuration cache entry stored.")

        when: "second run — configuration cache must be reused without errors"
        BuildResult secondRun = gradleRunner
                .withArguments('--parallel', ':sub1:spotbugsMain', ':sub2:spotbugsMain')
                .build()

        then:
        secondRun.output.contains("Configuration cache entry reused.")
    }
}
