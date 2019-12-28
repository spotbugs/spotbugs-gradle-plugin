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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

public class SpotBugsExtension {
  @NonNull private final Property<Boolean> ignoreFailures;
  @NonNull private final Property<Boolean> showProgress;
  @NonNull private Confidence reportLevel = Confidence.DEFAULT;
  @NonNull private Effort effort = Effort.DEFAULT;
  @NonNull private List<String> visitors = Collections.emptyList();
  @NonNull private List<String> omitVisitors = Collections.emptyList();

  public SpotBugsExtension(Project project) {
    ignoreFailures = project.getObjects().property(Boolean.class);
    showProgress = project.getObjects().property(Boolean.class);
  }

  @Input
  public boolean isIgnoreFailures() {
    return ignoreFailures.getOrElse(Boolean.FALSE);
  }

  public void setIgnoreFailures(boolean ignoreFailures) {
    this.ignoreFailures.set(ignoreFailures);
  }

  @Input
  public boolean isShowProgress() {
    return showProgress.getOrElse(Boolean.FALSE);
  }

  public void setShowProgress(boolean showProgress) {
    this.showProgress.set(showProgress);
  }

  @Input
  public List<String> getVisitors() {
    return visitors;
  }

  public void setVisitors(List<String> visitors) {
    this.visitors = Collections.unmodifiableList(new ArrayList<>(visitors));
  }

  @Input
  public List<String> getOmitVisitors() {
    return omitVisitors;
  }

  public void setOmitVisitors(List<String> omitVisitors) {
    this.omitVisitors = Collections.unmodifiableList(new ArrayList<>(omitVisitors));
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

  void applyTo(ImmutableSpotBugsSpec.Builder builder) {
    builder.isIgnoreFailures(isIgnoreFailures());
    builder.isShowProgress(isShowProgress());
    if (getEffort() != null) {
      builder.addExtraArguments("-effort:" + getEffort().toString().toLowerCase());
    }
    if (getReportLevel() != null) {
      builder.addExtraArguments(getReportLevel().toCommandLineOption());
    }
    if (!visitors.isEmpty()) {
      builder.addExtraArguments("-visitors");
      builder.addExtraArguments(visitors.stream().collect(Collectors.joining(",")));
    }
    if (!omitVisitors.isEmpty()) {
      builder.addExtraArguments("-omitVisitors");
      builder.addExtraArguments(omitVisitors.stream().collect(Collectors.joining(",")));
    }
  }
}
