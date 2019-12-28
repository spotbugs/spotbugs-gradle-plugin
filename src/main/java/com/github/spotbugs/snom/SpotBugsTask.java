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
import edu.umd.cs.findbugs.annotations.OverrideMustInvoke;
import java.util.stream.Collectors;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.JavaExec;

abstract class SpotBugsTask extends JavaExec {
  @Input @NonNull final Property<Boolean> ignoreFailures;
  @Input @NonNull final Property<Boolean> showProgress;
  @Input @NonNull final Property<Confidence> reportLevel;
  @Input @NonNull final Property<Effort> effort;
  @Input @NonNull final ListProperty<String> visitors;
  @Input @NonNull final ListProperty<String> omitVisitors;

  public SpotBugsTask(ObjectFactory objects) {
    ignoreFailures = objects.property(Boolean.class);
    showProgress = objects.property(Boolean.class);
    reportLevel = objects.property(Confidence.class);
    effort = objects.property(Effort.class);
    visitors = objects.listProperty(String.class);
    omitVisitors = objects.listProperty(String.class);
  }

  /**
   * Set properties from extension right after the task creation. User may overwrite these
   * properties by build script.
   *
   * @param extension the source extension to copy the properties.
   */
  final void init(SpotBugsExtension extension) {
    ignoreFailures.set(extension.ignoreFailures);
    showProgress.set(extension.showProgress);
    reportLevel.set(extension.reportLevel);
    effort.set(extension.effort);
    visitors.set(extension.visitors);
    omitVisitors.set(extension.omitVisitors);
  }

  @OverrideMustInvoke
  void applyTo(ImmutableSpotBugsSpec.Builder builder) {
    builder.isIgnoreFailures(ignoreFailures.getOrElse(false));
    builder.isShowProgress(showProgress.getOrElse(false));
    if (effort.isPresent()) {
      builder.addExtraArguments("-effort:" + effort.get().toString().toLowerCase());
    }
    if (reportLevel.isPresent()) {
      builder.addExtraArguments(reportLevel.get().toCommandLineOption());
    }
    if (visitors.isPresent() && !visitors.get().isEmpty()) {
      builder.addExtraArguments("-visitors");
      builder.addExtraArguments(visitors.get().stream().collect(Collectors.joining(",")));
    }
    if (omitVisitors.isPresent() && !omitVisitors.get().isEmpty()) {
      builder.addExtraArguments("-omitVisitors");
      builder.addExtraArguments(omitVisitors.get().stream().collect(Collectors.joining(",")));
    }
  }
}
