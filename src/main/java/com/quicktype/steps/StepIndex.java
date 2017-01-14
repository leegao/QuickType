package com.quicktype.steps;

import java.util.Objects;

public class StepIndex {
  private final int slice;
  private final int index;
  private final int bucket;

  public StepIndex(int slice, int index, int bucket) {
    this.slice = slice;
    this.index = index;
    this.bucket = bucket;
  }

  public int index() {
    return slice + index * bucket;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || !(o instanceof StepIndex)) return false;
    StepIndex stepIndex = (StepIndex) o;
    return slice == stepIndex.slice &&
        index == stepIndex.index &&
        bucket == stepIndex.bucket;
  }

  @Override
  public int hashCode() {
    return Objects.hash(slice, index, bucket);
  }
}
