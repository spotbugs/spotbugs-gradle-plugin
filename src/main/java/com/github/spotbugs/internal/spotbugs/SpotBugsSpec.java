package com.github.spotbugs.internal.spotbugs;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;

public class SpotBugsSpec implements Serializable {
  private static final long serialVersionUID = 1L;
  private List<String> arguments;
  private String maxHeapSize;
  private boolean debugEnabled;
  private Collection<String> jvmArgs;
  private final Map<String, Object> systemProperties;

  public SpotBugsSpec(List<String> arguments, String maxHeapSize, boolean debugEnabled, Collection<String> jvmArgs, Map<String, Object> systemProperties) {
      this.debugEnabled = debugEnabled;
      this.maxHeapSize = maxHeapSize;
      this.arguments = arguments;
      this.jvmArgs = jvmArgs;
      this.systemProperties = systemProperties;
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

  public Map<String, Object> getSystemProperties() {
      return systemProperties;
  }

    @Override
public String toString() {
      return new ToStringBuilder(this).append("arguments", arguments).append("debugEnabled", debugEnabled).toString();
  }
}