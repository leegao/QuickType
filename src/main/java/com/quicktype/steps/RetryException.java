package com.quicktype.steps;

import java.util.Set;

public class RetryException extends Exception {
  public final Set<StepIndex> dependencies;

  public RetryException(Set<StepIndex> dependencies) {
    this.dependencies = dependencies;
  }
}
