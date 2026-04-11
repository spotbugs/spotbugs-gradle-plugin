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

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.github.spotbugs.snom.SpotBugsTask
import java.util.Locale
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
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
        project.plugins.withId("com.android.application") {
            val components =
                project.extensions.findByType(ApplicationAndroidComponentsExtension::class.java)
            if (components == null) {
                log.warn(
                    "ApplicationAndroidComponentsExtension not found; " +
                        "SpotBugs tasks will not be generated for Android application variants",
                )
                return@withId
            }
            components.onVariants { variant ->
                registerSpotBugsTaskForAndroid(project, variant.name)
            }
        }
        project.plugins.withId("com.android.library") {
            val components =
                project.extensions.findByType(LibraryAndroidComponentsExtension::class.java)
            if (components == null) {
                log.warn(
                    "LibraryAndroidComponentsExtension not found; " +
                        "SpotBugs tasks will not be generated for Android library variants",
                )
                return@withId
            }
            components.onVariants { variant ->
                registerSpotBugsTaskForAndroid(project, variant.name)
            }
        }
    }

    private fun registerSpotBugsTaskForAndroid(project: Project, variantName: String) {
        val spotbugsTaskName = toLowerCamelCase("spotbugs", variantName)
        log.debug("Creating SpotBugsTask for {}", variantName)
        // AGP uses the naming convention compile${VariantName}JavaWithJavac for the Java compile task
        val capitalizedVariantName =
            variantName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        val javaCompileTaskName = "compile${capitalizedVariantName}JavaWithJavac"
        project.tasks.register(spotbugsTaskName, SpotBugsTask::class.java) { task ->
            val javaCompile =
                try {
                    project.tasks.named(javaCompileTaskName, JavaCompile::class.java)
                } catch (e: UnknownDomainObjectException) {
                    log.warn(
                        "JavaCompile task '{}' not found; " +
                            "SpotBugs task '{}' will have no sources or classes configured",
                        javaCompileTaskName,
                        spotbugsTaskName,
                        e,
                    )
                    return@register
                }
            task.sourceDirs.setFrom(javaCompile.map(JavaCompile::getSource))
            task.classDirs.setFrom(javaCompile.map(JavaCompile::getDestinationDirectory))
            task.auxClassPaths.setFrom(javaCompile.map(JavaCompile::getClasspath))
            task.dependsOn(javaCompile)
        }
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
