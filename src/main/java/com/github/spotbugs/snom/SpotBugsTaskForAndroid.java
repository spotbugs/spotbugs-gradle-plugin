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
import java.util.Objects;
import javax.inject.Inject;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.model.ObjectFactory;

public class SpotBugsTaskForAndroid extends SpotBugsTask {
  private final AndroidJavaCompile task;

  @Inject
  public SpotBugsTaskForAndroid(AndroidJavaCompile task, ObjectFactory objects) {
    super(objects);
    this.task = Objects.requireNonNull(task);
  }

  @Override
  void applyTo(ImmutableSpotBugsSpec.Builder builder) {
    super.applyTo(builder);
    // TODO consider input and output for incremental build
    FileTree sourceDirs = task.getSource();
    FileTree classDirs = task.getOutputDirectory().getAsFileTree();
    FileCollection auxClassPaths = task.getClasspath();
    dependsOn(task);

    builder.sourceDirs(sourceDirs).addAllClassDirs(classDirs).addAllAuxClassPaths(auxClassPaths);
  }
}
