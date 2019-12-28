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

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Objects;
import javax.inject.Inject;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.SourceSet;

@CacheableTask
public class SpotBugsTaskForJava extends SpotBugsTask {
  @NonNull private final SourceSet sourceSet;

  @Inject
  public SpotBugsTaskForJava(@NonNull SourceSet sourceSet, ObjectFactory objects) {
    super(objects);
    this.sourceSet = Objects.requireNonNull(sourceSet);
    dependsOn(sourceSet.getClassesTaskName());
  }

  @NonNull
  @Override
  FileCollection getSourceDirs() {
    return sourceSet.getAllJava();
  }

  @NonNull
  @Override
  FileCollection getClassDirs() {
    return sourceSet.getOutput();
  }

  @NonNull
  @Override
  FileCollection getAuxClassPaths() {
    return sourceSet.getCompileClasspath();
  }
}
