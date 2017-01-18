package com.quicktype.steps;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ProcessingState<T> {
  ConcurrentLinkedQueue<?> queue = new ConcurrentLinkedQueue<>();
  ConcurrentHashMap<?, Set<?>> retryDependencies = new ConcurrentHashMap<>();

  public void notify(Set<?> result) {

  }

  public void resubmit(Object key, Set<?> dependencies) {

  }
}
