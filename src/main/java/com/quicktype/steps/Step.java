package com.quicktype.steps;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.*;
import com.quicktype.IndexingContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observer;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public abstract class Step {

  public abstract void step(List<ListenableFuture<?>> previousSteps, ListeningExecutorService executor, IndexingContext context)
      throws ExecutionException, InterruptedException;

  public static <T> Builder<T> callable(Transformer<T> transformer) {
    return new Builder<T>(transformer);
  }

  public static Step barrier() {
    return new Step() {
      @Override
      public void step(
          List<ListenableFuture<?>> previousSteps,
          ListeningExecutorService executor,
          IndexingContext context) throws ExecutionException, InterruptedException {
        Futures.allAsList(previousSteps).get();
      }
    };
  }

  public interface Transformer<T> {
    T call(int slice, int bucket, IndexingContext context) throws IOException;
  }

  public static int getLength(int size, int mod, int flag) {
    int quotient = size / mod;
    int remainder = size % mod;
    if (flag < remainder) {
      return quotient + 1;
    }
    return quotient;
  }

  public static class Builder<T> {
    private final Transformer<T> transformer;
    private String name = "";
    private int buckets = 1;
    private Optional<Consumer<List<T>>> postProccessor = Optional.empty();
    private FutureCallback<T> callback = new FutureCallback<T>() {
        @Override
        public void onSuccess(T result) {

        }

        @Override
        public void onFailure(Throwable t) {
          throw Throwables.propagate(t);
        }
      };

    Builder(Transformer<T> transformer) {
      this.transformer = transformer;
    }

    public Builder<T> withName(String name) {
      this.name = name;
      return this;
    }

    public Builder<T> splitInto(int buckets) {
      this.buckets = buckets;
      return this;
    }

    public Builder<T> after(Consumer<List<T>> results) {
      this.postProccessor = Optional.of(results);
      return this;
    }

    public Step build() {
      return new Step() {
        @Override
        public void step(List<ListenableFuture<?>> previousSteps, ListeningExecutorService executor, IndexingContext context)
            throws ExecutionException, InterruptedException {
          ListenableFuture<?> future = Futures.transform(
              Futures.allAsList(
                  distributeAndSubmitJobs(
                      transformer,
                      buckets,
                      ImmutableList.of(),
                      callback,
                      context,
                      executor)),
              result -> {
                postProccessor.ifPresent(listConsumer -> listConsumer.accept(result));
                return result;
              },
              executor);
          previousSteps.add(future);
        }
      };
    }
  }

  /**
   * Takes a job and splits it into subparts and submits them to an executor.
   * @return A list of futures representing the subparts of this job.
   */
  private static <T> List<ListenableFuture<T>> distributeAndSubmitJobs(
      final Transformer<T> job,
      int numberOfSubtasks,
      final Iterable<? extends Observer> observers,
      FutureCallback<T> callback,
      IndexingContext context,
      ListeningExecutorService executorService) {
    List<ListenableFuture<T>> futures = new ArrayList<>();
    for (int i = 0; i < numberOfSubtasks; i++) {
      final int slice = i;
      ListenableFuture<T> future = executorService.submit(() -> job.call(slice, numberOfSubtasks, context));
      Futures.addCallback(future, callback, MoreExecutors.sameThreadExecutor());
      futures.add(future);
    }
    return futures;
  }
}
