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

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

class SpecGeneratorForJava implements SpotBugsSpecGenerator {
  private final SpotBugsSpec baseSpec;

  /** @param baseSpec the base spec generated from the {@code spotbugs} extension. */
  SpecGeneratorForJava(SpotBugsSpec baseSpec) {
    this.baseSpec = Objects.requireNonNull(baseSpec);
  }

  @Override
  public Set<SpotBugsSpec> generate(Project project) {
    JavaPluginConvention convention = project.getConvention().getPlugin(JavaPluginConvention.class);
    if (convention == null) {
      return Collections.emptySet();
    }

    SourceSetContainer sourceSets = convention.getSourceSets();
    return sourceSets.stream().map(this::convert).collect(Collectors.toSet());
  }

  private SpotBugsSpec convert(SourceSet sourceSet) {
    return ImmutableSpotBugsSpec.builder()
        .from(baseSpec)
        .name(sourceSet.getTaskName("spotbugs", null))
        .addAllSourceDirs(sourceSet.getAllJava().getSrcDirs())
        .addAllClassDirs(sourceSet.getOutput().getFiles())
        .addAllAuxClassPaths(sourceSet.getCompileClasspath())
        .build();
  }
}
