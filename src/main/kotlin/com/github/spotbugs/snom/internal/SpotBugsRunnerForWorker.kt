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
import edu.umd.cs.findbugs.DetectorFactoryCollection
import edu.umd.cs.findbugs.FindBugs
import edu.umd.cs.findbugs.FindBugs2
import edu.umd.cs.findbugs.TextUICommandLine
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.process.JavaForkOptions
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.util.stream.Collectors
import javax.inject.Inject

@Deprecated("Will be removed in v6 release")
class SpotBugsRunnerForWorker @Inject constructor(
    private val workerExecutor: WorkerExecutor,
    private val javaLauncher: Property<JavaLauncher>,
) : SpotBugsRunner() {
    private val log = LoggerFactory.getLogger(SpotBugsRunnerForWorker::class.java)

    override fun run(task: SpotBugsTask) {
        val workerQueue = workerExecutor.processIsolation { spec ->
            spec.classpath.setFrom(task.spotbugsClasspath)
            spec.forkOptions { option: JavaForkOptions ->
                option.jvmArgs(buildJvmArguments(task))
                val maxHeapSize = task.maxHeapSize.getOrNull()
                if (maxHeapSize != null) {
                    option.maxHeapSize = maxHeapSize
                }
                if (javaLauncher.isPresent) {
                    log.info(
                        "Spotbugs will be executed using Java Toolchain configuration: Vendor: {} | Version: {}",
                        javaLauncher.get().metadata.vendor,
                        javaLauncher.get().metadata.languageVersion.asInt(),
                    )
                    option.executable = javaLauncher.get().executablePath.asFile.absolutePath
                }
            }
        }
        workerQueue.submit(SpotBugsExecutor::class.java) { params ->
            params.getArguments().addAll(buildArguments(task))
            params.getIgnoreFailures().set(task.getIgnoreFailures())
            params.getShowStackTraces().set(task.showStackTraces)
            task.getRequiredReports()
                .map(SpotBugsReport::getOutputLocation)
                .forEach(params.getReports()::add)
        }
    }

    interface SpotBugsWorkParameters : WorkParameters {
        fun getArguments(): ListProperty<String>

        fun getIgnoreFailures(): Property<Boolean>

        fun getShowStackTraces(): Property<Boolean>

        fun getReports(): ListProperty<RegularFile>
    }

    abstract class SpotBugsExecutor : WorkAction<SpotBugsWorkParameters> {
        private val log = LoggerFactory.getLogger(SpotBugsExecutor::class.java)

        override fun execute() {
            val args = parameters.getArguments().get().toTypedArray()
            DetectorFactoryCollection.resetInstance(DetectorFactoryCollection())

            try {
                edu.umd.cs.findbugs.Version.printVersion(false)
                FindBugs2().use { findBugs2 ->
                    val commandLine = TextUICommandLine()
                    FindBugs.processCommandLine(commandLine, args, findBugs2)
                    findBugs2.execute()

                    val message = buildString {
                        if (findBugs2.errorCount > 0) {
                            append(findBugs2.errorCount).append(" SpotBugs errors were found.")
                        }
                        if (findBugs2.bugCount > 0) {
                            if (isNotEmpty()) {
                                append(' ')
                            }
                            append(findBugs2.bugCount)
                            append(" SpotBugs violations were found.")
                        }
                    }
                    if (message.isNotEmpty()) {
                        val reportLocation = buildString {
                            val reportPaths =
                                parameters.getReports().get().stream()
                                    .map(RegularFile::getAsFile)
                                    .map(File::toPath)
                                    .map(Path::toUri)
                                    .map(URI::toString)
                                    .collect(Collectors.toList())
                            if (reportPaths.isNotEmpty()) {
                                append("See the report at: ")
                                append(reportPaths.joinToString(", "))
                            }
                        }

                        val e = GradleException(message + reportLocation)
                        if (parameters.getIgnoreFailures().getOrElse(false)) {
                            log.warn(message)
                            log.warn(reportLocation)
                            if (parameters.getShowStackTraces().getOrElse(false)) {
                                log.warn("", e)
                            }
                        } else {
                            throw e
                        }
                    }
                }
            } catch (e: GradleException) {
                throw e
            } catch (e: Exception) {
                throw GradleException("Verification failed: SpotBugs execution thrown exception", e)
            }
        }
    }
}
