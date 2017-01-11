package com.quicktype;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.quicktype.parsing.Parser;
import com.quicktype.steps.Processor;
import com.quicktype.steps.Step;
import com.sun.source.tree.CompilationUnitTree;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class Index {
  public static IndexingContext index(List<String> srcs) throws ExecutionException, InterruptedException {
    int parallelism = Runtime.getRuntime().availableProcessors();
    ListeningExecutorService executor = MoreExecutors.listeningDecorator(
        Executors.newFixedThreadPool(parallelism));

    final CompilationUnitTree[] compiledTrees = new CompilationUnitTree[srcs.size()];
    IndexingContext context = new IndexingContext(compiledTrees, srcs);
    ImmutableList<Step> steps = ImmutableList.<Step>builder()
        .add(
            Step.callable(Parser::parse)
                .withName("Parsing Files")
                .splitInto(5)
                .after(context::updateASTs)
                .build())
        .build();
    Processor.process(steps, executor, context);
    executor.shutdown();
    return context;
  }
}