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
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.TextUICommandLine;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.workers.ProcessWorkerSpec;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
      spec.getClasspath().setFrom(task.getSpotbugsClasspath());
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
    return params -> {
      params.getArguments().addAll(buildArguments(task));
      params.getIgnoreFailures().set(task.getIgnoreFailures());
      params.getShowStackTraces().set(task.getShowStackTraces());
    };
  }

  interface SpotBugsWorkParameters extends WorkParameters {
    ListProperty<String> getArguments();

    Property<Boolean> getIgnoreFailures();

    Property<Boolean> getShowStackTraces();
  }

  public abstract static class SpotBugsExecutor implements WorkAction<SpotBugsWorkParameters> {
    private final Logger log = LoggerFactory.getLogger(SpotBugsExecutor.class);

    @Override
    public void execute() {
      SpotBugsWorkParameters params = getParameters();
      String[] args = params.getArguments().get().toArray(new String[0]);
      DetectorFactoryCollection.resetInstance(new DetectorFactoryCollection());

      try {
        edu.umd.cs.findbugs.Version.printVersion(false);
        try (FindBugs2 findBugs2 = new FindBugs2()) {
          TextUICommandLine commandLine = new TextUICommandLine();
          FindBugs.processCommandLine(commandLine, args, findBugs2);
          findBugs2.execute();

          StringBuilder message = new StringBuilder();
          if (findBugs2.getErrorCount() > 0) {
            message.append(findBugs2.getErrorCount()).append(" SpotBugs errors were found.");
          }
          if (findBugs2.getBugCount() > 0) {
            if (message.length() > 0) {
              message.append(' ');
            }
            message.append(findBugs2.getBugCount()).append(" SpotBugs violations were found.");
          }
          if (message.length() > 0) {
            String reportPath = findReportPath();
            if (reportPath != null) {
              message.append(" See the report at: ").append(Paths.get(reportPath).toUri());
            }

            GradleException e = new GradleException(message.toString());

            if (params.getIgnoreFailures().getOrElse(Boolean.FALSE).booleanValue()) {
              log.warn(message.toString());
              if (params.getShowStackTraces().getOrElse(Boolean.TRUE).booleanValue()) {
                log.warn("", e);
              }
            } else {
              throw e;
            }
          }
        }
      } catch (GradleException e) {
        throw e;
      } catch (Exception e) {
        throw new GradleException("Verification failed: SpotBugs execution thrown exception", e);
      }
    }

    @CheckForNull
    private String findReportPath() {
      List<String> arguments = getParameters().getArguments().get();
      int outputFileParameterIndex = arguments.indexOf("-outputFile");
      if (outputFileParameterIndex > 0) {
        return arguments.get(outputFileParameterIndex + 1);
      } else {
        return null;
      }
    }
  }
}
