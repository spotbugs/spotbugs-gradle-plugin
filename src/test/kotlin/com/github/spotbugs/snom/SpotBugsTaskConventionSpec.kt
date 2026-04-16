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

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testfixtures.ProjectBuilder

/**
 * Tests for [SpotBugsTask] property conventions that are established by [SpotBugsBasePlugin.apply]
 * via [SpotBugsTask.init].  Verifies that extension defaults propagate correctly to tasks and
 * that task-level overrides take precedence over extension conventions.
 */
class SpotBugsTaskConventionSpec :
    DescribeSpec({
        describe("SpotBugsTask conventions from SpotBugsExtension") {
            describe("ignoreFailures") {
                it("defaults to false via extension") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    task.getIgnoreFailures() shouldBe false
                }

                it("picks up extension override") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    val ext = project.extensions.getByType(SpotBugsExtension::class.java)
                    ext.useJavaToolchains.set(false)
                    ext.ignoreFailures.set(true)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    task.getIgnoreFailures() shouldBe true
                }

                it("can be overridden at task level") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    task.setIgnoreFailures(true)
                    task.getIgnoreFailures() shouldBe true
                }
            }

            describe("showStackTraces") {
                it("defaults to false via extension") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    task.showStackTraces.get() shouldBe false
                }

                it("picks up extension override") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    val ext = project.extensions.getByType(SpotBugsExtension::class.java)
                    ext.useJavaToolchains.set(false)
                    ext.showStackTraces.set(true)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    task.showStackTraces.get() shouldBe true
                }
            }

            describe("useAuxclasspathFile") {
                it("defaults to true via extension") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    task.useAuxclasspathFile.get() shouldBe true
                }

                it("picks up extension override") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    val ext = project.extensions.getByType(SpotBugsExtension::class.java)
                    ext.useJavaToolchains.set(false)
                    ext.useAuxclasspathFile.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    task.useAuxclasspathFile.get() shouldBe false
                }
            }

            describe("effort") {
                it("picks up Effort.MAX from extension") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    val ext = project.extensions.getByType(SpotBugsExtension::class.java)
                    ext.useJavaToolchains.set(false)
                    ext.effort.set(Effort.MAX)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    task.effort.get() shouldBe Effort.MAX
                }

                it("picks up Effort.LESS from extension") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    val ext = project.extensions.getByType(SpotBugsExtension::class.java)
                    ext.useJavaToolchains.set(false)
                    ext.effort.set(Effort.LESS)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    task.effort.get() shouldBe Effort.LESS
                }
            }

            describe("reportLevel (confidence)") {
                it("picks up Confidence.LOW from extension") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    val ext = project.extensions.getByType(SpotBugsExtension::class.java)
                    ext.useJavaToolchains.set(false)
                    ext.reportLevel.set(Confidence.LOW)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    task.reportLevel.get() shouldBe Confidence.LOW
                }

                it("picks up Confidence.HIGH from extension") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    val ext = project.extensions.getByType(SpotBugsExtension::class.java)
                    ext.useJavaToolchains.set(false)
                    ext.reportLevel.set(Confidence.HIGH)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    task.reportLevel.get() shouldBe Confidence.HIGH
                }
            }

            describe("projectName") {
                it("defaults to project-name plus task-name") {
                    val project = ProjectBuilder.builder().withName("my-lib").build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    task.projectName.get() shouldContain "my-lib"
                    task.projectName.get() shouldContain "spotbugsMain"
                }
            }

            describe("release") {
                it("defaults to the project version") {
                    val project = ProjectBuilder.builder().build()
                    project.version = "2.3.4"
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    task.release.get() shouldBe "2.3.4"
                }
            }

            describe("visitors / omitVisitors / chooseVisitors") {
                it("picks up visitors from extension") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    val ext = project.extensions.getByType(SpotBugsExtension::class.java)
                    ext.useJavaToolchains.set(false)
                    ext.visitors.set(listOf("FindSqlInjection"))
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    task.visitors.get() shouldBe listOf("FindSqlInjection")
                }

                it("picks up omitVisitors from extension") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    val ext = project.extensions.getByType(SpotBugsExtension::class.java)
                    ext.useJavaToolchains.set(false)
                    ext.omitVisitors.set(listOf("FindNonShortCircuit"))
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    task.omitVisitors.get() shouldBe listOf("FindNonShortCircuit")
                }

                it("picks up chooseVisitors from extension") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    val ext = project.extensions.getByType(SpotBugsExtension::class.java)
                    ext.useJavaToolchains.set(false)
                    ext.chooseVisitors.set(listOf("-FindNonShortCircuit", "+TestASM"))
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    task.chooseVisitors.get() shouldBe listOf("-FindNonShortCircuit", "+TestASM")
                }
            }

            describe("extraArgs / jvmArgs / maxHeapSize") {
                it("picks up extraArgs from extension") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    val ext = project.extensions.getByType(SpotBugsExtension::class.java)
                    ext.useJavaToolchains.set(false)
                    ext.extraArgs.set(listOf("-nested:false"))
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    task.extraArgs.get() shouldBe listOf("-nested:false")
                }

                it("picks up jvmArgs from extension") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    val ext = project.extensions.getByType(SpotBugsExtension::class.java)
                    ext.useJavaToolchains.set(false)
                    ext.jvmArgs.set(listOf("-Duser.language=ja"))
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    task.jvmArgs.get() shouldBe listOf("-Duser.language=ja")
                }

                it("picks up maxHeapSize from extension") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    val ext = project.extensions.getByType(SpotBugsExtension::class.java)
                    ext.useJavaToolchains.set(false)
                    ext.maxHeapSize.set("512m")
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    task.maxHeapSize.get() shouldBe "512m"
                }
            }

            describe("analyseClassFile") {
                it("is set to a file under the build directory") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    val analyseClassFile = task.analyseClassFile.get().asFile
                    analyseClassFile.absolutePath shouldContain project.layout.buildDirectory.get().asFile.absolutePath
                }
            }

            describe("auxclasspathFile") {
                it("is set to a file under the build/spotbugs/auxclasspath directory") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    val auxFile = task.auxclasspathFile.get().asFile
                    auxFile.absolutePath shouldContain "spotbugs"
                    auxFile.absolutePath shouldContain "auxclasspath"
                }
            }

            describe("classes property") {
                it("falls back to classDirs .class file tree when not explicitly set") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    // classDirs is empty, so classes should be an empty FileTree
                    task.classes shouldNotBe null
                    task.classes!!.isEmpty shouldBe true
                }

                it("returns the explicitly set classes when set") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    val fileCollection = project.files()
                    task.classes = fileCollection
                    task.classes shouldBe fileCollection
                }
            }

            describe("reportsDir") {
                it("defaults to a spotbugs subdirectory inside the reports base directory") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val ext = project.extensions.getByType(SpotBugsExtension::class.java)
                    ext.reportsDir.get().asFile.name shouldBe "spotbugs"
                }

                it("task reportsDir inherits from extension default") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    task.reportsDir.get().asFile.name shouldBe "spotbugs"
                }
            }

            describe("getRequiredReports") {
                it("returns only reports that are required") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()

                    task.reports.maybeCreate("xml").required.set(true)
                    task.reports.maybeCreate("html").required.set(false)
                    task.reports.maybeCreate("text").required.set(true)
                    task.reports.maybeCreate("sarif").required.set(false)

                    val requiredReports = task.getRequiredReports().toList()
                    requiredReports.map { it.name }.toSet() shouldBe setOf("xml", "text")
                }

                it("returns no reports when all reports are disabled") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()

                    task.reports.maybeCreate("xml").required.set(false)
                    task.reports.maybeCreate("html").required.set(false)

                    task.getRequiredReports().toList() shouldBe emptyList()
                }
            }

            describe("reports(Action)") {
                it("creates and returns the reports container") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()

                    var called = false
                    val result = task.reports { _ -> called = true }
                    called shouldBe true
                    result shouldNotBe null
                }
            }
        }
    })
