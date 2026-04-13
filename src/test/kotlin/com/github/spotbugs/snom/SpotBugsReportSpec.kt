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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.api.resources.TextResource
import org.gradle.testfixtures.ProjectBuilder

class SpotBugsReportSpec :
    DescribeSpec({
        describe("SpotBugsReport concrete implementations") {
            describe("SpotBugsXmlReport") {
                it("has '-xml:withMessages' as commandLineOption") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    val report = task.reports.maybeCreate("xml")
                    report.commandLineOption shouldBe "-xml:withMessages"
                }

                it("includes task path in displayName") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    val report = task.reports.maybeCreate("xml")
                    report.getDisplayName() shouldContain "spotbugsMain"
                }

                it("includes 'XML' in displayName") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    val report = task.reports.maybeCreate("xml")
                    report.getDisplayName() shouldContain "XML"
                }
            }

            describe("SpotBugsTextReport") {
                it("has '-sortByClass' as commandLineOption") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    val report = task.reports.maybeCreate("text")
                    report.commandLineOption shouldBe "-sortByClass"
                }

                it("includes task path in displayName") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    val report = task.reports.maybeCreate("text")
                    report.getDisplayName() shouldContain "spotbugsMain"
                }

                it("includes 'Text' in displayName") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    val report = task.reports.maybeCreate("text")
                    report.getDisplayName() shouldContain "Text"
                }
            }

            describe("SpotBugsSarifReport") {
                it("has '-sarif' as commandLineOption") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    val report = task.reports.maybeCreate("sarif")
                    report.commandLineOption shouldBe "-sarif"
                }
            }

            describe("SpotBugsHtmlReport") {
                it("has '-html' as commandLineOption when no stylesheet is set") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    val report = task.reports.maybeCreate("html")
                    report.commandLineOption shouldBe "-html"
                }

                it("returns null for getStylesheet when no stylesheet is set") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    val report = task.reports.maybeCreate("html")
                    report.getStylesheet() shouldBe null
                }

                it("throws UnsupportedOperationException when setStylesheet(TextResource) is called on non-html") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    val report = task.reports.maybeCreate("xml")
                    shouldThrow<UnsupportedOperationException> {
                        report.setStylesheet(null as TextResource?)
                    }
                }

                it("throws UnsupportedOperationException when setStylesheet(String) is called on non-html reports") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    val report = task.reports.maybeCreate("xml")
                    shouldThrow<UnsupportedOperationException> {
                        report.setStylesheet(null as String?)
                    }
                }
            }

            describe("SpotBugsReport base class") {
                it("configure(Action) applies the action and returns the report") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    val report = task.reports.maybeCreate("xml")
                    var actionApplied = false
                    val result = report.configure { _ -> actionApplied = true }
                    actionApplied shouldBe true
                    result shouldBe report
                }

                it("throws InvalidUserDataException for unknown report types") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsBasePlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    val task = project.tasks.register("spotbugsMain", SpotBugsTask::class.java).get()
                    shouldThrow<org.gradle.api.InvalidUserDataException> {
                        task.reports.maybeCreate("unknown")
                    }
                }
            }
        }
    })

