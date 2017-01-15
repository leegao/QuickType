package com.quicktype;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import org.apache.commons.collections4.trie.PatriciaTrie;

import java.util.*;

public class IndexingContext {
  public final CompilationUnitTree[] compiledTrees;
  public final List<String> files;
  public final BiMap<String, ClassTree> symbolsToTrees = HashBiMap.create();
  public final BiMap<String, ClassNode> symbolsToNodes = HashBiMap.create();
  public final PatriciaTrie<String> trie = new PatriciaTrie<>();
  public final Multimap<String, String> ancestors = HashMultimap.create();
  public final List<String> classes;
  public final List<ClassNode> classNodes = new ArrayList<>();
  public final List<ClassTree> trees = new ArrayList<>();

  IndexingContext(List<String> files, List<String> classes) {
    this.compiledTrees = new CompilationUnitTree[files.size()];
    this.files = files;
    this.classes = classes;
  }

  void updateASTs(List<List<CompilationUnitTree>> trees) {
    for (int slice = 0; slice < trees.size(); slice++) {
      List<CompilationUnitTree> currentSlice = trees.get(slice);
      for (int i = 0; i < currentSlice.size(); i++) {
        compiledTrees[slice + trees.size() * i] = currentSlice.get(i);
      }
    }
  }

  void updateClassSymbols(List<BiMap<String, ClassTree>> splitSymbols) {
    splitSymbols.forEach(symbolsToTrees::putAll);
    symbolsToTrees.forEach((symbol, tree) -> trie.put(symbol.replace('$', '.'), symbol));
    trees.addAll(symbolsToTrees.values());
  }

  void updateAncestors(List<Multimap<String, String>> splitAncestors) {
    splitAncestors.forEach(ancestors::putAll);
  }

  void updateClassNodes(List<List<ClassNode>> splitClassNodes) {
    splitClassNodes.forEach(classNodes::addAll);
  }

  public Optional<String> getSymbol(String name) {
    if (trie.containsKey(name)) {
      return Optional.of(trie.get(name));
    }
    return Optional.empty();
  }

  public Map<String, String> getSubSymbols(String prefix) {
    Map<String, String> subSymbols = new HashMap<>();
    for (Map.Entry<String, String> entry : trie.prefixMap(prefix).entrySet()) {
      if (entry.getValue().charAt(prefix.length()) == '$') {
        subSymbols.put(entry.getKey(), entry.getValue());
      }
    }
    return subSymbols;
  }

  public void updateClassNodesSymbols(List<BiMap<String, ClassNode>> slices) {
    slices.forEach(symbolsToNodes::putAll);
    symbolsToNodes.forEach((symbol, node) -> trie.put(symbol.replace('$', '.'), symbol));
  }
}
