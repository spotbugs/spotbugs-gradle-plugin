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

import com.github.spotbugs.snom.SpotBugsReport
import com.github.spotbugs.snom.SpotBugsTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.artifacts.Dependency
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.resources.TextResource
import javax.inject.Inject

abstract class SpotBugsHtmlReport @Inject constructor(objects: ObjectFactory, task: SpotBugsTask) :
    SpotBugsReport(objects, task) {
    private val stylesheet: Property<TextResource>
    private val stylesheetPath: Property<String>

    init {
        // the default reportsDir is "$buildDir/reports/spotbugs/${baseName}.html"
        outputLocation.convention(task.reportsDir.file(task.baseName + ".html"))
        stylesheet = objects.property(TextResource::class.java)
        stylesheetPath = objects.property(String::class.java)
    }

    override fun toCommandLineOption(): String {
        val stylesheet = getStylesheet()
        return if (stylesheet == null) {
            "-html"
        } else {
            "-html:" + stylesheet.asFile().absolutePath
        }
    }

    override fun getStylesheet(): TextResource? {
        if (stylesheet.isPresent) {
            return stylesheet.get()
        } else if (stylesheetPath.isPresent) {
            return resolve(stylesheetPath.get())
        }
        return null
    }

    private fun resolve(path: String): TextResource {
        val spotbugsJar = task
            .project
            .configurations
            .getByName("spotbugs")
            .files { dependency: Dependency -> dependency.group == "com.github.spotbugs" && dependency.name == "spotbugs" }
            .stream()
            .findFirst()
        return if (spotbugsJar.isPresent) {
            task
                .project
                .resources
                .text
                .fromArchiveEntry(spotbugsJar.get(), path)
        } else {
            throw InvalidUserDataException(
                "The dependency on SpotBugs not found in 'spotbugs' configuration"
            )
        }
    }

    override fun setStylesheet(textResource: TextResource?) {
        stylesheet.set(textResource)
    }

    override fun setStylesheet(path: String?) {
        stylesheetPath.set(path)
    }
}
