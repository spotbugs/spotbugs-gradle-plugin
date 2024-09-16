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

import com.github.spotbugs.snom.internal.SpotBugsHtmlReport
import com.github.spotbugs.snom.internal.SpotBugsRunnerForHybrid
import com.github.spotbugs.snom.internal.SpotBugsRunnerForJavaExec
import com.github.spotbugs.snom.internal.SpotBugsSarifReport
import com.github.spotbugs.snom.internal.SpotBugsTextReport
import com.github.spotbugs.snom.internal.SpotBugsXmlReport
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.VerificationTask
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkerExecutor
import org.slf4j.LoggerFactory

/**
 * This Gradle task runs the SpotBugs analysis.
 * All properties are optional.
 *
 * **Usage for Java projects:**
 * After applying the SpotBugs Gradle plugin to your project, a [SpotBugsTask] is automatically
 * generated for each sourceSet.
 * If you need to configure the generated tasks, use the following build script as a guide:
 *
 * ```groovy
 * spotbugsMain {
 *     sourceSets = sourceSets.main.allSource.srcDirs
 *     classDirs = sourceSets.main.output
 *     auxClassPaths = sourceSets.main.compileClasspath
 *     ignoreFailures = false
 *     showStackTraces = true
 *     showProgress = false
 *     reportLevel = 'default'
 *     effort = 'default'
 *     visitors = [ 'FindSqlInjection', 'SwitchFallthrough' ]
 *     omitVisitors = [ 'FindNonShortCircuit' ]
 *     reportsDir = file("$buildDir/reports/spotbugs")
 *     includeFilter = file('spotbugs-include.xml')
 *     excludeFilter = file('spotbugs-exclude.xml')
 *     baselineFile = file('spotbugs-baseline.xml')
 *     onlyAnalyze = ['com.foobar.MyClass', 'com.foobar.mypkg.*']
 *     projectName = name
 *     release = version
 *     extraArgs = [ '-nested:false' ]
 *     jvmArgs = [ '-Duser.language=ja' ]
 *     maxHeapSize = '512m'
 * ```
 *
 * See also [SpotBugs Manual about configuration](https://spotbugs.readthedocs.io/en/stable/running.html).
 */
@CacheableTask
abstract class SpotBugsTask : DefaultTask(), VerificationTask {
    private val log = LoggerFactory.getLogger(SpotBugsTask::class.java)

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @get:Inject
    abstract val execOps: ExecOperations

    @Input
    override fun getIgnoreFailures(): Boolean = ignoreFailures.get()

    override fun setIgnoreFailures(ignoreFailures: Boolean) {
        this.ignoreFailures.set(ignoreFailures)
    }

    private val ignoreFailures = project.objects.property(Boolean::class.java)

    @get:Input
    abstract val showStackTraces: Property<Boolean>

    /**
     * Property to enable progress reporting during the analysis. Default value is `false`.
     */
    @get:Optional
    @get:Input
    abstract val showProgress: Property<Boolean>

    /**
     * Property to specify the level to report bugs. Default value is [Confidence.DEFAULT].
     */
    @get:Input
    @get:Optional
    abstract val reportLevel: Property<Confidence>

    /**
     * Property to adjust SpotBugs detectors. Default value is [Effort.DEFAULT].
     */
    @get:Input
    @get:Optional
    abstract val effort: Property<Effort>

    /**
     * Property to enable visitors (detectors) for analysis. Default is empty that means all visitors run analysis.
     */
    @get:Input
    abstract val visitors: ListProperty<String>

    /**
     * Property to disable visitors (detectors) for analysis. Default is empty that means SpotBugs omits no visitor.
     */
    @get:Input
    abstract val omitVisitors: ListProperty<String>

    /**
     * Property to set the directory to generate report files. Default is `"$buildDir/reports/spotbugs/$taskName"}`.
     */
    @get:Internal("Refer the destination of each report instead.")
    abstract val reportsDir: DirectoryProperty

    /**
     * Property defined to keep the backward compatibility with [org.gradle.api.reporting.Reporting] interface.
     *
     * See [SpotBugsReport]
     */
    @get:Internal
    val reports: NamedDomainObjectContainer<SpotBugsReport>

