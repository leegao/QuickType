package com.quicktype;

import com.quicktype.parsing.Parser;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
  @Option(name = "-I", metaVar = "include")
  private List<String> includes = new ArrayList<String>();

  @Argument(required = true, metaVar = "input-files")
  private List<String> inputFiles = new ArrayList<String>();

  public static void main(String[] args) throws CmdLineException, IOException {
    new Main().driver(args);
  }

  private void driver(String[] args) throws CmdLineException, IOException {
    CmdLineParser parser = new CmdLineParser(this);
    parser.parseArgument(args);
    Index.index(inputFiles);
  }
}
