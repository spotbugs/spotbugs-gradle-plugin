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
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.process.JavaExecSpec
import org.gradle.process.internal.ExecException
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.util.stream.Collectors
import javax.inject.Inject

class SpotBugsRunnerForJavaExec @Inject constructor(
    private val javaLauncher: Property<JavaLauncher>,
) : SpotBugsRunner() {
    private val log = LoggerFactory.getLogger(SpotBugsRunnerForJavaExec::class.java)
    private lateinit var stderrOutputScanner: OutputScanner

    override fun run(task: SpotBugsTask) {
        // TODO print version of SpotBugs and Plugins
        try {
            task.project.javaexec(configureJavaExec(task)).rethrowFailure().assertNormalExitValue()
            if (stderrOutputScanner.isFailedToReport && !task.getIgnoreFailures()) {
                throw GradleException("SpotBugs analysis succeeded but report generation failed")
            }
        } catch (e: ExecException) {
            if (task.getIgnoreFailures()) {
                log.warn(
                    "SpotBugs reported failures",
                    if (task.showStackTraces.get()) {
                        e
                    } else {
                        null
                    },
                )
            } else {
                val errorMessage = buildString {
                    append("Verification failed: SpotBugs execution thrown exception.")
                    val reportPaths =
                        task.getEnabledReports().stream()
                            .map(SpotBugsReport::getOutputLocation)
                            .map(RegularFileProperty::getAsFile)
                            .map {
                                it.get()
                            }
                            .map(File::toPath)
                            .map(Path::toUri)
                            .map(URI::toString)
                            .collect(Collectors.toList())
                    if (reportPaths.isNotEmpty()) {
                        append("See the report at: ")
                        append(reportPaths.joinToString(","))
                    }
                }
                throw GradleException(errorMessage, e)
            }
        }
    }

    private fun configureJavaExec(task: SpotBugsTask): Action<JavaExecSpec> {
        return Action { spec ->
            val args = mutableListOf<String>()
            args.add("-exitcode")
            args.addAll(buildArguments(task))
            spec.classpath(task.spotbugsClasspath)
            spec.jvmArgs = buildJvmArguments(task)
            spec.mainClass.set("edu.umd.cs.findbugs.FindBugs2")
            spec.setArgs(args)
            val maxHeapSize = task.maxHeapSize.getOrNull()
            if (maxHeapSize != null) {
                spec.maxHeapSize = maxHeapSize
            }
            stderrOutputScanner = OutputScanner(System.err)
            spec.setErrorOutput(stderrOutputScanner)
            if (javaLauncher.isPresent) {
                log.info(
                    "Spotbugs will be executed using Java Toolchain configuration: Vendor: {} | Version: {}",
                    javaLauncher.get().metadata.vendor,
                    javaLauncher.get().metadata.languageVersion.asInt(),
                )
                spec.executable = javaLauncher.get().executablePath.asFile.absolutePath
            }
        }
    }
}
