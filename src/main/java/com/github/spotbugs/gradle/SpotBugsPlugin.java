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
package com.github.spotbugs.gradle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Properties;
import java.util.Set;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.util.GradleVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpotBugsPlugin implements Plugin<Project> {
  private final Logger log = LoggerFactory.getLogger(SpotBugsPlugin.class);

  /**
   * Supported Gradle version described at <a
   * href="http://spotbugs.readthedocs.io/en/latest/gradle.html">official manual site</a>.
   */
  private static final GradleVersion SUPPORTED_VERSION = GradleVersion.version("4.0");

  @Override
  public void apply(Project project) {
    verifyGradleVersion(GradleVersion.current());

    createExtension(project);
    createConfiguration(project);
    createPluginConfiguration(project);

    createTasks(project);
    project.afterEvaluate(this::configureTasks);
  }

  private void configureTasks(Project project) {
    project
        .getTasks()
        .withType(SpotBugsTask.class)
        .configureEach(
            task -> {
              Configuration config = project.getConfigurations().getByName("spotbugs");
              Configuration pluginConfig = project.getConfigurations().getByName("spotbugsPlugin");
              Set<File> spotbugsJar = config.getFiles();
              log.info("SpotBugs jar file: {}", spotbugsJar);
              ImmutableSpotBugsSpec.Builder builder =
                  ImmutableSpotBugsSpec.builder()
                      .spotbugsJar(spotbugsJar)
                      .addAllPlugins(pluginConfig.files());

              SpotBugsExtension extension =
                  project.getExtensions().findByType(SpotBugsExtension.class);
              extension.applyTo(builder);

              task.applyTo(builder);
              builder.build().applyTo(task);
            });
  }

  private SpotBugsExtension createExtension(Project project) {
    SpotBugsExtension extension =
        project.getExtensions().create("spotbugs", SpotBugsExtension.class, project);
    return extension;
  }

  private Configuration createConfiguration(Project project) {
    Configuration configuration =
        project
            .getConfigurations()
            .create("spotbugs")
            .setDescription("configuration for the SpotBugs engine")
            .setVisible(false)
            .setTransitive(true);

    configuration.defaultDependencies(
        (DependencySet dependencies) -> {
          dependencies.add(
              project
                  .getDependencies()
                  .create("com.github.spotbugs:spotbugs:" + loadToolVersion()));
          // TODO add SLF4J
        });
    return configuration;
  }

  private Configuration createPluginConfiguration(Project project) {
    Configuration configuration =
        project
            .getConfigurations()
            .create("spotbugsPlugin")
            .setDescription("configuration for the SpotBugs plugin")
            .setVisible(false)
            .setTransitive(true);
    return configuration;
  }

  private void createTasks(Project project) {
    Task check = project.getTasks().getByName("check");
    new SpotBugsTaskGenerator().generate(project).forEach(check::dependsOn);
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

  String loadToolVersion() {
    URL url =
        SpotBugsPlugin.class.getClassLoader().getResource("spotbugs-gradle-plugin.properties");
    try (InputStream input = url.openStream()) {
      Properties prop = new Properties();
      prop.load(input);
      return prop.getProperty("spotbugs-version");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
