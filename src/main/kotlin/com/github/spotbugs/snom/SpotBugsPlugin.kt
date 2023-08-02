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

import com.github.spotbugs.snom.internal.SpotBugsTaskFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.AppliedPlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.slf4j.LoggerFactory

class SpotBugsPlugin : Plugin<Project> {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun apply(project: Project) {
        project.pluginManager.apply(SpotBugsBasePlugin::class.java)
        project
            .pluginManager
            .withPlugin(
                "java-base"
            ) { javaBase: AppliedPlugin ->
                log.debug(
                    "The javaBase plugin has been applied, so making the check task depending on all of SpotBugsTask"
                )
                project
                    .tasks
                    .named(JavaBasePlugin.CHECK_TASK_NAME)
                    .configure { task: Task ->
                        task.dependsOn(
                            project.tasks.withType(
                                SpotBugsTask::class.java
                            )
                        )
                    }
            }
        createTasks(project)
    }

    private fun createTasks(project: Project) {
        SpotBugsTaskFactory().generate(project)
    }

    companion object {
        const val CONFIG_NAME = "spotbugs"

        /**
         * The configuration contains SpotBugs plugin jar files only
         *
         * @see [GitHub issue](https://github.com/spotbugs/spotbugs-gradle-plugin/issues/910)
         */
        const val PLUGINS_CONFIG_NAME = "spotbugsPlugins"
        const val SLF4J_CONFIG_NAME = "spotbugsSlf4j"
        const val EXTENSION_NAME = "spotbugs"
    }
}