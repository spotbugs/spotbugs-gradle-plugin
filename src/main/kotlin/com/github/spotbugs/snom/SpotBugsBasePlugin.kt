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

import java.io.IOException
import java.io.UncheckedIOException
import java.util.Properties
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.plugins.ReportingBasePlugin
import org.gradle.api.reporting.ReportingExtension
import org.gradle.util.GradleVersion

class SpotBugsBasePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        verifyGradleVersion(GradleVersion.current())
        project.pluginManager.apply(ReportingBasePlugin::class.java)
        val extension = createExtension(project)
        createConfiguration(project, extension)
        createPluginConfiguration(project.configurations)
        val enableWorkerApi = project.providers.gradleProperty(FEATURE_FLAG_WORKER_API).getOrElse("true")
        project.tasks.withType(SpotBugsTask::class.java).configureEach {
            it.init(extension, enableWorkerApi.toBoolean())
        }
    }

    private fun createExtension(project: Project): SpotBugsExtension {
        return project.extensions.create(SpotBugsPlugin.EXTENSION_NAME, SpotBugsExtension::class.java).apply {
            ignoreFailures.convention(false)
            showStackTraces.convention(false)
            projectName.convention(project.provider { project.name })
            release.convention(
                project.provider {
                    project.version.toString()
                },
            )

            // ReportingBasePlugin should be applied before we create this SpotBugsExtension instance
            val baseReportsDir = project.extensions.getByType(ReportingExtension::class.java).baseDirectory
            reportsDir.convention(
                baseReportsDir.map {
                    it.dir(DEFAULT_REPORTS_DIR_NAME)
                },
            )
            useAuxclasspathFile.convention(true)
            useJavaToolchains.convention(true)
        }
    }

    private fun createConfiguration(
        project: Project,
        extension: SpotBugsExtension,
    ) {
        val props = loadProperties()
        extension.toolVersion.convention(props.getProperty("spotbugs-version"))
        val configs = project.configurations

        configs.register(SpotBugsPlugin.CONFIG_NAME) {
            it.setDescription("configuration for the SpotBugs engine")
            it.setVisible(false)
            it.setTransitive(true)
            it.defaultDependencies { deps ->
                val dep = project.dependencies.create("com.github.spotbugs:spotbugs:" + extension.toolVersion.get())
                deps.add(dep)
            }
        }

        configs.register(SpotBugsPlugin.SLF4J_CONFIG_NAME) {
            it.description = "configuration for the SLF4J provider to run SpotBugs"
            it.setVisible(false)
            it.setTransitive(true)
            it.defaultDependencies { deps ->
                val dep = project.dependencies.create("org.slf4j:slf4j-simple:" + props.getProperty("slf4j-version"))
                deps.add(dep)
            }
        }
    }

    fun loadProperties(): Properties {
        val url = SpotBugsPlugin::class.java.classLoader.getResource("spotbugs-gradle-plugin.properties")
        url ?: error("spotbugs-gradle-plugin.properties not found")
        try {
            url.openStream().use {
                val prop = Properties()
                prop.load(it)
                return prop
            }
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }

    private fun createPluginConfiguration(configs: ConfigurationContainer) {
        configs.register(SpotBugsPlugin.PLUGINS_CONFIG_NAME) {
            it.setDescription("configuration for the external SpotBugs plugins")
            it.setVisible(false)
            it.setTransitive(false)
        }
    }

    fun verifyGradleVersion(version: GradleVersion) {
        require(version >= SUPPORTED_VERSION) {
            "Gradle version $version is unsupported. Please use $SUPPORTED_VERSION or later."
        }
    }

    companion object {
        private const val FEATURE_FLAG_WORKER_API = "com.github.spotbugs.snom.worker"
        private const val DEFAULT_REPORTS_DIR_NAME = "spotbugs"

        /**
         * Supported Gradle version described at [official manual site](http://spotbugs.readthedocs.io/en/latest/gradle.html).
         * The convention API provides replacement from 7.1 and later, so we use this value as minimal required version.
         */
        @Suppress("MaxLineLength")
        private val SUPPORTED_VERSION = GradleVersion.version("7.1")
    }
}