    /**
     * Property to set the filter file to limit which bug should be reported.
     *
     * Note that this property will NOT limit which bug should be detected. To limit the target classes to analyze,
     * use [onlyAnalyze] instead.
     * To limit the visitors (detectors) to run, use [visitors] and [omitVisitors] instead.
     *
     * See also [SpotBugs Manual about Filter file](https://spotbugs.readthedocs.io/en/stable/filter.html).
     */
    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val includeFilter: RegularFileProperty

    /**
     * Property to set the filter file to limit which bug should be reported.
     *
     * Note that this property will NOT limit which bug should be detected. To limit the target classes to analyze,
     * use [onlyAnalyze] instead.
     * To limit the visitors (detectors) to run, use [visitors] and [omitVisitors] instead.
     *
     * See also [SpotBugs Manual about Filter file](https://spotbugs.readthedocs.io/en/stable/filter.html).
     */
    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val excludeFilter: RegularFileProperty

    /**
     * Property to set the baseline file. This file is a Spotbugs result file, and all bugs reported in this file
     * will not be reported in the final output.
     */
    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val baselineFile: RegularFileProperty

    /**
     * Property to specify the target classes for analysis. Default value is empty that means all classes are analyzed.
     */
    @get:Input
    abstract val onlyAnalyze: ListProperty<String>

    /**
     * Property to specify the name of project. Some reporting formats use this property.
     * Default value is `"${project.name} (${task.name})"`.
     *
     * Note that this property, if treated as a task input, can break cacheability.
     * As such, it has been marked [Internal] to exclude it from task up-to-date and
     * cacheability checks.
     */
    @get:Internal
    abstract val projectName: Property<String>

    /**
     * Property to specify the release identifier of project. Some reporting formats use this property.
     * Default value is the version of your Gradle project.
     */
    @get:Input
    abstract val release: Property<String>

    /**
     * Property to specify the extra arguments for SpotBugs. Default value is empty so SpotBugs will get no
     * extra argument.
     */
    @get:Optional
    @get:Input
    abstract val extraArgs: ListProperty<String>

    /**
     * Property to specify the extra arguments for JVM process. Default value is empty so JVM process will get no
     * extra argument.
     */
    @get:Optional
    @get:Input
    abstract val jvmArgs: ListProperty<String>

    /**
     * Property to specify the max heap size (`-Xmx` option) of JVM process.
     * Default value is empty so the default configuration made by Gradle will be used.
     */
    @get:Optional
    @get:Input
    abstract val maxHeapSize: Property<String>

    /**
     * Property to specify the directories that contain the source of target classes to analyze.
     * Default value is the source directory of the target sourceSet.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDirs: ConfigurableFileCollection

    /**
     * Property to specify the directories that contains the target classes to analyze.
     * Default value is the output directory of the target sourceSet.
     */
    @get:Internal
    abstract val classDirs: ConfigurableFileCollection

    /**
     * Property to specify the aux class paths that contains the libraries to refer during analysis.
     * Default value is the compile-scope dependencies of the target sourceSet.
     */
    @get:Classpath
    abstract val auxClassPaths: ConfigurableFileCollection

    /**
     * Property to enable auxclasspathFromFile and prevent Argument List Too Long issues in java processes.
     * Default value is `false`.
     */
    @get:Input
    @get:Optional
    abstract val useAuxclasspathFile: Property<Boolean>

    @get:Internal
    abstract val auxclasspathFile: RegularFileProperty

    /**
     * Property to specify the target classes to analyze by SpotBugs.
     * Default value is the all existing `.class` files in `getClassDirs`.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    var classes: FileCollection? = null
        get() {
            return field ?: classDirs.asFileTree.filter {
                it.name.endsWith(".class")
            }
        }

    private var enableWorkerApi: Boolean = true

    @get:Internal
    abstract val pluginJarFiles: ConfigurableFileCollection

    @get:Internal
    abstract val spotbugsClasspath: ConfigurableFileCollection

    @get:Nested
    @get:Optional
    abstract val launcher: Property<JavaLauncher>

    /**
     * A file that lists class files and jar files to analyze.
     */
    @get:OutputFile
    abstract val analyseClassFile: RegularFileProperty

