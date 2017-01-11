package com.quicktype.parsing;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import javax.tools.JavaCompiler;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;

public class Parser {
  public static void parse(String source) throws IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    StandardJavaFileManager javaFileManager =
        compiler.getStandardFileManager(null, null, null);
    Iterable<? extends JavaFileObject> sources = javaFileManager.getJavaFileObjects(new File(source));
    final ImmutableSet<String> emptySet = ImmutableSet.of();
    JavacTask task =
        (JavacTask) compiler.getTask(null, javaFileManager, null, emptySet, emptySet, sources);
    for (CompilationUnitTree compilationUnitTree : task.parse()) {
      System.err.println(compilationUnitTree);
    }
    ;
  }
}
