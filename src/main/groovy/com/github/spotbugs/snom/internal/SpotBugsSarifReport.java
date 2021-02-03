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
import java.util.Optional;
import javax.inject.Inject;
import org.gradle.api.file.RegularFile;
import org.gradle.api.model.ObjectFactory;

public class SpotBugsSarifReport extends SpotBugsReport {
  @Inject
  public SpotBugsSarifReport(ObjectFactory objects, SpotBugsTask task) {
    super(objects, task);
    // the default reportsDir is "$buildDir/reports/spotbugs/${baseName}.sarif"
    setDestination(
        task.getReportsDir().file(task.getBaseName() + ".sarif").map(RegularFile::getAsFile));
  }

  @NonNull
  @Override
  public Optional<String> toCommandLineOption() {
    return Optional.of("-sarif");
  }

  @Override
  public String getName() {
    return "SARIF";
  }
}
