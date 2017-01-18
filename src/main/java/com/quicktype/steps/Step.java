package com.quicktype.steps;

import com.google.common.base.Function;
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
    return new Builder<>(transformer);
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
    T call(int slice, int bucket, IndexingContext context, ProcessingState<T> state) throws IOException;
  }

  public interface SingleTransformer<T> {
    T call(Object value, IndexingContext context, ProcessingState<T> state) throws IOException, RetryException;
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
    private Optional<SingleTransformer<T>> singleTransformer = Optional.empty();
    private String name = "";
    private int buckets = 256;
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

    public Builder<T> singleTransformer(SingleTransformer<T> singleTransformer) {
      this.singleTransformer = Optional.of(singleTransformer);
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
          ProcessingState<T> state = new ProcessingState<>();
          Function<List<T>, List<T>> reduceQueue = new Function<List<T>, List<T>>() {
            @Override
            public List<T> apply(List<T> result) {
              if (!state.queue.isEmpty()) {
                result = new ArrayList<T>(result);
                List<ListenableFuture<T>> futures = new ArrayList<>();
                List<?> stepIndices = new ArrayList<>(state.queue);
                state.queue.clear();
                state.retryDependencies.clear();
                for (Object index : stepIndices) {
                  ListenableFuture<T> future = executor.submit(() -> singleTransformer.get().call(index, context, state));
                  Futures.addCallback(future, callback, MoreExecutors.sameThreadExecutor());
                  futures.add(future);
                }
                ListenableFuture<List<T>> input = Futures.allAsList(futures);
                try {
                  List<T> subresults = Futures.<List<T>, List<T>>transform(input, this, executor).get();
                  result.addAll(subresults);
                } catch (InterruptedException | ExecutionException e) {
                  throw Throwables.propagate(e);
                }
              }
              return result;
            }
          };
          ListenableFuture<?> future = Futures.transform(
              Futures.allAsList(
                  distributeAndSubmitJobs(
                      transformer,
                      buckets,
                      ImmutableList.of(),
                      callback,
                      context,
                      state,
                      executor)),
              result -> {
                List<T> extras = reduceQueue.apply(new ArrayList<T>());
                final List<T> newResult = new ArrayList<>(result);
                newResult.addAll(extras);
                postProccessor.ifPresent(listConsumer -> listConsumer.accept(newResult));
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
      ProcessingState<T> state,
      ListeningExecutorService executorService) {
    List<ListenableFuture<T>> futures = new ArrayList<>();
    for (int i = 0; i < numberOfSubtasks; i++) {
      final int slice = i;
      ListenableFuture<T> future = executorService.submit(() -> job.call(slice, numberOfSubtasks, context, state));
      Futures.addCallback(future, callback, MoreExecutors.sameThreadExecutor());
      futures.add(future);
    }
    return futures;
  }
}
