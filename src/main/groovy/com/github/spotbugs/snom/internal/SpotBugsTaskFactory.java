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
package com.github.spotbugs.snom.internal;

import com.android.build.gradle.tasks.AndroidJavaCompile;
import com.github.spotbugs.snom.SpotBugsTask;
import java.util.List;
import java.util.stream.Collectors;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Provider;
import org.gradle.util.GUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpotBugsTaskFactory {
  private final Logger log = LoggerFactory.getLogger(SpotBugsTaskFactory.class);

  public void generate(Project project, Action<? super SpotBugsTask> configurationAction) {
    generateForJava(project, configurationAction);
    generateForAndroid(project, configurationAction);
  }

  private void generateForJava(Project project, Action<? super SpotBugsTask> configurationAction) {
    project
        .getPlugins()
        .withType(JavaBasePlugin.class)
        .configureEach(
            javaBasePlugin -> {
              JavaPluginConvention convention =
                  project.getConvention().getPlugin(JavaPluginConvention.class);
              convention
                  .getSourceSets()
                  .all(
                      sourceSet -> {
                        String name = sourceSet.getTaskName("spotbugs", null);
                        log.debug("Creating SpotBugsTaskForJava for {}", sourceSet);
                        project
                            .getTasks()
                            .register(
                                name,
                                SpotBugsTaskForJava.class,
                                task -> {
                                  task.setSourceSet(sourceSet);
                                  configurationAction.execute(task);
                                });
                      });
            });
  }

  private void generateForAndroid(
      Project project, Action<? super SpotBugsTask> configurationAction) {
    project
        .getPlugins()
        .withId(
            "com.android.application",
            plugin -> {
              List<Provider<SpotBugsTask>> spotbugsTasks =
                  project.getTasks().withType(AndroidJavaCompile.class).stream()
                      .map(
                          task -> {
                            String name =
                                GUtil.toLowerCamelCase("spotbugs " + task.getVariantName());
                            log.debug("Creating SpotBugsTaskForAndroid for {}", task);
                            return project
                                .getTasks()
                                .register(
                                    name,
                                    SpotBugsTaskForAndroid.class,
                                    spotbugsTask -> {
                                      configurationAction.execute(spotbugsTask);
                                      spotbugsTask.setTask(task);
                                    });
                          })
                      .map(p -> p.map(SpotBugsTask.class::cast))
                      .collect(Collectors.toList());
            });
  }
}
