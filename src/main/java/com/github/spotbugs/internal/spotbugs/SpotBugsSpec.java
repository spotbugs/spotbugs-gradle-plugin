package com.github.spotbugs.internal.spotbugs;

import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public class SpotBugsSpec implements Serializable {
  private static final long serialVersionUID = 1L;
  private List<String> arguments;
  private String maxHeapSize;
  private boolean debugEnabled;
  private Collection<String> jvmArgs;

  public SpotBugsSpec(List<String> arguments, String maxHeapSize, boolean debugEnabled, Collection<String> jvmArgs) {
      this.debugEnabled = debugEnabled;
      this.maxHeapSize = maxHeapSize;
      this.arguments = arguments;
      this.jvmArgs = jvmArgs;
  }

  public List<String> getArguments() {
      return arguments;
  }

  public String getMaxHeapSize() {
      return maxHeapSize;
  }

  public boolean isDebugEnabled() {
      return debugEnabled;
  }

  public Collection<String> getJvmArgs() {
      return jvmArgs;
  }

    @Override
public String toString() {
      return new ToStringBuilder(this).append("arguments", arguments).append("debugEnabled", debugEnabled).toString();
  }
}