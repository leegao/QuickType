package com.quicktype;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import org.apache.commons.collections4.trie.PatriciaTrie;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class IndexingContext {
  public final CompilationUnitTree[] compiledTrees;
  public final List<String> files;
  public final BiMap<String, ClassTree> symbolsToTrees = HashBiMap.create();
  public final PatriciaTrie<String> trie = new PatriciaTrie<>();
  public final Multimap<String, String> ancestors = HashMultimap.create();

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

  void updateClassSymbols(List<BiMap<String, ClassTree>> splitSymbols) {
    splitSymbols.forEach(symbolsToTrees::putAll);
    symbolsToTrees.forEach((symbol, tree) -> trie.put(symbol.replace('$', '.'), symbol));
  }

  void updateAncestors(List<Multimap<String, String>> splitAncestors) {
    splitAncestors.forEach(ancestors::putAll);
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
}
