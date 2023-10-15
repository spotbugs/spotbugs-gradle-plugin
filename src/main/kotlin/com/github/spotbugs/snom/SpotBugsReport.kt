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

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.reporting.CustomizableHtmlReport
import org.gradle.api.reporting.Report
import org.gradle.api.reporting.SingleFileReport
import org.gradle.api.resources.TextResource
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import java.io.File
import javax.inject.Inject

abstract class SpotBugsReport
    @Inject
    constructor(
        objects: ObjectFactory,
        @get:Internal
        protected val task: SpotBugsTask,
    ) :
    SingleFileReport, CustomizableHtmlReport { // to expose CustomizableHtmlReport#setStylesheet to build script
        private val destination: RegularFileProperty
        private val isRequired: Property<Boolean>

        init {
            destination = objects.fileProperty()
            isRequired = objects.property(Boolean::class.java).convention(true)
        }

        abstract fun toCommandLineOption(): String

        @Internal
        @Deprecated("use {@link #getOutputLocation()} instead.")
        fun getDestination(): File {
            return destination.get().asFile
        }

        override fun getOutputLocation(): RegularFileProperty {
            return destination
        }

        @Internal("This property returns always same value")
        override fun getOutputType(): Report.OutputType {
            return Report.OutputType.FILE
        }

        @Input
        override fun getRequired(): Property<Boolean> {
            return isRequired
        }

        @get:Deprecated("use {@link #getRequired()} instead.")
        @get:Internal
        @set:Deprecated("use {@code getRequired().set(value)} instead.")
        var isEnabled: Boolean
            get() = isRequired.get()
            set(b) {
                isRequired.set(b)
            }

        @Deprecated("use {@code getRequired().set(provider)} instead.")
        fun setEnabled(provider: Provider<Boolean>) {
            isRequired.set(provider)
        }

        @Deprecated("use {@code getOutputLocation().set(file)} instead.")
        override fun setDestination(file: File) {
            destination.set(file)
        }

        @Deprecated("use {@code getOutputLocation().set(provider)} instead.")
        fun setDestination(provider: Provider<File?>) {
            destination.set(task.project.layout.file(provider))
        }

        override fun configure(closure: Closure<in Report>): Report {
            return configure { report ->
                closure.delegate = report
                closure.call(report)
            }
        }

        fun configure(action: Action<in Report>): Report {
            action.execute(this)
            return this
        }

        @Internal("This property provides only a human readable name.")
        override fun getDisplayName(): String {
            return String.format("%s type report generated by the task %s", name, task.path)
        }

        // TODO adding an @Input triggers 'cannot be serialized' exception
        override fun getStylesheet(): TextResource? {
            return null
        }

        override fun setStylesheet(textResource: TextResource?) {
            throw UnsupportedOperationException(
                String.format(
                    "stylesheet property is not available in the %s type report",
                    name,
                ),
            )
        }

        open fun setStylesheet(path: String?) {
            throw UnsupportedOperationException(
                String.format(
                    "stylesheet property is not available in the %s type report",
                    name,
                ),
            )
        }
    }