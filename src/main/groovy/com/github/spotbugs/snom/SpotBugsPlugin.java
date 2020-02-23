/*
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

import com.github.spotbugs.snom.internal.SpotBugsTaskFactory;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

public class SpotBugsPlugin implements Plugin<Project> {
  public static final String CONFIG_NAME = "spotbugs";
  public static final String PLUGINS_CONFIG_NAME = "spotbugsPlugins";
  public static final String SLF4J_CONFIG_NAME = "spotbugsSlf4j";
  public static final String EXTENSION_NAME = "spotbugs";

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply(SpotBugsBasePlugin.class);
    SpotBugsExtension extension = project.getExtensions().findByType(SpotBugsExtension.class);
    createTasks(project, extension);
  }

  private void createTasks(Project project, SpotBugsExtension extension) {
    @Nullable Task check = project.getTasks().findByName("check");
    new SpotBugsTaskFactory()
        .generate(
            project,
            task -> {
              if (check != null) {
                check.dependsOn(task);
              }
              task.init(extension);
            });
  }
}
