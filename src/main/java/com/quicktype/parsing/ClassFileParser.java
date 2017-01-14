package com.quicktype.parsing;

import com.google.common.base.Throwables;
import com.quicktype.IndexingContext;
import com.quicktype.steps.ProcessingState;
import com.quicktype.steps.Step;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassFileParser {
  public static List<ClassNode> parse(
      int slice,
      int buckets,
      IndexingContext context,
      ProcessingState<List<ClassNode>> state) throws IOException {
    int length = Step.getLength(context.classes.size(), buckets, slice);
    List<ClassNode> classes = new ArrayList<>();
    for (int i = 0; i < length; i++) {
      classes.addAll(parse(context.classes.get(slice + buckets * i), context));
    }
    return classes;
  }

  private static List<ClassNode> parse(String file, IndexingContext context) {
    List<ClassNode> classes = new ArrayList<>();
    if (file.endsWith(".jar")) {
      try (JarFile jarFile = new JarFile(new File(file))) {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
          JarEntry entry = entries.nextElement();
          if (!entry.getName().endsWith(".class")) continue;
          try (InputStream stream = new BufferedInputStream(jarFile.getInputStream(entry))) {
            classes.add(parse(stream, context));
          }
        }
      } catch (IOException e) {
        throw Throwables.propagate(e);
      }
    } else {
      try (FileInputStream stream = new FileInputStream(file)) {
        classes.add(parse(stream, context));
      } catch (IOException e) {
        throw Throwables.propagate(e);
      }
    }
    return classes;
  }

  private static ClassNode parse(InputStream stream, IndexingContext context) throws IOException {
    ClassReader reader = new ClassReader(stream);
    ClassNode node = new ClassNode();
    reader.accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
    return node;
  }
}
