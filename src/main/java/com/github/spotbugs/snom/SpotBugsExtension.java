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
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;
import javax.inject.Inject;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

public class SpotBugsExtension {
  @NonNull final Property<Boolean> ignoreFailures;
  @NonNull final Property<Boolean> showProgress;
  @NonNull final Property<Confidence> reportLevel;
  @NonNull final Property<Effort> effort;
  @NonNull final ListProperty<String> visitors;
  @NonNull final ListProperty<String> omitVisitors;
  @NonNull final Property<File> reportsDir;
  @NonNull final Property<File> includeFilter;
  @NonNull final Property<File> excludeFilter;
  @NonNull final ListProperty<String> onlyAnalyze;
  @NonNull final Property<String> projectName;
  @NonNull final Property<String> release;

  @Inject
  public SpotBugsExtension(Project project, ObjectFactory objects) {
    ignoreFailures = objects.property(Boolean.class);
    showProgress = objects.property(Boolean.class);
    reportLevel = objects.property(Confidence.class);
    effort = objects.property(Effort.class);
    visitors = objects.listProperty(String.class);
    omitVisitors = objects.listProperty(String.class);
    reportsDir = objects.property(File.class);
    // the default reportsDir is "$buildDir/reports/spotbugs"
    reportsDir.set(
        project.getBuildDir().toPath().resolve(Paths.get("reports", "spotbugs")).toFile());
    includeFilter = objects.property(File.class);
    excludeFilter = objects.property(File.class);
    onlyAnalyze = objects.listProperty(String.class);
    projectName = objects.property(String.class);
    projectName.convention(project.getName());
    release = objects.property(String.class);
  }

  public Property<File> getReportsDir() {
    return reportsDir;
  }

  public void setReportsDir(Property<File> file) {
    reportsDir.set(file);
  }

  public void setReportsDir(File file) {
    reportsDir.set(file);
  }

  public Property<File> getIncludeFilter() {
    return includeFilter;
  }

  public void setIncludeFilter(Property<File> file) {
    includeFilter.set(file);
  }

  public void setIncludeFilter(@Nullable File file) {
    includeFilter.set(file);
  }

  public Property<File> getExcludeFilter() {
    return excludeFilter;
  }

  public void setExcludeFilter(Property<File> file) {
    excludeFilter.set(file);
  }

  public void setExcludeFilter(@Nullable File file) {
    excludeFilter.set(file);
  }

  public void setVisitors(@Nullable Collection<String> collection) {
    visitors.set(collection);
  }

  public void setOmitVisitors(@Nullable Collection<String> collection) {
    omitVisitors.set(collection);
  }

  public void setOnlyAnalyze(@Nullable Collection<String> collection) {
    onlyAnalyze.set(collection);
  }
}
