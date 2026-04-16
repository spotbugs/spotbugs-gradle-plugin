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

import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsBasePlugin
import com.github.spotbugs.snom.SpotBugsExtension
import com.github.spotbugs.snom.SpotBugsTask
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Files
import org.gradle.testfixtures.ProjectBuilder

class SpotBugsRunnerSpec :
    DescribeSpec({
        describe("SpotBugsRunner") {
            it("produces only required arguments when no optional task properties are set") {
                val project = ProjectBuilder.builder().build()
                project.plugins.apply(SpotBugsBasePlugin::class.java)
                project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                val runner = TestSpotBugsRunner()

                task.projectName.set("my-project")
                task.release.set("unspecified")
                task.classes = project.files()
                project.layout.buildDirectory.get().asFile.mkdirs()

                val args = runner.buildArgumentsFor(task)

                args shouldContain "-timestampNow"
                args shouldContain "-projectName"
                args shouldContain "my-project"
                args shouldContain "-release"
                args shouldContain "unspecified"
                args shouldContain "-analyzeFromFile"
                args shouldNotContain "-pluginList"
                args shouldNotContain "-auxclasspath"
                args shouldNotContain "-auxclasspathFromFile"
                args shouldNotContain "-sourcepath"
                args shouldNotContain "-progress"
                args shouldNotContain "-effort:default"
                args shouldNotContain "-visitors"
                args shouldNotContain "-omitVisitors"
                args shouldNotContain "-chooseVisitors"
                args shouldNotContain "-include"
                args shouldNotContain "-exclude"
                args shouldNotContain "-excludeBugs"
                args shouldNotContain "-onlyAnalyze"
            }

            it("does not add a confidence flag for Confidence.DEFAULT") {
                val project = ProjectBuilder.builder().build()
                project.plugins.apply(SpotBugsBasePlugin::class.java)
                project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                val runner = TestSpotBugsRunner()

                task.reportLevel.set(Confidence.DEFAULT)
                task.projectName.set("demo")
                task.release.set("1.0")
                task.classes = project.files()
                project.layout.buildDirectory.get().asFile.mkdirs()

                val args = runner.buildArgumentsFor(task)

                args shouldNotContain "-low"
                args shouldNotContain "-medium"
                args shouldNotContain "-high"
            }

            it("adds -low for Confidence.LOW and -medium for Confidence.MEDIUM") {
                val project = ProjectBuilder.builder().build()
                project.plugins.apply(SpotBugsBasePlugin::class.java)
                project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                val runner = TestSpotBugsRunner()
                project.layout.buildDirectory.get().asFile.mkdirs()

                val taskLow = project.tasks.register("spotbugsLow", SpotBugsTask::class.java).get()
                taskLow.reportLevel.set(Confidence.LOW)
                taskLow.projectName.set("demo")
                taskLow.release.set("1.0")
                taskLow.classes = project.files()
                runner.buildArgumentsFor(taskLow) shouldContain "-low"

                val taskMedium = project.tasks.register("spotbugsMedium", SpotBugsTask::class.java).get()
                taskMedium.reportLevel.set(Confidence.MEDIUM)
                taskMedium.projectName.set("demo")
                taskMedium.release.set("1.0")
                taskMedium.classes = project.files()
                runner.buildArgumentsFor(taskMedium) shouldContain "-medium"

                val taskHigh = project.tasks.register("spotbugsHigh", SpotBugsTask::class.java).get()
                taskHigh.reportLevel.set(Confidence.HIGH)
                taskHigh.projectName.set("demo")
                taskHigh.release.set("1.0")
                taskHigh.classes = project.files()
                runner.buildArgumentsFor(taskHigh) shouldContain "-high"
            }

            it("does not add -effort flag when effort is not set") {
                val project = ProjectBuilder.builder().build()
                project.plugins.apply(SpotBugsBasePlugin::class.java)
                project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                val runner = TestSpotBugsRunner()

                task.projectName.set("demo")
                task.release.set("1.0")
                task.classes = project.files()
                // effort is not set
                project.layout.buildDirectory.get().asFile.mkdirs()

                val args = runner.buildArgumentsFor(task)
                args.none { it.startsWith("-effort:") } shouldBe true
            }

            it("adds the correct effort flag for each Effort value") {
                val project = ProjectBuilder.builder().build()
                project.plugins.apply(SpotBugsBasePlugin::class.java)
                project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                val runner = TestSpotBugsRunner()
                project.layout.buildDirectory.get().asFile.mkdirs()

                for (effort in Effort.entries) {
                    val task = project.tasks.register("spotbugs${effort.name}", SpotBugsTask::class.java).get()
                    task.effort.set(effort)
                    task.projectName.set("demo")
                    task.release.set("1.0")
                    task.classes = project.files()
                    runner.buildArgumentsFor(task) shouldContain "-effort:${effort.name.lowercase()}"
                }
            }

            it("does not add -visitors/-omitVisitors/-chooseVisitors when the lists are empty") {
                val project = ProjectBuilder.builder().build()
                project.plugins.apply(SpotBugsBasePlugin::class.java)
                project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                val runner = TestSpotBugsRunner()

                task.visitors.set(emptyList())
                task.omitVisitors.set(emptyList())
                task.chooseVisitors.set(emptyList())
                task.projectName.set("demo")
                task.release.set("1.0")
                task.classes = project.files()
                project.layout.buildDirectory.get().asFile.mkdirs()

                val args = runner.buildArgumentsFor(task)

                args shouldNotContain "-visitors"
                args shouldNotContain "-omitVisitors"
                args shouldNotContain "-chooseVisitors"
            }

            it("does not add -onlyAnalyze when the list is empty") {
                val project = ProjectBuilder.builder().build()
                project.plugins.apply(SpotBugsBasePlugin::class.java)
                project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                val runner = TestSpotBugsRunner()

                task.onlyAnalyze.set(emptyList())
                task.projectName.set("demo")
                task.release.set("1.0")
                task.classes = project.files()
                project.layout.buildDirectory.get().asFile.mkdirs()

                val args = runner.buildArgumentsFor(task)
                args shouldNotContain "-onlyAnalyze"
            }

            it("returns empty jvmArgs when not configured") {
                val project = ProjectBuilder.builder().build()
                project.plugins.apply(SpotBugsBasePlugin::class.java)
                project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                val runner = TestSpotBugsRunner()

                task.projectName.set("demo")
                task.release.set("1.0")
                task.classes = project.files()

                runner.buildJvmArgumentsFor(task) shouldBe emptyList()
            }

            it("builds arguments for configured task options and writes helper files") {
                val project = ProjectBuilder.builder().build()
                project.plugins.apply(SpotBugsBasePlugin::class.java)
                project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                val runner = TestSpotBugsRunner()

                val workDir = project.layout.buildDirectory.dir("runner-spec").get().asFile
                val pluginJar = File(workDir, "plugin.jar").apply {
                    parentFile.mkdirs()
                    writeText("plugin")
                }
                val auxJar = File(workDir, "aux.jar").apply { writeText("aux") }
                val sourceDir = File(workDir, "src").apply { mkdirs() }
                val includeFilter = File(workDir, "include.xml").apply { writeText("<FindBugsFilter/>") }
                val excludeFilter = File(workDir, "exclude.xml").apply { writeText("<FindBugsFilter/>") }
                val baselineFile = File(workDir, "baseline.xml").apply { writeText("<BugCollection/>") }
                val existingClass = File(workDir, "Example.class").apply { writeText("bytecode") }
                val missingClass = File(workDir, "Missing.class")
                val reportFile = File(workDir, "reports/spotbugs.xml")

                task.pluginJarFiles.setFrom(pluginJar)
                task.auxClassPaths.setFrom(auxJar)
                task.useAuxclasspathFile.set(true)
                task.sourceDirs.setFrom(sourceDir)
                task.showProgress.set(true)
                task.reports.maybeCreate("xml").outputLocation.set(reportFile)
                task.effort.set(Effort.MAX)
                task.reportLevel.set(Confidence.LOW)
                task.visitors.set(listOf("FindSqlInjection", "SwitchFallthrough"))
                task.omitVisitors.set(listOf("FindNonShortCircuit"))
                task.chooseVisitors.set(listOf("-FindNonShortCircuit", "+TestASM"))
                task.includeFilter.set(includeFilter)
                task.excludeFilter.set(excludeFilter)
                task.baselineFile.set(baselineFile)
                task.onlyAnalyze.set(listOf("com.example.Foo", "com.example.bar.*"))
                task.projectName.set("demo-project")
                task.release.set("1.2.3")
                task.extraArgs.set(listOf("-nested:false"))
                task.jvmArgs.set(listOf("-Duser.language=ja"))
                task.classes = project.files(existingClass, missingClass)

                val args = runner.buildArgumentsFor(task)
                val jvmArgs = runner.buildJvmArgumentsFor(task)
                val auxClasspathFile = task.auxclasspathFile.get().asFile
                val analyseClassFile = task.analyseClassFile.get().asFile

                args shouldContainAll listOf(
                    "-pluginList",
                    pluginJar.absolutePath,
                    "-timestampNow",
                    "-auxclasspathFromFile",
                    auxClasspathFile.absolutePath,
                    "-sourcepath",
                    sourceDir.absolutePath,
                    "-progress",
                    "-effort:max",
                    "-low",
                    "-visitors",
                    "FindSqlInjection,SwitchFallthrough",
                    "-omitVisitors",
                    "FindNonShortCircuit",
                    "-chooseVisitors",
                    "-FindNonShortCircuit,+TestASM",
                    "-include",
                    includeFilter.absolutePath,
                    "-exclude",
                    excludeFilter.absolutePath,
                    "-excludeBugs",
                    baselineFile.absolutePath,
                    "-onlyAnalyze",
                    "com.example.Foo,com.example.bar.*",
                    "-projectName",
                    "demo-project",
                    "-release",
                    "1.2.3",
                    "-analyzeFromFile",
                    analyseClassFile.absolutePath,
                    "-nested:false",
                )
                args shouldContain "-xml:withMessages=${reportFile.absolutePath}"
                jvmArgs shouldBe listOf("-Duser.language=ja")
                auxClasspathFile.exists() shouldBe true
                auxClasspathFile.readText() shouldBe auxJar.absolutePath
                analyseClassFile.exists() shouldBe true
                analyseClassFile.readLines() shouldBe listOf(existingClass.absolutePath)
            }

            it("uses -auxclasspath when useAuxclasspathFile is false") {
                val project = ProjectBuilder.builder().build()
                project.plugins.apply(SpotBugsBasePlugin::class.java)
                project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                val runner = TestSpotBugsRunner()

                val aux1 = Files.createTempFile("aux1", ".jar").toFile()
                val aux2 = Files.createTempFile("aux2", ".jar").toFile()
                task.auxClassPaths.setFrom(aux1, aux2)
                task.useAuxclasspathFile.set(false)
                task.projectName.set("demo-project")
                task.release.set("1.0")
                task.classes = project.files()
                project.layout.buildDirectory.get().asFile.mkdirs()

                val args = runner.buildArgumentsFor(task)

                args shouldContain "-auxclasspath"
                args shouldContain "${aux1.absolutePath}${File.pathSeparator}${aux2.absolutePath}"
                args shouldNotContain "-auxclasspathFromFile"
            }
        }
    })

private class TestSpotBugsRunner : SpotBugsRunner() {
    override fun run(task: SpotBugsTask) = Unit

    fun buildArgumentsFor(task: SpotBugsTask): List<String> = buildArguments(task)

    fun buildJvmArgumentsFor(task: SpotBugsTask): List<String> = buildJvmArguments(task)
}
