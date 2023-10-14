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
package com.github.spotbugs.snom;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Properties;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.ReportingBasePlugin;
import org.gradle.util.GradleVersion;

public class SpotBugsBasePlugin implements Plugin<Project> {
  private static final String FEATURE_FLAG_WORKER_API = "com.github.spotbugs.snom.worker";
  private static final String FEATURE_FLAG_HYBRID_WORKER =
      "com.github.spotbugs.snom.javaexec-in-worker";

  /**
   * Supported Gradle version described at <a
   * href="http://spotbugs.readthedocs.io/en/latest/gradle.html">official manual site</a>. <a
   * href="https://guides.gradle.org/using-the-worker-api/">The Gradle Worker API</a> needs 5.6 or
   * later, so we use this value as minimal required version.
   */
  private static final GradleVersion SUPPORTED_VERSION = GradleVersion.version("7.0");

  @Override
  public void apply(Project project) {
    verifyGradleVersion(GradleVersion.current());
    project.getPluginManager().apply(ReportingBasePlugin.class);

    SpotBugsExtension extension = createExtension(project);
    createConfiguration(project, extension);
    createPluginConfiguration(project.getConfigurations());

    String enableWorkerApi = getPropertyOrDefault(project, FEATURE_FLAG_WORKER_API, "true");
    String enableHybridWorker = getPropertyOrDefault(project, FEATURE_FLAG_HYBRID_WORKER, "true");
    project
        .getTasks()
        .withType(SpotBugsTask.class)
        .configureEach(
            task ->
                task.init(
                    extension,
                    Boolean.parseBoolean(enableWorkerApi),
                    Boolean.parseBoolean(enableHybridWorker)));
  }

  private SpotBugsExtension createExtension(Project project) {
    return project
        .getExtensions()
        .create(
            SpotBugsPlugin.EXTENSION_NAME, SpotBugsExtension.class, project, project.getObjects());
  }

  private void createConfiguration(Project project, SpotBugsExtension extension) {
    Properties props = loadProperties();
    extension.getToolVersion().convention(props.getProperty("spotbugs-version"));
    ConfigurationContainer configs = project.getConfigurations();

    configs.register(
        SpotBugsPlugin.CONFIG_NAME,
        c -> {
          c.setDescription("configuration for the SpotBugs engine");
          c.setVisible(false);
          c.setTransitive(true);
          c.defaultDependencies(
              d -> {
                Dependency dep =
                    project
                        .getDependencies()
                        .create("com.github.spotbugs:spotbugs:" + extension.getToolVersion().get());
                d.add(dep);
              });
        });

    configs.register(
        SpotBugsPlugin.SLF4J_CONFIG_NAME,
        c -> {
          c.setDescription("configuration for the SLF4J provider to run SpotBugs");
          c.setVisible(false);
          c.setTransitive(true);
          c.defaultDependencies(
              d -> {
                Dependency dep =
                    project
                        .getDependencies()
                        .create("org.slf4j:slf4j-simple:" + props.getProperty("slf4j-version"));
                d.add(dep);
              });
        });
  }

  Properties loadProperties() {
    URL url =
        SpotBugsPlugin.class.getClassLoader().getResource("spotbugs-gradle-plugin.properties");
    try (InputStream input = url.openStream()) {
      Properties prop = new Properties();
      prop.load(input);
      return prop;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void createPluginConfiguration(ConfigurationContainer configs) {
    configs.register(
        SpotBugsPlugin.PLUGINS_CONFIG_NAME,
        c -> {
          c.setDescription("configuration for the external SpotBugs plugins");
          c.setVisible(false);
          c.setTransitive(false);
        });
  }

  void verifyGradleVersion(GradleVersion version) {
    if (version.compareTo(SUPPORTED_VERSION) < 0) {
      String message =
          String.format(
              "Gradle version %s is unsupported. Please use %s or later.",
              version, SUPPORTED_VERSION);
      throw new IllegalArgumentException(message);
    }
  }

  private String getPropertyOrDefault(Project project, String propertyName, String defaultValue) {
    return project.hasProperty(propertyName)
        ? project.property(propertyName).toString()
        : defaultValue;
  }
}
