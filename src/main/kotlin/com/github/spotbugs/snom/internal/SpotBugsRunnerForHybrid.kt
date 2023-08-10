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
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.util.stream.Collectors
import javax.inject.Inject

/**
 * A {@link SpotBugsRunner} implementation that runs SpotBugs process from the worker process. This
 * approach enables applying benefit of both {@link org.gradle.api.Project#javaexec(Closure)} and
 * Worker API: provide larger Java heap to SpotBugs process and shorten their lifecycle.
 *
 * @see <a href="https://github.com/spotbugs/spotbugs-gradle-plugin/issues/416">The related GitHub
 *     issue</a>
 */
class SpotBugsRunnerForHybrid(
    private val workerExecutor: WorkerExecutor,
    private val javaLauncher: Property<JavaLauncher>,
) : SpotBugsRunner() {

    override fun run(task: SpotBugsTask) {
        workerExecutor.noIsolation().submit(SpotBugsExecutor::class.java) { params: SpotBugsWorkParameters ->
            val args = mutableListOf<String>()
            args.add("-exitcode")
            args.addAll(buildArguments(task))
            params.getClasspath().setFrom(task.spotbugsClasspath)
            params.getJvmArgs().set(buildJvmArguments(task))
            params.getArgs().set(args)
            val maxHeapSize = task.maxHeapSize.getOrNull()
            if (maxHeapSize != null) {
                params.getMaxHeapSize().set(maxHeapSize)
            }
            params.getIgnoreFailures().set(task.ignoreFailures)
            params.getShowStackTraces().set(task.showStackTraces)
            task.getEnabledReports().stream()
                .map(SpotBugsReport::getOutputLocation)
                .forEach(params.getReports()::add)
            if (javaLauncher.isPresent) {
                params
                    .getJavaToolchainExecutablePath()
                    .set(javaLauncher.get().executablePath.asFile.absolutePath)
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
        private val log = LoggerFactory.getLogger(this.javaClass)
        private lateinit var stderrOutputScanner: OutputScanner

        override fun execute() {
            // TODO print version of SpotBugs and Plugins
            val exitValue =
                execOperations.javaexec(configureJavaExec(parameters)).rethrowFailure().exitValue
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
                val reportPaths =
                    parameters.getReports().get().stream()
                        .map(RegularFile::getAsFile)
                        .map(File::toPath)
                        .map(Path::toUri)
                        .map(URI::toString)
                        .collect(Collectors.toList())
                if (reportPaths.isNotEmpty()) {
                    append(" See the report at: ")
                    append(reportPaths.joinToString(","))
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

        private fun configureJavaExec(params: SpotBugsWorkParameters): Action<JavaExecSpec> {
            return Action { spec ->
                spec.jvmArgs = params.getJvmArgs().get()
                spec.classpath(params.getClasspath())
                spec.setArgs(params.getArgs().get())
                spec.mainClass.set("edu.umd.cs.findbugs.FindBugs2")
                val maxHeapSize = params.getMaxHeapSize().getOrNull()
                if (maxHeapSize != null) {
                    spec.maxHeapSize = maxHeapSize
                }
                if (params.getJavaToolchainExecutablePath().isPresent) {
                    log.info(
                        "Spotbugs will be executed using Java Toolchain configuration: {}",
                        params.getJavaToolchainExecutablePath().get(),
                    )
                    spec.executable = params.getJavaToolchainExecutablePath().get()
                }
                spec.setIgnoreExitValue(true)
                stderrOutputScanner = OutputScanner(System.err)
                spec.setErrorOutput(stderrOutputScanner)
            }
        }
    }

    companion object {
        /**
         * Exit code which is set when classes needed for analysis were missing.
         *
         * @see <a
         *     href="https://javadoc.io/static/com.github.spotbugs/spotbugs/4.4.2/constant-values.html#edu.umd.cs.findbugs.ExitCodes.MISSING_CLASS_FLAG">Constant
         *     Field Values from javadoc of the SpotBugs</a>
         */
        private const val MISSING_CLASS_FLAG = 2
    }
}
