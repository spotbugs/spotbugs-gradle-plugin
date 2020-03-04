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
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
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
                        log.debug("Creating SpotBugsTask for {}", sourceSet);
                        project
                            .getTasks()
                            .create(
                                name,
                                SpotBugsTask.class,
                                task -> {
                                  task.setSourceDirs(
                                      sourceSet.getAllSource().getSourceDirectories());
                                  task.setClassDirs(sourceSet.getOutput());
                                  task.setAuxClassPaths(sourceSet.getCompileClasspath());
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
              project
                  .getTasks()
                  .withType(AndroidJavaCompile.class)
                  .all(
                      task -> {
                        String name = GUtil.toLowerCamelCase("spotbugs " + task.getVariantName());
                        log.debug("Creating SpotBugsTask for {}", task);
                        project
                            .getTasks()
                            .register(
                                name,
                                SpotBugsTask.class,
                                spotbugsTask -> {
                                  spotbugsTask.setSourceDirs(task.getSource());
                                  spotbugsTask.setClassDirs(
                                      task.getOutputDirectory().getAsFileTree());
                                  spotbugsTask.setAuxClassPaths(task.getClasspath());
                                  configurationAction.execute(spotbugsTask);
                                });
                      });
            });
  }
}
