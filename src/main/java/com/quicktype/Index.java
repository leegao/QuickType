package com.quicktype;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.quicktype.parsing.Parser;
import com.quicktype.steps.Processor;
import com.quicktype.steps.Step;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;

public class Index {
  public static void index(List<String> srcs) {
    int parallelism = Runtime.getRuntime().availableProcessors();
    ListeningExecutorService executor = MoreExecutors.listeningDecorator(
        Executors.newFixedThreadPool(parallelism));

    final CompilationUnitTree[] compiledTrees = new CompilationUnitTree[srcs.size()];

    ImmutableList<Step> steps = ImmutableList.<Step>builder()
        .add(
            Step.callable(Parser::parse)
            .withName("")
            .splitInto(5)
            .build())
        .build();
    Processor.process(steps, executor);
    executor.shutdown();
  }
}
