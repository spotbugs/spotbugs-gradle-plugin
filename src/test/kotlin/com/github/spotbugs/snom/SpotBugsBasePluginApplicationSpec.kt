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
import org.gradle.testfixtures.ProjectBuilder

class SpotBugsBasePluginApplicationSpec :
    DescribeSpec({
        describe("SpotBugsBasePlugin.apply") {
            it("creates the spotbugs extension") {
                val project = ProjectBuilder.builder().build()
                project.plugins.apply(SpotBugsBasePlugin::class.java)
                project.extensions.findByType(SpotBugsExtension::class.java) shouldNotBe null
            }

            it("creates the '${SpotBugsPlugin.CONFIG_NAME}' configuration") {
                val project = ProjectBuilder.builder().build()
                project.plugins.apply(SpotBugsBasePlugin::class.java)
                project.configurations.findByName(SpotBugsPlugin.CONFIG_NAME) shouldNotBe null
            }

            it("creates the '${SpotBugsPlugin.PLUGINS_CONFIG_NAME}' configuration") {
                val project = ProjectBuilder.builder().build()
                project.plugins.apply(SpotBugsBasePlugin::class.java)
                project.configurations.findByName(SpotBugsPlugin.PLUGINS_CONFIG_NAME) shouldNotBe null
            }

            it("creates the '${SpotBugsPlugin.SLF4J_CONFIG_NAME}' configuration") {
                val project = ProjectBuilder.builder().build()
                project.plugins.apply(SpotBugsBasePlugin::class.java)
                project.configurations.findByName(SpotBugsPlugin.SLF4J_CONFIG_NAME) shouldNotBe null
            }

            it("sets ignoreFailures default to false") {
                val project = ProjectBuilder.builder().build()
                project.plugins.apply(SpotBugsBasePlugin::class.java)
                val extension = project.extensions.getByType(SpotBugsExtension::class.java)
                extension.ignoreFailures.get() shouldBe false
            }

            it("sets showStackTraces default to false") {
                val project = ProjectBuilder.builder().build()
                project.plugins.apply(SpotBugsBasePlugin::class.java)
                val extension = project.extensions.getByType(SpotBugsExtension::class.java)
                extension.showStackTraces.get() shouldBe false
            }

            it("sets useAuxclasspathFile default to true") {
                val project = ProjectBuilder.builder().build()
                project.plugins.apply(SpotBugsBasePlugin::class.java)
                val extension = project.extensions.getByType(SpotBugsExtension::class.java)
                extension.useAuxclasspathFile.get() shouldBe true
            }

            it("sets useJavaToolchains default to true") {
                val project = ProjectBuilder.builder().build()
                project.plugins.apply(SpotBugsBasePlugin::class.java)
                val extension = project.extensions.getByType(SpotBugsExtension::class.java)
                extension.useJavaToolchains.get() shouldBe true
            }

            it("sets runOnCheck default to true") {
                val project = ProjectBuilder.builder().build()
                project.plugins.apply(SpotBugsBasePlugin::class.java)
                val extension = project.extensions.getByType(SpotBugsExtension::class.java)
                extension.runOnCheck.get() shouldBe true
            }

            it("sets the tool version convention from the properties file") {
                val project = ProjectBuilder.builder().build()
                project.plugins.apply(SpotBugsBasePlugin::class.java)
                val extension = project.extensions.getByType(SpotBugsExtension::class.java)
                extension.toolVersion.get() shouldNotBe null
            }

            it("sets the project name convention to the project name") {
                val project = ProjectBuilder.builder().withName("my-project").build()
                project.plugins.apply(SpotBugsBasePlugin::class.java)
                val extension = project.extensions.getByType(SpotBugsExtension::class.java)
                extension.projectName.get() shouldBe "my-project"
            }

            it("can be applied to a project with the java plugin") {
                val project = ProjectBuilder.builder().build()
                project.plugins.apply("java")
                project.plugins.apply(SpotBugsBasePlugin::class.java)
                project.extensions.findByType(SpotBugsExtension::class.java) shouldNotBe null
            }
        }
    })