    init {
        val objects = project.objects
        reports = objects.domainObjectContainer(SpotBugsReport::class.java) { name: String ->
            when (name) {
                "html" -> objects.newInstance(SpotBugsHtmlReport::class.java, name, objects, this)
                "xml" -> objects.newInstance(SpotBugsXmlReport::class.java, name, objects, this)
                "text" -> objects.newInstance(SpotBugsTextReport::class.java, name, objects, this)
                "sarif" -> objects.newInstance(SpotBugsSarifReport::class.java, name, objects, this)
                else -> throw InvalidUserDataException("$name is invalid as the report name")
            }.also {
                (outputs as org.gradle.api.tasks.TaskOutputs).file(it.outputLocation)
            }
        }
        description = "Run SpotBugs analysis."
        group = JavaBasePlugin.VERIFICATION_GROUP
    }

    /**
     * Set properties from extension right after the task creation. User may overwrite these
     * properties by build script.
     *
     * @param extension the source extension to copy the properties.
     */
    fun init(
        extension: SpotBugsExtension,
        enableWorkerApi: Boolean,
    ) {
        auxclasspathFile.convention(project.layout.buildDirectory.file("spotbugs/auxclasspath/$name"))
        ignoreFailures.convention(extension.ignoreFailures)
        showStackTraces.convention(extension.showStackTraces)
        showProgress.convention(extension.showProgress)
        reportLevel.convention(extension.reportLevel)
        effort.convention(extension.effort)
        visitors.convention(extension.visitors)
        omitVisitors.convention(extension.omitVisitors)
        // the default reportsDir is "$buildDir/reports/spotbugs/"
        reportsDir.convention(extension.reportsDir)
        includeFilter.convention(extension.includeFilter)
        excludeFilter.convention(extension.excludeFilter)
        baselineFile.convention(extension.baselineFile)
        onlyAnalyze.convention(extension.onlyAnalyze)
        projectName.convention(extension.projectName.map { p -> "$p ($name)" })
        release.convention(extension.release)
        jvmArgs.convention(extension.jvmArgs)
        extraArgs.convention(extension.extraArgs)
        maxHeapSize.convention(extension.maxHeapSize)
        useAuxclasspathFile.convention(extension.useAuxclasspathFile)

        if (extension.useJavaToolchains.isPresent && extension.useJavaToolchains.get()) {
            configureJavaLauncher()
        }

        this.enableWorkerApi = enableWorkerApi

        analyseClassFile.set(project.layout.buildDirectory.file("$name-analyse-class-file.txt"))

        val pluginConfiguration = project.configurations.getByName(SpotBugsPlugin.PLUGINS_CONFIG_NAME)
        pluginJarFiles.from(
            project.provider { pluginConfiguration.files },
        )
        val configuration = project.configurations.getByName(SpotBugsPlugin.CONFIG_NAME)
        val spotbugsSlf4j = project.configurations.getByName(SpotBugsPlugin.SLF4J_CONFIG_NAME)
        spotbugsClasspath.from(
            project.layout.files(
                project.provider { spotbugsSlf4j.files },
                project.provider { configuration.files },
            ),
        )
    }

    /**
     * Set convention for default java launcher based on Toolchain configuration
     */
    private fun configureJavaLauncher() {
        val toolchain = project.extensions.getByType(JavaPluginExtension::class.java).toolchain
        val service = project.extensions.getByType(JavaToolchainService::class.java)
        val defaultLauncher = service.launcherFor(toolchain)
        launcher.convention(defaultLauncher)
    }

    @TaskAction
    fun run() {
        if (enableWorkerApi) {
            log.info("Running SpotBugs by Gradle no-isolated Worker...")
            SpotBugsRunnerForHybrid(workerExecutor, launcher).run(this)
        } else {
            log.info("Running SpotBugs by JavaExec...")
            SpotBugsRunnerForJavaExec(execOps, launcher).run(this)
        }
    }

    /**
     * Function defined to keep the backward compatibility with [org.gradle.api.reporting.Reporting] interface.
     */
    @Suppress("MaxLineLength")
    fun reports(configureAction: Action<NamedDomainObjectContainer<SpotBugsReport>>): NamedDomainObjectContainer<SpotBugsReport> {
        configureAction.execute(reports)
        return reports
    }

    @Internal
    fun getBaseName(): String {
        var prunedName = name.replaceFirst("spotbugs", "")
        if (prunedName.isEmpty()) {
            prunedName = name
        }

        return buildString {
            append(Character.toLowerCase(prunedName[0]))
            append(prunedName.substring(1))
        }
    }

    @Internal
    internal fun getRequiredReports(): Sequence<SpotBugsReport> {
        return reports.matching { it.required.get() }.asMap.values.asSequence()
    }
}
