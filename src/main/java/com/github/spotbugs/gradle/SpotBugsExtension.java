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

import java.util.Objects;
import javax.annotation.Nonnull;
import org.gradle.api.Project;

public class SpotBugsExtension {
  private boolean ignoreFailures = false;

  @Nonnull private Confidence reportLevel = Confidence.DEFAULT;

  @Nonnull private Effort effort = Effort.DEFAULT;

  private boolean generateTask = true;

  public SpotBugsExtension(Project project) {
    // TODO disable task generation when android plugin is activated?
  }

  public boolean isIgnoreFailures() {
    return ignoreFailures;
  }

  public void setIgnoreFailures(boolean ignoreFailures) {
    this.ignoreFailures = ignoreFailures;
  }

  public Confidence getReportLevel() {
    return reportLevel;
  }

  public void setReportLevel(Confidence reportLevel) {
    this.reportLevel = Objects.requireNonNull(reportLevel);
  }

  public Effort getEffort() {
    return effort;
  }

  public void setEffort(Effort effort) {
    this.effort = Objects.requireNonNull(effort);
  }

  public boolean isGenerateTask() {
    return generateTask;
  }

  public void setGenerateTask(boolean generateTask) {
    this.generateTask = generateTask;
  }

  void applyTo(ImmutableSpotBugsSpec.Builder builder) {
    builder.isIgnoreFailures(isIgnoreFailures());
    if (getEffort() != null) {
      builder.addExtraArguments("-effort:" + getEffort().toString().toLowerCase());
    }
    if (getReportLevel() != null) {
      builder.addExtraArguments(getReportLevel().toCommandLineOption());
    }
  }
}
