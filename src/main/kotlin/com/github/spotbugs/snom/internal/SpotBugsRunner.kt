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
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.lang.Boolean
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.stream.Collectors
import kotlin.Exception
import kotlin.String
import kotlin.collections.ArrayList

abstract class SpotBugsRunner {
    private val log = LoggerFactory.getLogger(SpotBugsRunner::class.java)
    abstract fun run(task: SpotBugsTask)
    protected fun buildArguments(task: SpotBugsTask): List<String> {
        val args: MutableList<String> = ArrayList()
        val plugins = task.pluginJarFiles
        if (plugins.isPresent) {
            args.add("-pluginList")
            args.add(join(plugins.get().map { it.get().asFile }))
        }
        args.add("-timestampNow")
        if (!task.auxClassPaths.isEmpty) {
            if (task.useAuxclasspathFile.get()) {
                args.add("-auxclasspathFromFile")
                val auxClasspathFile = createFileForAuxClasspath(task)
                log.debug("Using auxclasspath file: {}", auxClasspathFile)
                args.add(auxClasspathFile)
            } else {
                args.add("-auxclasspath")
                args.add(join(task.auxClassPaths.files))
            }
        }
        if (!task.sourceDirs.isEmpty) {
            args.add("-sourcepath")
            args.add(task.sourceDirs.asPath)
        }
        if (task.showProgress.getOrElse(Boolean.FALSE)) {
            args.add("-progress")
        }
        for (report: SpotBugsReport in task.getEnabledReports()) {
            val reportFile = report.outputLocation.asFile.get()
            val dir = reportFile.parentFile
            dir.mkdirs()
            args.add(report.toCommandLineOption() + "=" + reportFile.absolutePath)
        }
        if (task.effort.isPresent) {
            args.add("-effort:" + task.effort.get().name.lowercase(Locale.getDefault()))
        }
        if (task.reportLevel.isPresent) {
            task.reportLevel.get().toCommandLineOption().ifPresent { e: String ->
                args.add(
                    e,
                )
            }
        }
        if (task.visitors.isPresent && task.visitors.get().isNotEmpty()) {
            args.add("-visitors")
            args.add(task.visitors.get().stream().collect(Collectors.joining(",")))
        }
        if (task.omitVisitors.isPresent && task.omitVisitors.get().isNotEmpty()) {
            args.add("-omitVisitors")
            args.add(task.omitVisitors.get().stream().collect(Collectors.joining(",")))
        }
        if (task.includeFilter.isPresent && task.includeFilter.get() != null) {
            args.add("-include")
            args.add(task.includeFilter.get().asFile.absolutePath)
        }
        if (task.excludeFilter.isPresent && task.excludeFilter.get() != null) {
            args.add("-exclude")
            args.add(task.excludeFilter.get().asFile.absolutePath)
        }
        if (task.baselineFile.isPresent && task.baselineFile.get() != null) {
            args.add("-excludeBugs")
            args.add(task.baselineFile.get().asFile.absolutePath)
        }
        if (task.onlyAnalyze.isPresent && task.onlyAnalyze.get().isNotEmpty()) {
            args.add("-onlyAnalyze")
            args.add(task.onlyAnalyze.get().stream().collect(Collectors.joining(",")))
        }
        args.add("-projectName")
        args.add(task.projectName.get())
        args.add("-release")
        args.add(task.release.get())
        val file = task.analyseClassFile.asFile.get()
        generateFile(task.classes ?: task.project.layout.files(), task.analyseClassFile.asFile.get())
        args.add("-analyzeFromFile")
        args.add(file.absolutePath)
        args.addAll(task.extraArgs.getOrElse(emptyList()))
        log.debug("Arguments for SpotBugs are generated: {}", args)
        return args
    }

    private fun createFileForAuxClasspath(task: SpotBugsTask): String {
        val auxClasspath = task.auxClassPaths.files.stream()
            .map { obj: File -> obj.absolutePath }
            .collect(Collectors.joining("\n"))
        try {
            val auxClasspathFile = task.auxclasspathFile
            try {
                Files.createDirectories(auxClasspathFile.parent)
                if (!Files.exists(auxClasspathFile)) {
                    Files.createFile(auxClasspathFile)
                }
                Files.write(
                    auxClasspathFile,
                    auxClasspath.toByteArray(),
                    StandardOpenOption.TRUNCATE_EXISTING,
                )
                return auxClasspathFile.normalize().toString()
            } catch (e: Exception) {
                throw GradleException(
                    "Could not create auxiliary classpath file for SpotBugsTask at " +
                        auxClasspathFile.normalize().toString(),
                    e,
                )
            }
        } catch (e: Exception) {
            throw GradleException("Could not create auxiliary classpath file for SpotBugsTask", e)
        }
    }

    private fun generateFile(files: FileCollection, file: File) {
        try {
            val lines =
                Iterable {
                    files.filter { obj: File -> obj.exists() }
                        .files.stream().map { obj: File -> obj.absolutePath }
                        .iterator()
                }
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE)
        } catch (e: IOException) {
            throw GradleException("Fail to generate the text file to list target .class files", e)
        }
    }

    protected fun buildJvmArguments(task: SpotBugsTask): List<String> {
        val args = task.jvmArgs.getOrElse(emptyList())
        log.debug("Arguments for JVM process are generated: {}", args)
        return args
    }

    private fun join(files: Collection<File>): String {
        return files.stream()
            .map { obj: File -> obj.absolutePath }
            .collect(Collectors.joining(File.pathSeparator))
    }
}
