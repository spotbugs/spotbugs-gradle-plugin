package com.github.spotbugs.internal.spotbugs;

import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.IFindBugsEngine;
import edu.umd.cs.findbugs.TextUICommandLine;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import javax.inject.Inject;
import org.gradle.api.GradleException;


public class SpotBugsRunner implements Runnable {
  private final SpotBugsSpec spec;
  private final boolean ignoreFailures;
  private final File report;

  @Inject
  SpotBugsRunner(SpotBugsSpec spec, boolean ignoreFailures, File report) {
    this.spec = spec;
    this.ignoreFailures = ignoreFailures;
    this.report = report;
  }

  @Override
  public void run() {
    final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      final List<String> args = spec.getArguments();
      String[] strArray = args.toArray(new String[0]);

      Thread.currentThread().setContextClassLoader(FindBugs2.class.getClassLoader());
      FindBugs2 findBugs2 = new FindBugs2();
      TextUICommandLine commandLine = new TextUICommandLine();

      FindBugs.processCommandLine(commandLine, strArray, findBugs2);
      findBugs2.execute();

      SpotBugsResult result = createSpotbugsResult(findBugs2);
      evaluateResult(result);
    } catch (IOException | InterruptedException e) {
      throw new GradleException("Error initializing SpotBugsRunner", e);
    } finally {
      Thread.currentThread().setContextClassLoader(contextClassLoader);
    }
  }

  SpotBugsResult createSpotbugsResult(IFindBugsEngine findBugs) {
    int bugCount = findBugs.getBugCount();
    int missingClassCount = findBugs.getMissingClassCount();
    int errorCount = findBugs.getErrorCount();
    return new SpotBugsResult(bugCount, missingClassCount, errorCount);
  }

  void evaluateResult(SpotBugsResult result) {
    if (result.getException() != null) {
      throw new GradleException("SpotBugs encountered an error. Run with --debug to get more information.", result.getException());
    }

    if (result.getErrorCount() > 0) {
      throw new GradleException("SpotBugs encountered an error. Run with --debug to get more information.");
    }

    if (result.getBugCount() > 0) {
      String message = "SpotBugs rule violations were found.";

      if (report != null) {
        String reportUrl = asClickableFileUrl(report);
        message += " See the report at: " + reportUrl;
      }

      if (ignoreFailures) {
        System.out.println(message);
      } else {
        throw new GradleException(message);
      }

    }
  }

  private String asClickableFileUrl(File file) {
    try {
      return new URI("file", "", file.toURI().getPath(), null, null).toString();
    } catch (URISyntaxException e) {
      throw new GradleException("Unable to parse path to destination file", e);
    }
  }

}
