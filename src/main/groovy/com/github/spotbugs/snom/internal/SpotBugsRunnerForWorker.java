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
import java.util.Objects;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.provider.ListProperty;
import org.gradle.workers.ProcessWorkerSpec;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

public class SpotBugsRunnerForWorker extends SpotBugsRunner {
  private final WorkerExecutor workerExecutor;

  public SpotBugsRunnerForWorker(@NonNull WorkerExecutor workerExecutor) {
    this.workerExecutor = Objects.requireNonNull(workerExecutor);
  }

  @Override
  public void run(@NonNull SpotBugsTask task) {
    Objects.requireNonNull(task);

    WorkQueue workerQueue = workerExecutor.processIsolation(configureWorkerSpec(task));
    workerQueue.submit(SpotBugsExecutor.class, configureWorkParameters(task));
  }

  private Action<ProcessWorkerSpec> configureWorkerSpec(SpotBugsTask task) {
    return spec -> {
      spec.getClasspath().setFrom(task.getJarOnClasspath());
      spec.forkOptions(
          option -> {
            option.jvmArgs(buildJvmArguments(task));
            String maxHeapSize = task.getMaxHeapSize().getOrNull();
            if (maxHeapSize != null) {
              option.setMaxHeapSize(maxHeapSize);
            }
          });
    };
  }

  private Action<SpotBugsWorkParameters> configureWorkParameters(SpotBugsTask task) {
    return params -> params.getArguments().addAll(buildArguments(task));
  }

  interface SpotBugsWorkParameters extends WorkParameters {
    ListProperty<String> getArguments();
  }

  public abstract static class SpotBugsExecutor implements WorkAction<SpotBugsWorkParameters> {
    @Override
    public void execute() {
      SpotBugsWorkParameters params = getParameters();
      String[] args = params.getArguments().get().toArray(new String[0]);

      // TODO handle isIgnoreFailures
      try {
        edu.umd.cs.findbugs.Version.printVersion(false);
        edu.umd.cs.findbugs.FindBugs2.main(args);
      } catch (Exception e) {
        throw new GradleException("SpotBugs execution thrown exception", e);
      }
    }
  }
}
