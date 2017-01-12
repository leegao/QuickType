package com.quicktype;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.quicktype.parsing.Parser;
import com.quicktype.steps.Processor;
import com.quicktype.steps.Step;
import com.quicktype.symbolize.SymbolizeClasses;
import com.sun.source.tree.CompilationUnitTree;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class Index {
  public static IndexingContext index(List<String> srcs) throws ExecutionException, InterruptedException {
    int parallelism = Runtime.getRuntime().availableProcessors();
    ListeningExecutorService executor = MoreExecutors.listeningDecorator(
        Executors.newFixedThreadPool(parallelism));

    IndexingContext context = new IndexingContext(srcs);
    ImmutableList<Step> steps = ImmutableList.<Step>builder()
        .add(
            Step.callable(Parser::parse)
                .withName("Parsing Files")
                .splitInto(256)
                .after(context::updateASTs)
                .build()
        )
        .add(Step.barrier())
        .add(
            Step.callable(SymbolizeClasses::compute)
                .withName("Symbolizing Class Symbols")
                .splitInto(256)
                .after(System.out::println)
                .build()
        )
        .add(Step.barrier())
        .build();
    Processor.process(steps, executor, context);
    executor.shutdown();
    return context;
  }
}
