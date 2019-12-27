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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Properties;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.JavaExec;
import org.gradle.util.GradleVersion;

public class SpotBugsPlugin implements Plugin<Project> {
  /**
   * Supported Gradle version described at <a
   * href="http://spotbugs.readthedocs.io/en/latest/gradle.html">official manual site</a>.
   */
  private static final GradleVersion SUPPORTED_VERSION = GradleVersion.version("4.0");

  @Override
  public void apply(Project project) {
    verifyGradleVersion(GradleVersion.current());

    SpotBugsExtension extension = createExtension(project);
    Configuration config = createConfiguration(project, extension);
    Configuration pluginConfig = createPluginConfiguration(project);

    // TODO adjust the timing to generate SpotBugsSpec from extension & configurations
    SpotBugsSpec baseSpec = createBaseSpec(extension, config, pluginConfig);
    createTasks(project, baseSpec);
  }

  private SpotBugsExtension createExtension(Project project) {
    SpotBugsExtension extension =
        project.getExtensions().create("spotbugs", SpotBugsExtension.class, project);
    extension.setToolVersion(loadToolVersion());
    return extension;
  }

  private Configuration createConfiguration(Project project, SpotBugsExtension extension) {
    Configuration configuration =
        project
            .getConfigurations()
            .create("spotbugs")
            .setDescription("configuration for the SpotBugs engine")
            .setVisible(false)
            .setTransitive(true);

    configuration.defaultDependencies(
        (DependencySet dependencies) ->
            dependencies.add(
                project
                    .getDependencies()
                    .create("com.github.spotbugs:spotbugs:" + extension.getToolVersion())));
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

  private void createTasks(Project project, SpotBugsSpec baseSpec) {
    Logger logger = project.getLogger();
    Task check = project.getTasks().getByName("check");

    Arrays.asList(new SpecGeneratorForJava(baseSpec)).stream()
        .flatMap(specGenerator -> specGenerator.generate(project).stream())
        .map(spec -> createTask(project, spec))
        .forEach(
            task -> {
              check.dependsOn(task);
              logger.debug("Task {} is created", task.getName());
            });
  }

  private JavaExec createTask(Project project, SpotBugsSpec spec) {
    return project
        .getTasks()
        .create(
            spec.name(),
            JavaExec.class,
            (JavaExec javaExec) -> {
              javaExec.setDescription("Run SpotBugs analysis");
              javaExec.setMain("edu.umd.cs.findbugs.FindBugs2");
              spec.applyTo(javaExec);
            });
  }

  private SpotBugsSpec createBaseSpec(
      SpotBugsExtension extension, Configuration config, Configuration pluginConfig) {
    return ImmutableSpotBugsSpec.builder()
        .from(extension.toBaseSpec())
        .spotbugsJar(config.getFiles())
        .addAllPlugins(pluginConfig.files())
        .build();
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
