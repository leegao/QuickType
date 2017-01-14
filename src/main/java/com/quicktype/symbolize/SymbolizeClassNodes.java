package com.quicktype.symbolize;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.quicktype.IndexingContext;
import com.quicktype.steps.Step;
import jdk.internal.org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.List;

public class SymbolizeClassNodes {
  public static BiMap<String, ClassNode> compute(int slice, int buckets, IndexingContext context) throws IOException {
    int length = Step.getLength(context.classNodes.size(), buckets, slice);
    BiMap<String, ClassNode> names = HashBiMap.create();
    for (int i = 0; i < length; i++) {
      names.putAll(compute(context.classNodes.get(slice + buckets * i), context));
    }
    return names;
  }

  private static BiMap<String, ClassNode> compute(ClassNode node, IndexingContext context) {
    BiMap<String, ClassNode> output = HashBiMap.create();
    List<String> names = Splitter.on("/").splitToList(node.name);
    String name;
    if (names.size() <= 1) {
      name = node.name;
    } else {
      name = Joiner.on(".").join(names.subList(0, names.size() - 1)) + "$" + names.get(names.size() - 1);
    }
    output.put(name, node);
    return output;
  }

}
