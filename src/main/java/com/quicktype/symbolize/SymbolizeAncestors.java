package com.quicktype.symbolize;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.quicktype.IndexingContext;
import com.quicktype.steps.Step;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePathScanner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SymbolizeAncestors {
  public static Multimap<String, String> compute(int slice, int buckets, IndexingContext context) throws IOException {
    int length = Step.getLength(context.compiledTrees.length, buckets, slice);
    Multimap<String, String> ancestors = HashMultimap.create();
    for (int i = 0; i < length; i++) {
      ancestors.putAll(compute(context.compiledTrees[slice + buckets * i], context));
    }
    return ancestors;
  }

  private static Multimap<String, String> compute(CompilationUnitTree compiledTree, IndexingContext context) {
    Multimap<String, String> ancestors = HashMultimap.create();
    String packageName = compiledTree.getPackageName().toString();
//    compiledTree.getImports().forEach(importTree -> System.err.println(importTree.getQualifiedIdentifier()));
    new TreePathScanner<Void, IndexingContext>() {
      @Override
      public Void visitClass(ClassTree classTree, IndexingContext context) {
        return super.visitClass(classTree, context);
      }
    }.scan(compiledTree, context);
    return ancestors;
  }
}
