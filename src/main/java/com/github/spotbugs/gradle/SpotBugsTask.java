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

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.reporting.Reporting;
import org.gradle.api.tasks.SourceTask;
import org.gradle.util.ConfigureUtil;

public class SpotBugsTask extends SourceTask implements Reporting<SpotBugsReports> {

  private SpotBugsReports reports;

  @Override
  public SpotBugsReports getReports() {
    return reports;
  }

  @Override
  public SpotBugsReports reports(@SuppressWarnings("rawtypes") Closure closure) {
    return reports(ConfigureUtil.configureUsing(closure));
  }

  @Override
  public SpotBugsReports reports(Action<? super SpotBugsReports> configureAction) {
    configureAction.execute(reports);
    return reports;
  }
}
