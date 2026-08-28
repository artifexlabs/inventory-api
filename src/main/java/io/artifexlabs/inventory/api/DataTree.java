/*
 * @formatter:off
 * Copyright © 2019 admin (admin@artifexlabs.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * @formatter:on
 */
package io.artifexlabs.inventory.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Derives a medium's directory tree from its flat manifest.
 *
 * <p>
 * A manifest is a list of FILES. Directories exist in it only as shared prefixes of path strings, which is precisely
 * why a relocated duplicate subtree used to be undetectable: there was no entity to compare. This turns those implicit
 * prefixes into {@link Node}s carrying a structure hash, so "is this folder a copy of that one" becomes an equality
 * check on an indexed column.
 *
 * <p>
 * Both backends derive their tree here rather than each walking paths their own way. The InMemory twin exists to prove
 * the Postgres one; a tree built by two different loops would make the twins agree by luck.
 *
 * <p>
 * <b>The root is a node.</b> It has path {@code ""} and depth 0, and its structure hash is the MEDIUM's structural
 * identity — the thing "are these two discs the same?" ultimately compares.
 */
public final class DataTree {

  /**
   * One directory. {@code subtreeFiles} and {@code subtreeBytes} are totals over the whole subtree, not just direct
   * children, because they exist to serve the size floor — and a floor that counted only direct children would let a
   * deep, thin, thousand-file tree look like a one-file directory.
   */
  public record Node(String path, int depth, byte[] structureHash, long subtreeFiles, long subtreeBytes) {
  }

  /**
   * What a directory's CONTENT says about it, once bytes have been read. The structural half ({@link Node}) exists the
   * moment a manifest lands; this half arrives weeks later, one hashed file at a time, which is why it is a separate
   * pass over the same tree rather than more fields on the same record.
   *
   * <p>
   * {@code merkleHash} and {@code merkleContentHash} are null while {@code pendingFiles} is above zero: a subtree whose
   * bytes are half read has no content identity yet, and inventing one from the half that finished would be a digest
   * that changes meaning as the run proceeds.
   *
   * <p>
   * {@code unreadableHash} is null when nothing below is damaged, so "identically damaged" is one column compare and
   * "damaged at all" is a null check.
   */
  public record Rollup(String path, byte[] merkleHash, byte[] merkleContentHash, byte[] unreadableHash,
      long pendingFiles, long unreadableFiles) {
  }

  private DataTree() {
  }

  /**
   * Build every directory implied by these entries, deepest first in the returned list so a caller can insert children
   * before parents if it wants to.
   *
   * <p>
   * A path ending in {@code /} is taken as an EMPTY DIRECTORY rather than a file. That is how the {@code find}-shaped
   * ingest expresses one, and it matters: a manifest that cannot mention empty directories makes two trees differing
   * only by an empty folder hash identically.
   */
  public static List<Node> build(HashAlgorithm algorithm, List<DataEntry> entries) {
    // path -> the child hashes accumulated for it so far
    Map<String, List<byte[]>> children = new HashMap<>();
    Map<String, long[]> totals = new HashMap<>(); // path -> {files, bytes}
    children.put("", new ArrayList<>());
    totals.put("", new long[2]);

    for (DataEntry entry : entries) {
      String path = entry.path();
      boolean emptyDir = path.endsWith("/");
      if (emptyDir) {
        ensureDirs(children, totals, path.substring(0, path.length() - 1));
        continue;
      }
      int cut = path.lastIndexOf('/');
      String parent = cut < 0 ? "" : path.substring(0, cut);
      String name = cut < 0 ? path : path.substring(cut + 1);
      ensureDirs(children, totals, parent);
      children.get(parent).add(MerkleHash.structureOfFile(algorithm, name, entry.sizeBytes()));
      // a file's size counts toward every directory above it, not just its own
      for (String dir : selfAndAncestors(parent)) {
        long[] t = totals.get(dir);
        t[0]++;
        t[1] += entry.sizeBytes();
      }
    }

    // deepest first: a parent's hash needs its children's, so children must fold
    // before the parent is folded
    List<String> order = new ArrayList<>(children.keySet());
    order.sort(Comparator.comparingInt(DataTree::depthOf).reversed().thenComparing(Comparator.naturalOrder()));

    Map<String, Node> built = new LinkedHashMap<>();
    for (String dir : order) {
      byte[] hash = MerkleHash.structureOfDir(algorithm, children.get(dir));
      long[] t = totals.get(dir);
      built.put(dir, new Node(dir, depthOf(dir), hash, t[0], t[1]));
      if (!dir.isEmpty()) {
        int cut = dir.lastIndexOf('/');
        String parent = cut < 0 ? "" : dir.substring(0, cut);
        children.get(parent).add(hash);
      }
    }
    return List.copyOf(built.values());
  }

