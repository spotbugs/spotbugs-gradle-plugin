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
        val plugins = task.getPluginJar()
        if (plugins.isNotEmpty()) {
            args.add("-pluginList")
            args.add(join(plugins))
        }
        args.add("-timestampNow")
        if (!task.getAuxClassPaths().isEmpty) {
            if (task.getUseAuxclasspathFile().get()) {
                args.add("-auxclasspathFromFile")
                val auxClasspathFile = createFileForAuxClasspath(task)
                log.debug("Using auxclasspath file: {}", auxClasspathFile)
                args.add(auxClasspathFile)
            } else {
                args.add("-auxclasspath")
                args.add(join(task.getAuxClassPaths().files))
            }
        }
        if (!task.getSourceDirs().isEmpty) {
            args.add("-sourcepath")
            args.add(task.getSourceDirs().asPath)
        }
        if (task.getShowProgress().getOrElse(Boolean.FALSE)) {
            args.add("-progress")
        }
        for (report: SpotBugsReport in task.getEnabledReports()) {
            val reportFile = report.outputLocation.asFile.get()
            val dir = reportFile.parentFile
            dir.mkdirs()
            args.add(report.toCommandLineOption() + "=" + reportFile.absolutePath)
        }
        if (task.getEffort().isPresent) {
            args.add("-effort:" + task.getEffort().get().name.lowercase(Locale.getDefault()))
        }
        if (task.getReportLevel().isPresent) {
            task.getReportLevel().get().toCommandLineOption().ifPresent { e: String ->
                args.add(
                    e,
                )
            }
        }
        if (task.getVisitors().isPresent && task.getVisitors().get().isNotEmpty()) {
            args.add("-visitors")
            args.add(task.getVisitors().get().stream().collect(Collectors.joining(",")))
        }
        if (task.getOmitVisitors().isPresent && task.getOmitVisitors().get().isNotEmpty()) {
            args.add("-omitVisitors")
            args.add(task.getOmitVisitors().get().stream().collect(Collectors.joining(",")))
        }
        if (task.getIncludeFilter().isPresent && task.getIncludeFilter().get() != null) {
            args.add("-include")
            args.add(task.getIncludeFilter().get().asFile.absolutePath)
        }
        if (task.getExcludeFilter().isPresent && task.getExcludeFilter().get() != null) {
            args.add("-exclude")
            args.add(task.getExcludeFilter().get().asFile.absolutePath)
        }
        if (task.getBaselineFile().isPresent && task.getBaselineFile().get() != null) {
            args.add("-excludeBugs")
            args.add(task.getBaselineFile().get().asFile.absolutePath)
        }
        if (task.getOnlyAnalyze().isPresent && task.getOnlyAnalyze().get().isNotEmpty()) {
            args.add("-onlyAnalyze")
            args.add(task.getOnlyAnalyze().get().stream().collect(Collectors.joining(",")))
        }
        args.add("-projectName")
        args.add(task.getProjectName().get())
        args.add("-release")
        args.add(task.getRelease().get())
        val file = task.getAnalyseClassFile().asFile.get()
        generateFile(task.getClasses(), task.getAnalyseClassFile().asFile.get())
        args.add("-analyzeFromFile")
        args.add(file.absolutePath)
        args.addAll(task.getExtraArgs().getOrElse(emptyList()))
        log.debug("Arguments for SpotBugs are generated: {}", args)
        return args
    }

    private fun createFileForAuxClasspath(task: SpotBugsTask): String {
        val auxClasspath = task.getAuxClassPaths().files.stream()
            .map { obj: File -> obj.absolutePath }
            .collect(Collectors.joining("\n"))
        try {
            val auxClasspathFile = task.getAuxclasspathFile()
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
        val args = task.getJvmArgs().getOrElse(emptyList())
        log.debug("Arguments for JVM process are generated: {}", args)
        return args
    }

    private fun join(files: Collection<File>): String {
        return files.stream()
            .map { obj: File -> obj.absolutePath }
            .collect(Collectors.joining(File.pathSeparator))
    }
}
