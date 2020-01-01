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

import com.android.build.gradle.tasks.AndroidJavaCompile;
import com.github.spotbugs.snom.internal.SpotBugsTaskForAndroid;
import com.github.spotbugs.snom.internal.SpotBugsTaskForJava;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.util.GUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SpotBugsTaskGenerator {
  private final Logger log = LoggerFactory.getLogger(SpotBugsTaskGenerator.class);

  Set<SpotBugsTask> generate(Project project) {
    Set<SpotBugsTask> tasks = new HashSet<>();
    tasks.addAll(generateForJava(project));
    tasks.addAll(generateForAndroid(project));
    return tasks;
  }

  private Set<SpotBugsTask> generateForJava(Project project) {
    JavaPluginConvention convention = project.getConvention().getPlugin(JavaPluginConvention.class);
    if (convention == null) {
      return Collections.emptySet();
    }

    SourceSetContainer sourceSets = convention.getSourceSets();
    return sourceSets.stream()
        .map(
            sourceSet -> {
              String name = sourceSet.getTaskName("spotbugs", null);
              return project
                  .getTasks()
                  .create(name, SpotBugsTaskForJava.class, task -> task.setSourceSet(sourceSet));
            })
        .collect(Collectors.toSet());
  }

  private Set<SpotBugsTask> generateForAndroid(Project project) {
    try {
      Class.forName("com.android.build.gradle.tasks.AndroidJavaCompile");
    } catch (ClassNotFoundException ignore) {
      log.info("Android Gradle Plugin not found, so skip the task generation for Android");
      return Collections.emptySet();
    }

    return project.getTasks().withType(AndroidJavaCompile.class).stream()
        .map(
            task -> {
              String name = GUtil.toLowerCamelCase("spotbugs " + task.getVariantName());
              return project
                  .getTasks()
                  .create(
                      name,
                      SpotBugsTaskForAndroid.class,
                      spotbugsTask -> spotbugsTask.setTask(task));
            })
        .collect(Collectors.toSet());
  }
}