  /**
   * The CONTENT pass over the same tree: Merkle identity, its name-independent variant, and the shape of whatever could
   * not be read.
   *
   * <p>
   * Deliberately a pure function of the manifest plus the damage set, computed in the api where both backends call the
   * same code. A rollup recomputed from ground truth is also why decision 1 could reject an incremental per-file
   * counter: at 50M files that counter costs 91x the writes and makes the root a row every hasher contends on, and
   * crash recovery here is free because nothing is remembered between runs.
   *
   * @param unreadable manifest paths the worker could not read; they contribute to {@code unreadableHash} and to
   *                   nothing else, because a file with no bytes has no content to fold
   */
  public static List<Rollup> roll(HashAlgorithm algorithm, List<DataEntry> entries, Set<String> unreadable) {
    Set<String> damaged = unreadable == null ? Set.of() : unreadable;
    Map<String, List<byte[]>> merkle = new HashMap<>();
    Map<String, List<byte[]>> content = new HashMap<>();
    Map<String, List<byte[]>> damage = new HashMap<>();
    Map<String, long[]> counts = new HashMap<>(); // path -> {pending, unreadable}
    seed(merkle, content, damage, counts, "");

    for (DataEntry entry : entries) {
      String path = entry.path();
      if (path.endsWith("/")) {
        // an empty directory has no bytes, so it is neither pending nor damaged
        for (String d : selfAndAncestors(path.substring(0, path.length() - 1)))
          seed(merkle, content, damage, counts, d);
        continue;
      }
      int cut = path.lastIndexOf('/');
      String parent = cut < 0 ? "" : path.substring(0, cut);
      String name = cut < 0 ? path : path.substring(cut + 1);
      for (String d : selfAndAncestors(parent))
        seed(merkle, content, damage, counts, d);
      if (damaged.contains(path)) {
        for (String d : selfAndAncestors(parent))
          counts.get(d)[1]++;
        damage.get(parent).add(MerkleHash.damageOfFile(algorithm, name));
        continue;
      }
      if (entry.hash() == null) {
        // not yet read: it makes every directory above it uncomputable, which is
        // exactly the property that lets a rollup run at any moment
        for (String d : selfAndAncestors(parent))
          counts.get(d)[0]++;
        continue;
      }
      byte[] bytes = decode(entry.hash());
      merkle.get(parent).add(MerkleHash.merkleOfFile(algorithm, name, bytes));
      content.get(parent).add(bytes);
    }

    List<String> order = new ArrayList<>(counts.keySet());
    order.sort(Comparator.comparingInt(DataTree::depthOf).reversed().thenComparing(Comparator.naturalOrder()));

    Map<String, Rollup> built = new LinkedHashMap<>();
    for (String dir : order) {
      long[] c = counts.get(dir);
      boolean computable = c[0] == 0;
      byte[] m = computable ? MerkleHash.structureOfDir(algorithm, merkle.get(dir)) : null;
      byte[] o = computable ? MerkleHash.structureOfDir(algorithm, content.get(dir)) : null;
      byte[] u = damage.get(dir).isEmpty() ? null : MerkleHash.structureOfDir(algorithm, damage.get(dir));
      built.put(dir, new Rollup(dir, m, o, u, c[0], c[1]));
      if (dir.isEmpty())
        continue;
      int cut = dir.lastIndexOf('/');
      String parent = cut < 0 ? "" : dir.substring(0, cut);
      // a subdirectory contributes its digest unqualified by its own name — see
      // MerkleHash: a folder's identity is what it holds, not what it is called
      if (m != null) {
        merkle.get(parent).add(m);
        content.get(parent).add(o);
      }
      if (u != null)
        damage.get(parent).add(u);
    }
    return List.copyOf(built.values());
  }

  private static void seed(Map<String, List<byte[]>> merkle, Map<String, List<byte[]>> content,
      Map<String, List<byte[]>> damage, Map<String, long[]> counts, String dir) {
    merkle.computeIfAbsent(dir, k -> new ArrayList<>());
    content.computeIfAbsent(dir, k -> new ArrayList<>());
    damage.computeIfAbsent(dir, k -> new ArrayList<>());
    counts.computeIfAbsent(dir, k -> new long[2]);
  }

  /** Hex digest to bytes. DataEntry already validated the shape, so anything else here is a bug, not input. */
  private static byte[] decode(String hex) {
    byte[] out = new byte[hex.length() / 2];
    for (int i = 0; i < out.length; i++)
      out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
    return out;
  }

  /** Create this directory and every ancestor, so the tree has no holes even when only deep files were listed. */
  private static void ensureDirs(Map<String, List<byte[]>> children, Map<String, long[]> totals, String dir) {
    for (String d : selfAndAncestors(dir)) {
      children.computeIfAbsent(d, k -> new ArrayList<>());
      totals.computeIfAbsent(d, k -> new long[2]);
    }
  }

  /** {@code a/b/c} → {@code [a/b/c, a/b, a, ""]}. The root is always included. */
  private static List<String> selfAndAncestors(String dir) {
    List<String> out = new ArrayList<>();
    String cur = dir;
    while (!cur.isEmpty()) {
      out.add(cur);
      int cut = cur.lastIndexOf('/');
      cur = cut < 0 ? "" : cur.substring(0, cut);
    }
    out.add("");
    return out;
  }

  /** Root is 0, {@code a} is 1, {@code a/b} is 2. */
  private static int depthOf(String dir) {
    if (dir.isEmpty())
      return 0;
    int n = 1;
    for (int i = 0; i < dir.length(); i++)
      if (dir.charAt(i) == '/')
        n++;
    return n;
  }
}
