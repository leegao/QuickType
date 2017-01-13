package com.quicktype;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.quicktype.parsing.ClassFileParser;
import com.quicktype.parsing.Parser;
import com.quicktype.steps.Processor;
import com.quicktype.steps.Step;
import com.quicktype.symbolize.SymbolizeAncestors;
import com.quicktype.symbolize.SymbolizeClasses;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class Index {
  public static IndexingContext index(List<String> srcs, List<String> classes) throws ExecutionException, InterruptedException {
    int parallelism = Runtime.getRuntime().availableProcessors();
    ListeningExecutorService executor = MoreExecutors.listeningDecorator(
        Executors.newFixedThreadPool(parallelism));

    IndexingContext context = new IndexingContext(srcs, classes);
    ImmutableList<Step> steps = ImmutableList.<Step>builder()
        .add(
            Step.callable(Parser::parse)
                .withName("Parsing Files")
                .splitInto(256)
                .after(context::updateASTs)
                .build(),
            Step.callable(ClassFileParser::parse)
                .withName("Parsing Class Files")
                .splitInto(256)
                .after(context::updateClassNodes)
                .build()
        )
        .add(Step.barrier())
        .add(
            Step.callable(SymbolizeClasses::compute)
                .withName("Symbolizing Class Symbols")
                .splitInto(256)
                .after(context::updateClassSymbols)
                .build()
        )
        .add(Step.barrier())
        .add(
            Step.callable(SymbolizeAncestors::compute)
                .withName("Symbolizing Ancestors")
                .splitInto(256)
                .after(context::updateAncestors)
                .build()
        )
        .add(Step.barrier())
        .build();
    Processor.process(steps, executor, context);
    executor.shutdown();
    return context;
  }
}
