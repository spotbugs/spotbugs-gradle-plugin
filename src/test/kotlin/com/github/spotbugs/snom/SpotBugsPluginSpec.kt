package com.github.spotbugs.snom

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.util.GradleVersion

class SpotBugsPluginSpec :
    DescribeSpec({
        describe("SpotBugsPlugin") {
            it("loads the properties file from the packaged artifact") {
                SpotBugsBasePlugin().loadProperties().apply {
                    getProperty("spotbugs-version") shouldNotBe null
                    getProperty("slf4j-version") shouldNotBe null
                }
            }

            it("does not support Gradle 7.0") {
                shouldThrow<IllegalArgumentException> {
                    SpotBugsBasePlugin().verifyGradleVersion(GradleVersion.version("7.0"))
                }
            }

            it("supports Gradle 7.1") {
                SpotBugsBasePlugin().verifyGradleVersion(GradleVersion.version("7.1"))
            }

            it("rejects Gradle versions older than 7.1") {
                shouldThrow<IllegalArgumentException> {
                    SpotBugsBasePlugin().verifyGradleVersion(GradleVersion.version("6.9"))
                }
            }

            it("rejects Gradle 7.0") {
                shouldThrow<IllegalArgumentException> {
                    SpotBugsBasePlugin().verifyGradleVersion(GradleVersion.version("7.0"))
                }
            }

            it("supports Gradle 8.x") {
                SpotBugsBasePlugin().verifyGradleVersion(GradleVersion.version("8.0"))
            }

            it("does not depend on Gradle internal API") {
                val implementation = ClassFileImporter()
                    .importPackages("com.github.spotbugs.snom", "com.github.spotbugs.snom.internal")
                val rule: ArchRule = ArchRuleDefinition.noClasses().should()
                    .dependOnClassesThat().resideInAPackage("org.gradle..internal..")
                rule.check(implementation)
            }

            describe("SpotBugsPlugin.apply() with Java plugin") {
                it("creates spotbugsMain task when Java plugin is applied") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsPlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    project.plugins.apply("java")

                    val taskNames = project.tasks.withType(SpotBugsTask::class.java).names
                    taskNames.contains("spotbugsMain") shouldBe true
                }

                it("creates spotbugsTest task when Java plugin is applied") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsPlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    project.plugins.apply("java")

                    val taskNames = project.tasks.withType(SpotBugsTask::class.java).names
                    taskNames.contains("spotbugsTest") shouldBe true
                }

                it("spotbugsMain has the correct description") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsPlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    project.plugins.apply("java")

                    val task = project.tasks.withType(SpotBugsTask::class.java)
                        .first { it.name == "spotbugsMain" }
                    task.description shouldBe "Run SpotBugs analysis for the source set 'main'"
                }

                it("does not create SpotBugs tasks when Java plugin is not applied") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsPlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    // Java plugin NOT applied

                    project.tasks.withType(SpotBugsTask::class.java).names.isEmpty() shouldBe true
                }

                it("creates the spotbugs extension when applied") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsPlugin::class.java)
                    project.extensions.findByType(SpotBugsExtension::class.java) shouldNotBe null
                }

                it("spotbugsMain sourceDirs are configured from the main source set") {
                    val project = ProjectBuilder.builder().build()
                    project.plugins.apply(SpotBugsPlugin::class.java)
                    project.extensions.getByType(SpotBugsExtension::class.java).useJavaToolchains.set(false)
                    project.plugins.apply("java")

                    val task = project.tasks.withType(SpotBugsTask::class.java)
                        .first { it.name == "spotbugsMain" }
                    task.sourceDirs.isEmpty shouldBe false
                }
            }
        }
    })
