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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.file.Directory
import org.gradle.api.plugins.ReportingBasePlugin
import org.gradle.api.reporting.ReportingExtension
import org.gradle.util.GradleVersion
import java.io.IOException
import java.io.UncheckedIOException
import java.util.*
import kotlin.IllegalArgumentException
import kotlin.String
import kotlin.toString

class SpotBugsBasePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        verifyGradleVersion(GradleVersion.current())
        project.pluginManager.apply(ReportingBasePlugin::class.java)
        val extension = createExtension(project)
        createConfiguration(project, extension)
        createPluginConfiguration(project)
        val enableWorkerApi = getPropertyOrDefault(project, FEATURE_FLAG_WORKER_API, "true")
        val enableHybridWorker = getPropertyOrDefault(project, FEATURE_FLAG_HYBRID_WORKER, "true")
        project
            .tasks
            .withType(SpotBugsTask::class.java)
            .configureEach { task ->
                task.init(
                    extension,
                    enableWorkerApi.toBoolean(),
                    enableHybridWorker.toBoolean(),
                )
            }
    }

    private fun createExtension(project: Project): SpotBugsExtension {
        val extension = project
            .extensions
            .create(
                SpotBugsPlugin.EXTENSION_NAME,
                SpotBugsExtension::class.java,
                project,
                project.objects,
            )
        extension.ignoreFailures.convention(false)
        extension.showStackTraces.convention(false)
        extension.projectName.convention(project.provider { project.name })
        extension.release.convention(
            project.provider {
                project.version.toString()
            },
        )

        // ReportingBasePlugin should be applied before we create this SpotBugsExtension instance
        val baseReportsDir = project.extensions.getByType(
            ReportingExtension::class.java,
        ).baseDirectory
        extension
            .reportsDir
            .convention(
                baseReportsDir.map { directory: Directory ->
                    directory.dir(
                        DEFAULT_REPORTS_DIR_NAME,
                    )
                },
            )
        extension.useAuxclasspathFile.convention(true)
        extension.useJavaToolchains.convention(false)
        return extension
    }

    private fun createConfiguration(project: Project, extension: SpotBugsExtension) {
        val props = loadProperties()
        extension.toolVersion.convention(props.getProperty("spotbugs-version"))
        val configuration = project
            .configurations
            .create(SpotBugsPlugin.CONFIG_NAME)
            .setDescription("configuration for the SpotBugs engine")
            .setVisible(false)
            .setTransitive(true)
        configuration.defaultDependencies { dependencies: DependencySet ->
            dependencies.add(
                project
                    .dependencies
                    .create(
                        extension.toolVersion.map {
                            "com.github.spotbugs:spotbugs:$it"
                        },
                    ),
            )
        }
        val spotbugsSlf4j = project
            .configurations
            .create(SpotBugsPlugin.SLF4J_CONFIG_NAME)
            .setDescription("configuration for the SLF4J provider to run SpotBugs")
            .setVisible(false)
            .setTransitive(true)
        spotbugsSlf4j.defaultDependencies { dependencies: DependencySet ->
            dependencies.add(
                project
                    .dependencies
                    .create("org.slf4j:slf4j-simple:" + props.getProperty("slf4j-version")),
            )
        }
    }

    fun loadProperties(): Properties {
        val url = SpotBugsPlugin::class.java.classLoader.getResource("spotbugs-gradle-plugin.properties")
        try {
            url!!.openStream().use { input ->
                val prop = Properties()
                prop.load(input)
                return prop
            }
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }

    private fun createPluginConfiguration(project: Project): Configuration {
        return project
            .configurations
            .create(SpotBugsPlugin.PLUGINS_CONFIG_NAME)
            .setDescription("configuration for the external SpotBugs plugins")
            .setVisible(false)
            .setTransitive(false)
    }

    fun verifyGradleVersion(version: GradleVersion) {
        if (version < SUPPORTED_VERSION) {
            val message = String.format(
                "Gradle version %s is unsupported. Please use %s or later.",
                version,
                SUPPORTED_VERSION,
            )
            throw IllegalArgumentException(message)
        }
    }

    private fun getPropertyOrDefault(project: Project, propertyName: String, defaultValue: String): String {
        return if (project.hasProperty(propertyName)) project.property(propertyName).toString() else defaultValue
    }

    companion object {
        private const val FEATURE_FLAG_WORKER_API = "com.github.spotbugs.snom.worker"
        private const val FEATURE_FLAG_HYBRID_WORKER = "com.github.spotbugs.snom.javaexec-in-worker"
        private const val DEFAULT_REPORTS_DIR_NAME = "spotbugs"

        /**
         * Supported Gradle version described at [official manual site](http://spotbugs.readthedocs.io/en/latest/gradle.html). [The Gradle Worker API](https://guides.gradle.org/using-the-worker-api/) needs 5.6 or
         * later, so we use this value as minimal required version.
         */
        private val SUPPORTED_VERSION = GradleVersion.version("7.0")
    }
}
