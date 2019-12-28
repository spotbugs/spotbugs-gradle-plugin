/**
 * Copyright 2019 SpotBugs team
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
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.util.GradleVersion;

public class SpotBugsPlugin implements Plugin<Project> {
  static final String CONFIG_NAME = "spotbugs";

  /**
   * Supported Gradle version described at <a
   * href="http://spotbugs.readthedocs.io/en/latest/gradle.html">official manual site</a>.
   */
  private static final GradleVersion SUPPORTED_VERSION = GradleVersion.version("4.0");

  @Override
  public void apply(Project project) {
    verifyGradleVersion(GradleVersion.current());

    SpotBugsExtension extension = createExtension(project);
    createConfiguration(project);
    createPluginConfiguration(project);

    createTasks(project, extension);
  }

  private SpotBugsExtension createExtension(Project project) {
    return project
        .getExtensions()
        .create("spotbugs", SpotBugsExtension.class, project.getObjects());
  }

  private void createConfiguration(Project project) {
    Properties props = loadProperties();

    Configuration configuration =
        project
            .getConfigurations()
            .create(CONFIG_NAME)
            .setDescription("configuration for the SpotBugs engine")
            .setVisible(false)
            .setTransitive(true);
    configuration.defaultDependencies(
        (DependencySet dependencies) ->
            dependencies.add(
                project
                    .getDependencies()
                    .create(
                        "com.github.spotbugs:spotbugs:" + props.getProperty("spotbugs-version"))));

    Configuration spotbugsSlf4j =
        project
            .getConfigurations()
            .create("spotbugsSlf4j")
            .setDescription("configuration for the SLF4J provider to run SpotBugs")
            .setVisible(false)
            .setTransitive(true);

    spotbugsSlf4j.defaultDependencies(
        (DependencySet dependencies) ->
            dependencies.add(
                project
                    .getDependencies()
                    .create("org.slf4j:slf4j-simple:" + props.getProperty("slf4j-version"))));
  }

  private Configuration createPluginConfiguration(Project project) {
    return project
        .getConfigurations()
        .create("spotbugsPlugin")
        .setDescription("configuration for the SpotBugs plugin")
        .setVisible(false)
        .setTransitive(true);
  }

  private void createTasks(Project project, SpotBugsExtension extension) {
    Task check = project.getTasks().getByName("check");
    new SpotBugsTaskGenerator()
        .generate(project)
        .forEach(
            task -> {
              check.dependsOn(task);
              task.init(extension);
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
}
