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

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * [SpotBugsExtension] is an extension used to set up the SpotBugs Gradle plugin.
 * All properties in this extension act as default properties for all instances of [SpotBugsTask] are optional.
 *
 * ### Usage
 * Once you've applied the SpotBugs Gradle plugin to your project, configure it as shown below:
 *
 * ```kotlin
 * // Required: Gradle 8.2 or higher
 * spotbugs {
 *     ignoreFailures = false
 *     showStackTraces = true
 *     showProgress = true
 *     effort = com.github.spotbugs.snom.Effort.DEFAULT
 *     reportLevel = com.github.spotbugs.snom.Confidence.DEFAULT
 *     visitors = listOf("FindSqlInjection", "SwitchFallthrough")
 *     omitVisitors = listOf("FindNonShortCircuit")
 *     reportsDir = file("$buildDir/spotbugs")
 *     includeFilter = file("include.xml")
 *     excludeFilter = file("exclude.xml")
 *     baselineFile = file("baseline.xml")
 *     onlyAnalyze = listOf("com.foobar.MyClass", "com.foobar.mypkg.*")
 *     maxHeapSize = "1g"
 *     extraArgs = listOf("-nested:false")
 *     jvmArgs = listOf("-Duser.language=ja")
 *     runOnCheck = true
 * }
 * ```
 *
 * See also [SpotBugs Manual about configuration](https://spotbugs.readthedocs.io/en/stable/running.html).
 */
interface SpotBugsExtension {
    val ignoreFailures: Property<Boolean>
    val showStackTraces: Property<Boolean>

    /**
     * Property to enable progress reporting during the analysis. Default value is `false`.
     */
    val showProgress: Property<Boolean>

    /**
     * Property to specify the level to report bugs. Default value is [Confidence.DEFAULT].
     */
    val reportLevel: Property<Confidence>

    /**
     * Property to adjust SpotBugs detectors. Default value is [Effort.DEFAULT].
     */
    val effort: Property<Effort>

    /**
     * Property to enable visitors (detectors) for analysis. Default is empty that means all visitors run analysis.
     */
    val visitors: ListProperty<String>

    /**
     * Property to disable visitors (detectors) for analysis. Default is empty that means SpotBugs omits no visitor.
     */
    val omitVisitors: ListProperty<String>

    /**
     * Property to set the directory to generate report files. Default is `"$buildDir/reports/spotbugs"`.
     *
     * Note that each [SpotBugsTask] creates own subdirectory in this directory.
     */
    val reportsDir: DirectoryProperty

    /**
     * Property to set the filter file to limit which bug should be reported.
     *
     * Note that this property will NOT limit which bug should be detected. To limit the target classes to analyze,
     * use [onlyAnalyze] instead.
     * To limit the visitors (detectors) to run, use [visitors] and [omitVisitors] instead.
     *
     * See also [SpotBugs Manual about Filter file](https://spotbugs.readthedocs.io/en/stable/filter.html).
     */
    val includeFilter: RegularFileProperty

    /**
     * Property to set the filter file to limit which bug should be reported.
     *
     * Note that this property will NOT limit which bug should be detected. To limit the target classes to analyze,
     * use [onlyAnalyze] instead.
     * To limit the visitors (detectors) to run, use [visitors] and [omitVisitors] instead.
     *
     * See also [SpotBugs Manual about Filter file](https://spotbugs.readthedocs.io/en/stable/filter.html).
     */
    val excludeFilter: RegularFileProperty

    /**
     * Property to set the baseline file. This file is a Spotbugs result file, and all bugs reported in this file
     * will not be reported in the final output.
     */
    val baselineFile: RegularFileProperty

    /**
     * Property to specify the target classes for analysis. Default value is empty that means all classes are analyzed.
     */
    val onlyAnalyze: ListProperty<String>

    /**
     * Property to specify the name of project. Some reporting formats use this property.
     * Default value is the name of your Gradle project.
     */
    val projectName: Property<String>

    /**
     * Property to specify the release identifier of project. Some reporting formats use this property.
     * Default value is the version of your Gradle project.
     */
    val release: Property<String>

    /**
     * Property to specify the extra arguments for SpotBugs.
     * Default value is empty so SpotBugs will get no extra argument.
     */
    val extraArgs: ListProperty<String>

    /**
     * Property to specify the extra arguments for JVM process. Default value is empty so JVM process will get
     * no extra argument.
     */
    val jvmArgs: ListProperty<String>

    /**
     * Property to specify the max heap size (`-Xmx` option) of JVM process.
     * Default value is empty so the default configuration made by Gradle will be used.
     */
    val maxHeapSize: Property<String>

    val toolVersion: Property<String>

    val useAuxclasspathFile: Property<Boolean>

    val useJavaToolchains: Property<Boolean>

    /**
     * Property to specify if the SpotBugs tasks should automatically be marked as dependencies of the
     * check task. Defaults to true.
     */
    val runOnCheck: Property<Boolean>
}
