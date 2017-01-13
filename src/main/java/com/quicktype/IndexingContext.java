package com.quicktype;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import org.apache.commons.collections4.trie.PatriciaTrie;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class IndexingContext {
  public final CompilationUnitTree[] compiledTrees;
  public final List<String> files;
  public final BiMap<String, ClassTree> symbols = HashBiMap.create();
  public PatriciaTrie<String> trie = new PatriciaTrie<>();

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
    splitSymbols.forEach(symbols::putAll);
    this.symbols.forEach((symbol, tree) -> trie.put(symbol.replace('$', '.'), symbol));
  }

  public Optional<String> getSymbol(String name) {
    if (trie.containsKey(name)) {
      return Optional.of(trie.get(name));
    }
    return Optional.empty();
  }

  public Map<String, String> getSubSymbols(String prefix) {
    return trie.prefixMap(prefix);
  }
}
