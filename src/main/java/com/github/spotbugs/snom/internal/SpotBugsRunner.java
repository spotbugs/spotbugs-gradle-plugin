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

import com.github.spotbugs.snom.SpotBugsTask;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SpotBugsRunner {
  private final Logger log = LoggerFactory.getLogger(SpotBugsRunner.class);

  public abstract void run(@NonNull SpotBugsTask task);

  protected List<String> buildArguments(SpotBugsTask task) {
    List<String> args = new ArrayList<>();

    Set<File> plugins = task.getPluginJar();
    if (!plugins.isEmpty()) {
      args.add("-pluginList");
      args.add(join(plugins));
    }

    args.add("-sortByClass");
    args.add("-timestampNow");
    if (!task.getAuxClassPaths().isEmpty()) {
      args.add("-auxclasspath");
      args.add(join(task.getAuxClassPaths().getFiles()));
    }
    if (!task.getSourceDirs().isEmpty()) {
      args.add("-sourcepath");
      args.add(task.getSourceDirs().getAsPath());
    }
    if (task.getShowProgress().getOrElse(Boolean.FALSE)) {
      args.add("-progress");
    }

    task.getFirstEnabledReport()
        .ifPresent(
            report -> {
              File dir = report.getDestination().getParentFile();
              dir.mkdirs();
              report.toCommandLineOption().ifPresent(args::add);
              args.add("-outputFile");
              args.add(report.getDestination().getAbsolutePath());
            });
    if (task.getEffort().isPresent()) {
      args.add("-effort:" + task.getEffort().toString().toLowerCase());
    }
    if (task.getReportLevel().isPresent()) {
      args.add(task.getReportLevel().get().toCommandLineOption());
    }
    if (task.getVisitors().isPresent() && !task.getVisitors().get().isEmpty()) {
      args.add("-visitors");
      args.add(task.getVisitors().get().stream().collect(Collectors.joining(",")));
    }
    if (task.getOmitVisitors().isPresent() && !task.getOmitVisitors().get().isEmpty()) {
      args.add("-omitVisitors");
      args.add(task.getOmitVisitors().get().stream().collect(Collectors.joining(",")));
    }
    if (task.getIncludeFilter().isPresent() && task.getIncludeFilter().get() != null) {
      args.add("-include");
      args.add(task.getIncludeFilter().get().getAbsolutePath());
    }
    if (task.getExcludeFilter().isPresent() && task.getExcludeFilter().get() != null) {
      args.add("-exclude");
      args.add(task.getExcludeFilter().get().getAbsolutePath());
    }
    if (task.getOnlyAnalyze().isPresent() && !task.getOnlyAnalyze().get().isEmpty()) {
      args.add("-onlyAnalyze");
      args.add(task.getOnlyAnalyze().get().stream().collect(Collectors.joining(",")));
    }

    args.add("-projectName");
    args.add(task.getProjectName().get());
    args.add("-release");
    args.add(task.getRelease().get());

    // TODO get extraArguments from task
    task.getClassDirs().forEach(dir -> args.add(dir.getAbsolutePath()));

    log.debug("Arguments for SpotBugs are generated: {}", args);
    return args;
  }

  protected List<String> buildJvmArguments(SpotBugsTask task) {
    // TODO get jvmArgs from task
    return Collections.emptyList();
  }

  private String join(Collection<File> files) {
    return files.stream()
        .map(File::getAbsolutePath)
        .collect(Collectors.joining(File.pathSeparator));
  }
}
