package com.quicktype.steps;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.quicktype.IndexingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Processor {
  public static void process(List<Step> steps, ListeningExecutorService executor, IndexingContext context)
      throws ExecutionException, InterruptedException {
    // Process the actual futures
    List<ListenableFuture<?>> previousFutures = new ArrayList<>();
    for (Step step : steps) {
      step.step(previousFutures, executor, context);
    }
    Futures.allAsList(previousFutures).get();
  }
}
