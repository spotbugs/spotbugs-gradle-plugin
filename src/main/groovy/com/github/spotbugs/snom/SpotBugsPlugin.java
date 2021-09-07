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

import com.github.spotbugs.snom.internal.SpotBugsTaskFactory;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpotBugsPlugin implements Plugin<Project> {
  public static final String CONFIG_NAME = "spotbugs";
  public static final String PLUGINS_CONFIG_NAME = "spotbugsPlugins";
  public static final String SLF4J_CONFIG_NAME = "spotbugsSlf4j";
  public static final String EXTENSION_NAME = "spotbugs";

  private final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply(SpotBugsBasePlugin.class);
    SpotBugsExtension extension = project.getExtensions().findByType(SpotBugsExtension.class);
    project
        .getPluginManager()
        .withPlugin(
            "java-base",
            javaBase -> {
              log.debug(
                  "The javaBase plugin has been applied, so making the check task depending on all of SpotBugsTask");
              project
                  .getTasks()
                  .named(JavaBasePlugin.CHECK_TASK_NAME)
                  .configure(
                      task -> task.dependsOn(project.getTasks().withType(SpotBugsTask.class)));
            });
    createTasks(project, extension);
  }

  private void createTasks(Project project, SpotBugsExtension extension) {
    new SpotBugsTaskFactory().generate(project);
  }
}
