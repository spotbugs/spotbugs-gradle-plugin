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

import com.github.spotbugs.snom.SpotBugsPlugin
import com.github.spotbugs.snom.SpotBugsReport
import com.github.spotbugs.snom.SpotBugsTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.resources.TextResource
import org.gradle.api.resources.TextResourceFactory
import javax.inject.Inject

abstract class SpotBugsHtmlReport @Inject constructor(objects: ObjectFactory, task: SpotBugsTask) :
    SpotBugsReport(objects, task) {
    private val stylesheet: Property<TextResource>

    init {
        // the default reportsDir is "$buildDir/reports/spotbugs/${baseName}.html"
        outputLocation.convention(task.reportsDir.file(task.getBaseName() + ".html"))
        stylesheet = task.project.objects.property(TextResource::class.java)
    }

    override fun toCommandLineOption(): String {
        return stylesheet.map {
            "-html:" + it.asFile().absolutePath
        }.getOrElse("-html")
    }

    override fun getStylesheet(): TextResource? =
        stylesheet.orNull

    private fun resolve(path: String, configuration: Configuration, textResourceFactory: TextResourceFactory): TextResource {
        val spotbugsJar = configuration.files { dependency: Dependency -> dependency.group == "com.github.spotbugs" && dependency.name == "spotbugs" }
            .find { it.isFile }
        return if (spotbugsJar != null) {
            textResourceFactory
                .fromArchiveEntry(spotbugsJar, path)
        } else {
            throw InvalidUserDataException(
                "The dependency on SpotBugs not found in 'spotbugs' configuration",
            )
        }
    }

    override fun setStylesheet(textResource: TextResource?) {
        stylesheet.set(textResource)
    }

    override fun setStylesheet(path: String?) {
        if (path == null) {
            stylesheet.set(null as TextResource)
        } else {
            val configuration = task
                .project
                .configurations
                .getByName(SpotBugsPlugin.CONFIG_NAME)
            val textResourceFactory = task
                .project
                .resources
                .text
            stylesheet.set(
                task.project.provider {
                    resolve(path, configuration, textResourceFactory)
                },
            )
        }
    }
}
