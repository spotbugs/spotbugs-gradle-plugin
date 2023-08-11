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

import com.github.spotbugs.snom.SpotBugsPlugin;
import com.github.spotbugs.snom.SpotBugsReport;
import com.github.spotbugs.snom.SpotBugsTask;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.File;
import java.util.Optional;
import javax.inject.Inject;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.resources.TextResource;
import org.gradle.api.resources.TextResourceFactory;

public abstract class SpotBugsHtmlReport extends SpotBugsReport {
  private final Property<TextResource> stylesheet;

  @Inject
  public SpotBugsHtmlReport(ObjectFactory objects, SpotBugsTask task) {
    super(objects, task);
    // the default reportsDir is "$buildDir/reports/spotbugs/${baseName}.html"
    getOutputLocation().convention(task.getReportsDir().file(task.getBaseName() + ".html"));
    stylesheet = objects.property(TextResource.class);
  }

  @NonNull
  @Override
  public String toCommandLineOption() {
    return stylesheet
        .map(textResource -> "-html:" + textResource.asFile().getAbsolutePath())
        .getOrElse("-html");
  }

  @Override
  public TextResource getStylesheet() {
    return stylesheet.getOrNull();
  }

  private TextResource resolve(
      String path, Configuration configuration, TextResourceFactory factory) {
    Optional<File> spotbugsJar =
        configuration
            .files(
                dependency ->
                    dependency.getGroup().equals("com.github.spotbugs")
                        && dependency.getName().equals("spotbugs"))
            .stream()
            .findFirst();
    if (spotbugsJar.isPresent()) {
      return factory.fromArchiveEntry(spotbugsJar.get(), path);
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
    Configuration configuration =
        getTask().getProject().getConfigurations().getByName(SpotBugsPlugin.CONFIG_NAME);
    TextResourceFactory factory = getTask().getProject().getResources().getText();
    stylesheet.set(getTask().getProject().provider(() -> resolve(path, configuration, factory)));
  }
}
