package com.quicktype;

import com.sun.source.tree.CompilationUnitTree;

import java.util.List;

public class IndexingContext {
  public final CompilationUnitTree[] compiledTrees;
  public final List<String> files;

  IndexingContext(List<String> files) {
    this.compiledTrees = new CompilationUnitTree[files.size()];
    this.files = files;
  }

  void updateASTs(List<List<CompilationUnitTree>> trees) {
    for (int slice = 0; slice < trees.size(); slice++) {
      List<CompilationUnitTree> currentSlice = trees.get(slice);
      for (int i = 0; i < currentSlice.size(); i++) {
        compiledTrees[slice + trees.size() * i] = currentSlice.get(i);
      }
    }
  }
}
