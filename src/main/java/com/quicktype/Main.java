package com.quicktype;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class Main {
  @Option(name = "-I", metaVar = "include")
  private List<String> includes = new ArrayList<String>();

  @Argument(required = true, metaVar = "input-files")
  private List<String> inputFiles = new ArrayList<String>();

  public static void main(String[] args) throws CmdLineException, IOException, ExecutionException, InterruptedException {
    new Main().driver(args);
  }

  private void driver(String[] args) throws CmdLineException, IOException, ExecutionException, InterruptedException {
    CmdLineParser parser = new CmdLineParser(this);
    parser.parseArgument(args);
    List<String> inputs = new ArrayList<>();
    for (String file : inputFiles) {
      List<String> files = Files.find(Paths.get(file), 999, (p, bfa) -> true)
          .map(path -> path.toAbsolutePath().toFile().toString())
          .filter(path -> path.endsWith(".java"))
          .collect(Collectors.toList());
      inputs.addAll(files);
    }
    IndexingContext index = Index.index(inputs);
    System.err.println(index.compiledTrees[3]);
  }
}
