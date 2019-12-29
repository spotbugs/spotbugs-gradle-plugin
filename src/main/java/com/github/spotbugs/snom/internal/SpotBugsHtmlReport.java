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
package com.github.spotbugs.snom.internal;

import com.github.spotbugs.snom.ReportType;
import com.github.spotbugs.snom.SpotBugsTask;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.File;
import java.util.Optional;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.reporting.CustomizableHtmlReport;
import org.gradle.api.resources.TextResource;

public class SpotBugsHtmlReport extends AbstractSingleFileReport implements CustomizableHtmlReport {
  private final Property<TextResource> stylesheet;

  public SpotBugsHtmlReport(ObjectFactory objects, SpotBugsTask task) {
    super(objects, task);
    // the default reportsDir is "$buildDir/reports/spotbugs/${taskName}/spotbugs.html"
    setDestination(task.getReportsDir().map(dir -> new File(dir, "spotbugs.html")));
    stylesheet = objects.property(TextResource.class);
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
    return ReportType.HTML.name();
  }

  @Nullable
  @Override
  public TextResource getStylesheet() {
    return stylesheet.getOrNull();
  }

  @Override
  public void setStylesheet(@Nullable TextResource textResource) {
    stylesheet.set(textResource);
  }

  public void setStylesheet(@NonNull Provider<TextResource> textResource) {
    stylesheet.set(textResource);
  }
}
