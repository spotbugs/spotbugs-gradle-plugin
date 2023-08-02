package com.github.spotbugs.snom.internal;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Monitors the stdout of forked process, and report when it contains some problems reported by
 * SpotBugs core.
 */
class OutputScanner extends FilterOutputStream {
  private final StringBuilder builder = new StringBuilder();
  private boolean failedToReport = false;

  public OutputScanner(OutputStream out) {
    super(out);
  }

  boolean isFailedToReport() {
    return failedToReport;
  }

  @Override
  public void write(int b) throws IOException {
    super.write(b);
    builder.append((char)b);
    if (b == '\n') {
      String line = builder.toString();
      System.err.println("line:" + line);
      if (line.contains("Could not generate HTML output")) {
        failedToReport = true;
      }

      builder.delete(0, builder.length());
    }
  }
}
