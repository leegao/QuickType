package com.quicktype.symbolize;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.quicktype.IndexingContext;
import com.quicktype.steps.Step;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Name;

import java.io.IOException;
import java.util.*;

public class SymbolizeClasses {
  public static Set<String> compute(int slice, int buckets, IndexingContext context) throws IOException {
    int length = Step.getLength(context.compiledTrees.length, buckets, slice);
    Set<String> names = new HashSet<>();
    for (int i = 0; i < length; i++) {
      names.addAll(compute(context.compiledTrees[slice + buckets * i], context));
    }
    return names;
  }

  private static Set<String> compute(CompilationUnitTree tree, IndexingContext context) {
    Set<String> output = new HashSet<>();
    Stack<Integer> anonymous = new Stack<>();
    anonymous.push(-1);
    new TreePathScanner<Void, IndexingContext>() {
      @Override
      public Void visitNewClass(NewClassTree newClassTree, IndexingContext context) {
        Integer top = anonymous.pop();
        anonymous.push(top + 1);
        anonymous.push(-1);
        Void aVoid = super.visitNewClass(newClassTree, context);
        anonymous.pop();
        return aVoid;
      }

      @Override
      public Void visitClass(ClassTree classTree, IndexingContext context) {
        output.add(Joiner.on('$').join(name(getCurrentPath(), anonymous)));
        return super.visitClass(classTree, context);
      }
    }.scan(tree, context);

    return output;
  }

  private static List<String> name(TreePath treePath, Stack<Integer> anonymous) {
    Stack<Integer> stack = new Stack<>();
    stack.addAll(anonymous);
    stack.pop();
    List<String> name = new ArrayList<>();
    ArrayList<Tree> path = Lists.newArrayList(treePath.iterator());
    for (Tree tree : path) {
      if (tree instanceof JCTree.JCCompilationUnit) {
        name.add(0, ((JCTree.JCCompilationUnit) tree).getPackageName().toString());
      } else if (tree instanceof JCTree.JCClassDecl) {
        Name simpleName = ((JCTree.JCClassDecl) tree).getSimpleName();
        if (simpleName.isEmpty()) {
          name.add(0, String.format("%d", stack.pop()));
        } else {
          name.add(0, simpleName.toString());
        }
      }
    }
    return name;
  }
}
