/*
 * Copyright 2023 SpotBugs team
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

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.jetbrains.annotations.NotNull;

/**
 * Monitors the stdout of forked process, and report when it contains some problems reported by
 * SpotBugs core.
 */
class OutputScanner extends FilterOutputStream {
  private final ByteArrayOutputStream builder = new ByteArrayOutputStream();
  private boolean failedToReport = false;

  public OutputScanner(OutputStream out) {
    super(out);
  }

  boolean isFailedToReport() {
    return failedToReport;
  }

  @Override
  public void write(byte @NotNull [] b, int off, int len) throws IOException {
    super.write(b, off, len);
    builder.write(b, off, len);
  }

  @Override
  public void write(int b) throws IOException {
    super.write(b);
    builder.write(b);

    if (b == '\n') {
      String line = builder.toString();
      if (line.contains("Could not generate HTML output")) {
        failedToReport = true;
      }

      builder.reset();
    }
  }
}
