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
package com.github.spotbugs.snom.internal;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.BaseExtension;
import com.android.build.gradle.LibraryExtension;
import com.android.build.gradle.api.BaseVariant;
import com.github.spotbugs.snom.SpotBugsTask;
import org.gradle.api.Action;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.util.GUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpotBugsTaskFactory {
  private final Logger log = LoggerFactory.getLogger(SpotBugsTaskFactory.class);

  public void generate(Project project) {
    generateForJava(project);
    generateForAndroid(project);
  }

  private void generateForJava(Project project) {
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
                            .register(
                                name,
                                SpotBugsTask.class,
                                task -> {
                                  task.setSourceDirs(
                                      sourceSet.getAllSource().getSourceDirectories());
                                  task.setClassDirs(sourceSet.getOutput());
                                  task.setAuxClassPaths(sourceSet.getCompileClasspath());
                                });
                      });
            });
  }

  private void generateForAndroid(Project project) {

    @SuppressWarnings("rawtypes")
    final Action<? super Plugin> action =
        (Action<Plugin>)
            plugin -> {
              final BaseExtension baseExtension =
                  project.getExtensions().getByType(BaseExtension.class);
              DomainObjectSet<? extends BaseVariant> variants;
              if (baseExtension instanceof AppExtension) {
                variants = ((AppExtension) baseExtension).getApplicationVariants();
              } else if (baseExtension instanceof LibraryExtension) {
                variants = ((LibraryExtension) baseExtension).getLibraryVariants();
              } else {
                throw new GradleException("Unrecognized Android extension " + baseExtension);
              }
              variants.all(
                  (BaseVariant variant) -> {
                    String spotbugsTaskName =
                        GUtil.toLowerCamelCase("spotbugs " + variant.getName());
                    log.debug("Creating SpotBugsTask for {}", variant.getName());
                    project
                        .getTasks()
                        .register(
                            spotbugsTaskName,
                            SpotBugsTask.class,
                            spotbugsTask -> {
                              final JavaCompile javaCompile =
                                  variant.getJavaCompileProvider().get();
                              spotbugsTask.setSourceDirs(javaCompile.getSource());
                              spotbugsTask.setClassDirs(
                                  project.files(javaCompile.getDestinationDir()));
                              spotbugsTask.setAuxClassPaths(javaCompile.getClasspath());
                              spotbugsTask.dependsOn(javaCompile);
                            });
                  });
            };

    project.getPlugins().withId("com.android.application", action);
    project.getPlugins().withId("com.android.library", action);
  }
}
