package com.quicktype.steps;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ProcessingState<T> {
  ConcurrentHashMap<StepIndex, T> cache = new ConcurrentHashMap<>();
  ConcurrentLinkedQueue<StepIndex> queue = new ConcurrentLinkedQueue<>();
  ConcurrentHashMap<StepIndex, Set<StepIndex>> retryDependencies = new ConcurrentHashMap<>();

  public void push(StepIndex index, T result) {
    cache.put(index, result);
  }

  public void resubmit(StepIndex index, Set<StepIndex> dependencies) {
    if (!retryDependencies.containsKey(index)) {
      queue.add(index);
      retryDependencies.put(index, dependencies);
    }
  }

  public Optional<T> poll(StepIndex index) {
    // cache is monotone
    return Optional.ofNullable(cache.get(index));
  }

  public Map<StepIndex, T> poll(Set<StepIndex> indices) throws RetryException {
    Set<StepIndex> needsRetry = new HashSet<>();
    Map<StepIndex, T> output = new HashMap<>();
    for (StepIndex index : indices) {
      Optional<T> result = poll(index);
      if (result.isPresent()) {
        output.put(index, result.get());
      } else {
        needsRetry.add(index);
      }
    }
    if (!needsRetry.isEmpty()) {
      throw new RetryException(needsRetry);
    }
    return output;
  }
}
