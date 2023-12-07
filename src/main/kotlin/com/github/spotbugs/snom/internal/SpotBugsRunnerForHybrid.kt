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
package com.github.spotbugs.snom.internal

import com.github.spotbugs.snom.SpotBugsReport
import com.github.spotbugs.snom.SpotBugsTask
import java.io.File
import java.net.URI
import java.nio.file.Path
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.process.ExecOperations
import org.gradle.process.JavaExecSpec
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.slf4j.LoggerFactory

/**
 * This class is an implementation of [SpotBugsRunner] that executes the SpotBugs process from a worker process.
 * It leverages the benefits of both [org.gradle.api.Project.javaexec] and the Worker API to optimize
 * SpotBugs' performance.
 * This approach allows SpotBugs to utilize more Java heap memory and reduces its lifespan, enhancing efficiency.
 *
 * For more context, refer to GitHub issue:
 * [Issue #416](https://github.com/spotbugs/spotbugs-gradle-plugin/issues/416).
 */
internal class SpotBugsRunnerForHybrid(
    private val workerExecutor: WorkerExecutor,
    private val javaLauncher: Property<JavaLauncher>,
) : SpotBugsRunner() {
    override fun run(task: SpotBugsTask) {
        workerExecutor.noIsolation().submit(SpotBugsExecutor::class.java) {
            val args = mutableListOf<String>()
            args.add("-exitcode")
            args.addAll(buildArguments(task))
            it.getClasspath().setFrom(task.spotbugsClasspath)
            it.getJvmArgs().set(buildJvmArguments(task))
            it.getArgs().set(args)
            val maxHeapSize = task.maxHeapSize.getOrNull()
            if (maxHeapSize != null) {
                it.getMaxHeapSize().set(maxHeapSize)
            }
            it.getIgnoreFailures().set(task.ignoreFailures)
            it.getShowStackTraces().set(task.showStackTraces)
            task.getRequiredReports()
                .map(SpotBugsReport::getOutputLocation)
                .forEach(it.getReports()::add)
            if (javaLauncher.isPresent) {
                it.getJavaToolchainExecutablePath().set(javaLauncher.get().executablePath.asFile.absolutePath)
            }
        }
    }

    interface SpotBugsWorkParameters : WorkParameters {
        fun getClasspath(): ConfigurableFileCollection

        fun getMaxHeapSize(): Property<String>

        fun getArgs(): ListProperty<String>

        fun getJvmArgs(): ListProperty<String>

        fun getIgnoreFailures(): Property<Boolean>

        fun getShowStackTraces(): Property<Boolean>

        fun getJavaToolchainExecutablePath(): Property<String>

        fun getReports(): ListProperty<RegularFile>
    }

    abstract class SpotBugsExecutor @Inject constructor(
        private val execOperations: ExecOperations,
    ) : WorkAction<SpotBugsWorkParameters> {
        private val log = LoggerFactory.getLogger(javaClass)
        private lateinit var stderrOutputScanner: OutputScanner

        override fun execute() {
            // TODO print version of SpotBugs and Plugins
            val exitValue = execOperations.javaexec(configureJavaExec(parameters)).rethrowFailure().exitValue
            val ignoreFailures = parameters.getIgnoreFailures().getOrElse(false)
            if (ignoreMissingClassFlag(exitValue) == 0) {
                if (stderrOutputScanner.isFailedToReport && !ignoreFailures) {
                    throw GradleException("SpotBugs analysis succeeded but report generation failed")
                }
                return
            }

            if (ignoreFailures) {
                log.warn("SpotBugs ended with exit code $exitValue")
                return
            }

            val errorMessage = buildString {
                append("Verification failed: SpotBugs ended with exit code $exitValue.")
                val reportPaths = parameters.getReports().get().asSequence()
                    .map(RegularFile::getAsFile)
                    .map(File::toPath)
                    .map(Path::toUri)
                    .map(URI::toString)
                    .toList()
                if (reportPaths.isNotEmpty()) {
                    append(" See the report at: ")
                    append(reportPaths.joinToString())
                }
            }
            throw GradleException(errorMessage)
        }

        private fun ignoreMissingClassFlag(exitValue: Int): Int {
            if ((exitValue.and(MISSING_CLASS_FLAG)) == 0) {
                return exitValue
            }
            log.debug(
                "MISSING_CLASS_FLAG (2) was set to the exit code, but ignore it to keep the task result stable.",
            )
            return (exitValue.xor(MISSING_CLASS_FLAG))
        }

        private fun configureJavaExec(params: SpotBugsWorkParameters) = Action<JavaExecSpec> {
            it.jvmArgs = params.getJvmArgs().get()
            it.classpath(params.getClasspath())
            it.setArgs(params.getArgs().get())
            it.mainClass.set("edu.umd.cs.findbugs.FindBugs2")
            val maxHeapSize = params.getMaxHeapSize().getOrNull()
            if (maxHeapSize != null) {
                it.maxHeapSize = maxHeapSize
            }
            if (params.getJavaToolchainExecutablePath().isPresent) {
                log.info(
                    "Spotbugs will be executed using Java Toolchain configuration: {}",
                    params.getJavaToolchainExecutablePath().get(),
                )
                it.executable = params.getJavaToolchainExecutablePath().get()
            }
            it.setIgnoreExitValue(true)
            stderrOutputScanner = OutputScanner(System.err)
            it.setErrorOutput(stderrOutputScanner)
        }
    }

    companion object {
        /**
         * Exit code which is set when classes needed for analysis were missing.
         *
         * See [Constant Field Values from javadoc of the SpotBugs](https://javadoc.io/static/com.github.spotbugs/spotbugs/4.4.2/constant-values.html#edu.umd.cs.findbugs.ExitCodes.MISSING_CLASS_FLAG)
         */
        @Suppress("MaxLineLength")
        private const val MISSING_CLASS_FLAG = 2
    }
}
