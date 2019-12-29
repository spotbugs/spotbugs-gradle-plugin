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

import com.github.spotbugs.snom.SpotBugsReportsContainer;
import com.github.spotbugs.snom.SpotBugsTask;
import java.util.Optional;
import java.util.stream.Stream;
import org.gradle.api.reporting.CustomizableHtmlReport;
import org.gradle.api.reporting.SingleFileReport;

public class DefaultSpotBugsReportsContainer implements SpotBugsReportsContainer {
  private final SpotBugsTextReport text;
  private final SpotBugsXmlReport xml;
  private final SpotBugsHtmlReport html;

  public DefaultSpotBugsReportsContainer(SpotBugsTask task) {
    text = new SpotBugsTextReport(task.getProject().getObjects(), task);
    xml = new SpotBugsXmlReport(task.getProject().getObjects(), task);
    html = new SpotBugsHtmlReport(task.getProject().getObjects(), task);
  }

  @Override
  public SingleFileReport getText() {
    return text;
  }

  @Override
  public SingleFileReport getXml() {
    return xml;
  }

  @Override
  public CustomizableHtmlReport getHtml() {
    return html;
  }

  @Override
  public Optional<AbstractSingleFileReport> getFirstEnabled() {
    return Stream.of(text, xml, html).filter(SingleFileReport::isEnabled).findFirst();
  }
}
