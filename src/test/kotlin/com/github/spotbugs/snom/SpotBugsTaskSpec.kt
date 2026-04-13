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
import java.io.File
import org.gradle.api.reporting.Report
import org.gradle.testfixtures.ProjectBuilder

class SpotBugsTaskSpec :
    DescribeSpec({
        describe("SpotBugsTask") {
            describe("getBaseName") {
                it("returns 'spotbugs' when the task name is exactly 'spotbugs'") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugs", SpotBugsTask::class.java).get()
                    task.getBaseName() shouldBe "spotbugs"
                }

                it("returns 'main' when the task name is 'spotbugsMain'") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    task.getBaseName() shouldBe "main"
                }

                it("returns 'test' when the task name is 'spotbugsTest'") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsTest", SpotBugsTask::class.java).get()
                    task.getBaseName() shouldBe "test"
                }

                it("returns 'integrationTest' when the task name is 'spotbugsIntegrationTest'") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsIntegrationTest", SpotBugsTask::class.java).get()
                    task.getBaseName() shouldBe "integrationTest"
                }
            }

            describe("reports") {
                it("has an empty reports container initially") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    task.reports.isEmpty() shouldBe true
                }

                it("creates an html report on demand") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    val report = task.reports.maybeCreate("html")
                    report.getOutputType() shouldBe Report.OutputType.FILE
                    report.getRequired().get() shouldBe true
                }

                it("creates an xml report on demand") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    val report = task.reports.maybeCreate("xml")
                    report.getOutputType() shouldBe Report.OutputType.FILE
                }

                it("creates a text report on demand") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    val report = task.reports.maybeCreate("text")
                    report.getOutputType() shouldBe Report.OutputType.FILE
                }

                it("creates a sarif report on demand") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    val report = task.reports.maybeCreate("sarif")
                    report.getOutputType() shouldBe Report.OutputType.FILE
                }
            }

            describe("setDestination (deprecated)") {
                it("sets destination via deprecated File setter") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    val report = task.reports.maybeCreate("xml")
                    val target = File(project.layout.buildDirectory.get().asFile, "spotbugs/myreport.xml")
                    @Suppress("DEPRECATION")
                    report.setDestination(target)
                    @Suppress("DEPRECATION")
                    report.getDestination() shouldBe target
                }
            }

            describe("isEnabled (deprecated)") {
                it("reads and writes isEnabled flag") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    val report = task.reports.maybeCreate("xml")
                    report.getRequired().get() shouldBe true
                    @Suppress("DEPRECATION")
                    report.isEnabled = false
                    @Suppress("DEPRECATION")
                    report.isEnabled shouldBe false
                }
            }
        }
    })
