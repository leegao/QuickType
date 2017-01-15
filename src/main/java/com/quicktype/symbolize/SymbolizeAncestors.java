package com.quicktype.symbolize;

import com.google.common.collect.*;
import com.quicktype.IndexingContext;
import com.quicktype.steps.ProcessingState;
import com.quicktype.steps.RetryException;
import com.quicktype.steps.Step;
import com.quicktype.steps.StepIndex;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.tree.JCTree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

public class SymbolizeAncestors {
  public static Multimap<String, String> compute(
      int slice,
      int buckets,
      IndexingContext context,
      ProcessingState<Multimap<String, String>> state) throws IOException {
    int length = Step.getLength(context.compiledTrees.length, buckets, slice);
    Multimap<String, String> ancestors = HashMultimap.create();
    for (int i = 0; i < length; i++) {
      StepIndex index = new StepIndex(slice, i, buckets);
      try {
        Multimap<String, String> compute = computeOne(index, context, state);
        ancestors.putAll(compute);
        state.push(index, compute);
      } catch (RetryException e) {
        // Do nothing
        state.resubmit(index, e.dependencies);
      }
    }
    return ancestors;
  }

  public static Multimap<String, String> computeOne(
      StepIndex index,
      IndexingContext context,
      ProcessingState<Multimap<String, String>> state) throws RetryException {
    CompilationUnitTree compiledTree = context.compiledTrees[index.index()];
    Multimap<String, String> ancestorsSlice = HashMultimap.create();

    String packageName = compiledTree.getPackageName().toString();
    BiMap<String, String> importedSymbols = HashBiMap.create();
    importedSymbols.putAll(context.getSubSymbols(packageName));
    for (ImportTree importTree : compiledTree.getImports()) {
      String identifier = importTree.getQualifiedIdentifier().toString();
      if (identifier.endsWith("*")) {
        String prefix = identifier.substring(0, identifier.length() - 2);
        importedSymbols.putAll(context.getSubSymbols(prefix));
      } else {
        context.getSymbol(identifier).ifPresent(symbol -> importedSymbols.put(identifier, symbol));
      }
    }

    Stack<BiMap<String, String>> openedSymbols = new Stack<>();
    openedSymbols.push(importedSymbols);

    new TreePathScanner<Void, IndexingContext>() {
      @Override
      public Void visitClass(ClassTree classTree, IndexingContext context) {
        List<String> ancestors = new ArrayList<>();
        if (classTree.getExtendsClause() != null) {
          Optional<String> ancestor = resolveClass(
              getBaseType((JCTree) classTree.getExtendsClause()).toString(),
              openedSymbols);
          ancestor.ifPresent(ancestors::add);
        }
        classTree.getImplementsClause().forEach(
            implementing -> resolveClass(
                getBaseType((JCTree) implementing).toString(),
                openedSymbols).ifPresent(ancestors::add));
        if (classTree.getSimpleName().toString().isEmpty()) {
          Tree parent = getCurrentPath().getParentPath().getLeaf();
          if (parent instanceof JCTree.JCNewClass) {
            JCTree clazz = getBaseType(((JCTree.JCNewClass) parent).clazz);
            resolveClass(clazz.toString(), openedSymbols).ifPresent(ancestors::add);
          }
        }
        String currentClass = context.symbolsToTrees.inverse().get(classTree);
        if (currentClass != null) {
          ancestors.forEach(symbol -> ancestorsSlice.put(currentClass, symbol));
        }
        // Get the ancestors of the ancestors too
        for (String ancestor : ancestors) {
          System.err.println(ancestor);
        }

        BiMap<String, String> current = HashBiMap.create();
        ancestors.forEach(symbol -> current.put(symbol.replace('$', '.'), symbol));
        openedSymbols.push(current);
        super.visitClass(classTree, context);
        openedSymbols.pop();
        return null;
      }
    }.scan(compiledTree, context);
    return ancestorsSlice;
  }

  private static JCTree getBaseType(JCTree clazz) {
    if (clazz instanceof JCTree.JCTypeApply) {
      clazz = ((JCTree.JCTypeApply) clazz).getType();
    }
    return clazz;
  }

  private static Optional<String> resolveClass(String symbol, Stack<BiMap<String, String>> openedSymbols) {
    for (BiMap<String, String> m : Lists.reverse(openedSymbols)) {
      if (m.containsKey(symbol)) {
        return Optional.of(m.get(symbol));
      }
      for (String key : m.keySet()) {
        if (key.endsWith(symbol)) {
          return Optional.of(m.get(key));
        }
      }
    }
    return Optional.empty();
  }
}
