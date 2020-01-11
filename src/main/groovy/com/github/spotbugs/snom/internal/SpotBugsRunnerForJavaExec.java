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

import com.github.spotbugs.snom.SpotBugsTask;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.process.JavaExecSpec;
import org.gradle.process.internal.ExecException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpotBugsRunnerForJavaExec extends SpotBugsRunner {
  private final Logger log = LoggerFactory.getLogger(SpotBugsRunnerForJavaExec.class);

  @Override
  public void run(@NonNull SpotBugsTask task) {
    // TODO print version of SpotBugs and Plugins
    try {
      task.getProject().javaexec(configureJavaExec(task)).rethrowFailure();
    } catch (ExecException e) {
      if (task.getIgnoreFailures()) {
        log.warn("SpotBugs reported failures", e);
      } else {
        throw new GradleException("SpotBugs execution thrown exception", e);
      }
    }
  }

  private Action<? super JavaExecSpec> configureJavaExec(SpotBugsTask task) {
    return spec -> {
      spec.classpath(task.getSpotbugsClasspath());
      spec.setJvmArgs(buildJvmArguments(task));
      spec.setMain("edu.umd.cs.findbugs.FindBugs2");
      spec.setArgs(buildArguments(task));
      String maxHeapSize = task.getMaxHeapSize().getOrNull();
      if (maxHeapSize != null) {
        spec.setMaxHeapSize(maxHeapSize);
      }
    };
  }
}
