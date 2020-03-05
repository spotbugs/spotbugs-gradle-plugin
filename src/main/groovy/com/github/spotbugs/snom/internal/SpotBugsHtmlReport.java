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

import com.github.spotbugs.snom.SpotBugsReport;
import com.github.spotbugs.snom.SpotBugsTask;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.File;
import java.util.Optional;
import javax.inject.Inject;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.RegularFile;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.resources.TextResource;

public abstract class SpotBugsHtmlReport extends SpotBugsReport {
  private final Property<TextResource> stylesheet;
  private final Property<String> stylesheetPath;

  @Inject
  public SpotBugsHtmlReport(ObjectFactory objects, SpotBugsTask task) {
    super(objects, task);
    // the default reportsDir is "$buildDir/reports/spotbugs/${baseName}.html"
    setDestination(
        task.getReportsDir().file(task.getBaseName() + ".html").map(RegularFile::getAsFile));
    stylesheet = objects.property(TextResource.class);
    stylesheetPath = objects.property(String.class);
  }

  @NonNull
  @Override
  public Optional<String> toCommandLineOption() {
    @Nullable TextResource stylesheet = getStylesheet();

    if (stylesheet == null) {
      return Optional.of("-html");
    } else {
      return Optional.of("-html:" + stylesheet.asFile().getAbsolutePath());
    }
  }

  @Override
  public String getName() {
    return "HTML";
  }

  @Override
  public TextResource getStylesheet() {
    if (stylesheet.isPresent()) {
      return stylesheet.get();
    } else if (stylesheetPath.isPresent()) {
      return resolve(stylesheetPath.get());
    }

    return null;
  }

  private TextResource resolve(String path) {
    Optional<File> spotbugsJar =
        getTask().getProject().getConfigurations().getByName("spotbugs")
            .files(
                dependency ->
                    dependency.getGroup().equals("com.github.spotbugs")
                        && dependency.getName().equals("spotbugs"))
            .stream()
            .findFirst();
    if (spotbugsJar.isPresent()) {
      return getTask()
          .getProject()
          .getResources()
          .getText()
          .fromArchiveEntry(spotbugsJar.get(), path);
    } else {
      throw new InvalidUserDataException(
          "The dependency on SpotBugs not found in 'spotbugs' configuration");
    }
  }

  @Override
  public void setStylesheet(@Nullable TextResource textResource) {
    stylesheet.set(textResource);
  }

  @Override
  public void setStylesheet(@Nullable String path) {
    stylesheetPath.set(path);
  }
}
