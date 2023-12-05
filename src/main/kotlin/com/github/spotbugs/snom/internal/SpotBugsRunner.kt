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

import com.github.spotbugs.snom.SpotBugsTask
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.Locale
import kotlin.String
import kotlin.collections.ArrayList
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.slf4j.LoggerFactory

abstract class SpotBugsRunner {
    private val log = LoggerFactory.getLogger(SpotBugsRunner::class.java)

    abstract fun run(task: SpotBugsTask)

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    protected fun buildArguments(task: SpotBugsTask): List<String> {
        val args: MutableList<String> = ArrayList()
        val plugins = task.pluginJarFiles
        if (!plugins.isEmpty) {
            args.add("-pluginList")
            args.add(join(plugins.files))
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
        if (task.showProgress.getOrElse(false)) {
            args.add("-progress")
        }
        task.getRequiredReports().forEach { report ->
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
            args.add(task.visitors.get().joinToString(","))
        }
        if (task.omitVisitors.isPresent && task.omitVisitors.get().isNotEmpty()) {
            args.add("-omitVisitors")
            args.add(task.omitVisitors.get().joinToString(","))
        }
        if (task.includeFilter.isPresent) {
            args.add("-include")
            args.add(task.includeFilter.get().asFile.absolutePath)
        }
        if (task.excludeFilter.isPresent) {
            args.add("-exclude")
            args.add(task.excludeFilter.get().asFile.absolutePath)
        }
        if (task.baselineFile.isPresent) {
            args.add("-excludeBugs")
            args.add(task.baselineFile.get().asFile.absolutePath)
        }
        if (task.onlyAnalyze.isPresent) {
            args.add("-onlyAnalyze")
            args.add(task.onlyAnalyze.get().joinToString(","))
        }
        args.add("-projectName")
        args.add(task.projectName.get())
        args.add("-release")
        args.add(task.release.get())
        val file = task.analyseClassFile.asFile.get()
        task.classes?.let { generateFile(it, file) }
        args.add("-analyzeFromFile")
        args.add(file.absolutePath)
        args.addAll(task.extraArgs.getOrElse(emptyList()))
        log.debug("Arguments for SpotBugs are generated: {}", args)
        return args
    }

    private fun createFileForAuxClasspath(task: SpotBugsTask): String {
        val auxClasspath =
            task.auxClassPaths.files.asSequence()
                .map { obj: File -> obj.absolutePath }
                .joinToString("\n")
        val auxClasspathFile =
            task.auxclasspathFile.map {
                it.asFile.toPath()
            }.get()
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
        } catch (e: IOException) {
            throw GradleException(
                "Could not create auxiliary classpath file for SpotBugsTask at " +
                    auxClasspathFile.normalize().toString(),
                e,
            )
        }
    }

    private fun generateFile(
        files: FileCollection,
        file: File,
    ) {
        try {
            file.bufferedWriter().use { writer ->
                files.filter(File::exists)
                    .forEach {
                        writer.write(it.absolutePath)
                        writer.newLine()
                    }
            }
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
        return files.asSequence()
            .map { obj: File -> obj.absolutePath }
            .joinToString(File.pathSeparator)
    }
}
