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
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.util.GradleVersion
import org.slf4j.LoggerFactory

class SpotBugsTaskFactory {
    private val log = LoggerFactory.getLogger(SpotBugsTaskFactory::class.java)
    fun generate(project: Project) {
        generateForJava(project)
        generateForAndroid(project)
    }

    private fun getSourceSetContainer(project: Project): SourceSetContainer {
        return if (GradleVersion.current() < GradleVersion.version("7.1")) {
            project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets
        } else {
            project.extensions.getByType(JavaPluginExtension::class.java).sourceSets
        }
    }

    private fun generateForJava(project: Project) {
        project
            .plugins
            .withType(JavaBasePlugin::class.java)
            .configureEach {
                getSourceSetContainer(project)
                    .all { sourceSet: SourceSet ->
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
        val action: Action<in Plugin<*>?> =
            Action {
                val baseExtension = project.extensions.getByType(
                    BaseExtension::class.java,
                )
                val variants: DomainObjectSet<out BaseVariant> = when (baseExtension) {
                    is AppExtension -> baseExtension.applicationVariants
                    is LibraryExtension -> baseExtension.libraryVariants
                    else -> throw GradleException("Unrecognized Android extension $baseExtension")
                }
                variants.all { variant: BaseVariant ->
                    val spotbugsTaskName =
                        toLowerCamelCase(
                            "spotbugs",
                            variant.name,
                        )
                    log.debug("Creating SpotBugsTask for {}", variant.name)
                    project
                        .tasks
                        .register(
                            spotbugsTaskName,
                            SpotBugsTask::class.java,
                            Action { spotbugsTask: SpotBugsTask ->
                                val javaCompile =
                                    variant.javaCompileProvider.get()
                                spotbugsTask.sourceDirs.setFrom(javaCompile.source)
                                spotbugsTask.classDirs.setFrom(javaCompile.destinationDirectory)
                                spotbugsTask.auxClassPaths.setFrom(javaCompile.classpath)
                                spotbugsTask.dependsOn(javaCompile)
                            },
                        )
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
