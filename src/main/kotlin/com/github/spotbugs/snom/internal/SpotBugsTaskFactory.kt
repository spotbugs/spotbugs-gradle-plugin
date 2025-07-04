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

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.github.spotbugs.snom.SpotBugsTask
import org.gradle.api.Action
import org.gradle.api.DomainObjectSet
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.slf4j.LoggerFactory

internal class SpotBugsTaskFactory {
    private val log = LoggerFactory.getLogger(SpotBugsTaskFactory::class.java)

    fun generate(project: Project) {
        generateForJava(project)
        generateForAndroid(project)
    }

    private fun generateForJava(project: Project) {
        project.plugins.withType(JavaBasePlugin::class.java).configureEach {
            project.extensions.getByType(JavaPluginExtension::class.java).sourceSets.configureEach { sourceSet ->
                val name = sourceSet.getTaskName("spotbugs", null)
                log.debug("Creating SpotBugsTask for {}", sourceSet)
                project.tasks.register(name, SpotBugsTask::class.java) {
                    it.sourceDirs.setFrom(sourceSet.allSource.sourceDirectories)
                    it.classDirs.setFrom(sourceSet.output)
                    it.auxClassPaths.setFrom(sourceSet.compileClasspath)
                    it.description = "Run SpotBugs analysis for the source set '${sourceSet.name}'"
                }
            }
        }
    }

    private fun generateForAndroid(project: Project) {
        val action = Action<Plugin<*>> {
            val variants: DomainObjectSet<out BaseVariant> =
                when (val baseExtension = project.extensions.getByType(BaseExtension::class.java)) {
                    is AppExtension -> baseExtension.applicationVariants
                    is LibraryExtension -> baseExtension.libraryVariants
                    else -> throw GradleException("Unrecognized Android extension $baseExtension")
                }
            variants.configureEach { variant: BaseVariant ->
                val spotbugsTaskName = toLowerCamelCase("spotbugs", variant.name)
                log.debug("Creating SpotBugsTask for {}", variant.name)
                project.tasks.register(spotbugsTaskName, SpotBugsTask::class.java) {
                    val javaCompile = variant.javaCompileProvider
                    it.sourceDirs.setFrom(javaCompile.map(JavaCompile::getSource))
                    it.classDirs.setFrom(javaCompile.map(JavaCompile::getDestinationDirectory))
                    it.auxClassPaths.setFrom(javaCompile.map(JavaCompile::getClasspath))
                    it.dependsOn(javaCompile)
                }
            }
        }
        project.plugins.withId("com.android.application", action)
        project.plugins.withId("com.android.library", action)
    }

    companion object {
        fun toLowerCamelCase(head: String, tail: String?): String {
            if (tail.isNullOrEmpty()) {
                return head
            }
            return buildString(head.length + tail.length) {
                append(head)
                append(tail[0].uppercaseChar())
                append(tail.substring(1))
            }
        }
    }
}
