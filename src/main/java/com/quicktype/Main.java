package com.quicktype;

import com.quicktype.parsing.Parser;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
    IndexingContext index = Index.index(inputFiles);
    System.err.println(index.compiledTrees[0]);
  }
}
