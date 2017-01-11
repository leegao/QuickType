package com.quicktype.parsing;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.quicktype.IndexingContext;
import com.quicktype.steps.Step;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class Parser {
  public static List<CompilationUnitTree> parse(int slice, int buckets, IndexingContext context) throws IOException {
    List<String> srcs = context.files;
    int length = Step.getLength(srcs.size(), buckets, slice);

    File[] files = new File[length];
    for (int i = 0; i < length; i++) {
      files[i] = new File(srcs.get(slice + buckets * i));
    }

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    StandardJavaFileManager javaFileManager =
        compiler.getStandardFileManager(null, null, null);
    Iterable<? extends JavaFileObject> sources = javaFileManager.getJavaFileObjects(files);
    final ImmutableSet<String> emptySet = ImmutableSet.of();
    JavacTask task =
        (JavacTask) compiler.getTask(null, javaFileManager, null, emptySet, emptySet, sources);

    return Lists.newArrayList(task.parse());
  }
}
