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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/** The tree derived from a flat manifest — the thing that makes "is this folder a copy of that one" askable at all. */
public class DataTreeTest {

  private final static HashAlgorithm SHA = HashAlgorithm.SHA256;

  private static DataEntry file(String path, long size) {
    return new DataEntry(path, size, null, null, null, null, List.of());
  }

  private static Map<String, DataTree.Node> byPath(List<DataTree.Node> nodes) {
    return nodes.stream().collect(Collectors.toMap(DataTree.Node::path, Function.identity()));
  }

  private static DataEntry hashed(String path, long size, String seed) {
    return new DataEntry(path, size, HashAlgorithm.SHA256, digest(seed), null, null, List.of());
  }

  /** A distinct, well-formed sha256 per seed; the rollup never inspects the bytes, only folds them. */
  private static String digest(String seed) {
    String h = Integer.toHexString(seed.hashCode()).replace("-", "f");
    return h.repeat(64 / h.length() + 1).substring(0, 64);
  }

  private static Map<String, DataTree.Rollup> rolled(List<DataEntry> entries, java.util.Set<String> unreadable) {
    return DataTree.roll(SHA, entries, unreadable).stream()
        .collect(Collectors.toMap(DataTree.Rollup::path, Function.identity()));
  }

  @Test
  public void aSubtreeWithAnUnhashedFileHasNoContentIdentityYet() {
    var r = rolled(List.of(hashed("a/one.txt", 1L, "one"), file("a/two.txt", 2L)), java.util.Set.of());

    assertEquals(1, r.get("a").pendingFiles());
    assertEquals(null, r.get("a").merkleHash(),
        "half a subtree has no content identity; inventing one would give a digest that changes as the run proceeds");
    assertEquals(null, r.get("").merkleHash(), "and the root inherits it, because pending rolls all the way up");
  }

  @Test
  public void afullyHashedSubtreeGetsBothContentDigests() {
    var r = rolled(List.of(hashed("a/one.txt", 1L, "one"), hashed("a/two.txt", 2L, "two")), java.util.Set.of());

    assertEquals(0, r.get("a").pendingFiles());
    assertTrue(r.get("a").merkleHash() != null && r.get("a").merkleContentHash() != null);
    assertNotEquals(java.util.Arrays.toString(r.get("a").merkleHash()),
        java.util.Arrays.toString(r.get("a").merkleContentHash()),
        "names in or names out are different questions and must be different digests");
    assertEquals(null, r.get("a").unreadableHash(), "nothing damaged, so no damage identity at all");
  }

  @Test
  public void aRelocatedCopyMatchesByMerkleAndARenamedFileOnlyByContent() {
    var mine = rolled(List.of(hashed("photos/cat.jpg", 5L, "cat"), hashed("photos/dog.jpg", 6L, "dog")),
        java.util.Set.of());
    // the same two files, in a folder with a different name, on some other disc
    var theirs = rolled(
        List.of(hashed("backup/2019/pets/cat.jpg", 5L, "cat"), hashed("backup/2019/pets/dog.jpg", 6L, "dog")),
        java.util.Set.of());
    // the same two files again, but one of them renamed
    var renamed = rolled(List.of(hashed("stuff/kitty.jpg", 5L, "cat"), hashed("stuff/dog.jpg", 6L, "dog")),
        java.util.Set.of());

    assertArrayEquals(mine.get("photos").merkleHash(), theirs.get("backup/2019/pets").merkleHash(),
        "moved AND renamed, and still the same folder: a directory's identity is what it holds, not its label");
    assertNotEquals(java.util.Arrays.toString(mine.get("photos").merkleHash()),
        java.util.Arrays.toString(renamed.get("stuff").merkleHash()),
        "MERKLE carries file names, so renaming a file inside is a different folder");
    assertArrayEquals(mine.get("photos").merkleContentHash(), renamed.get("stuff").merkleContentHash(),
        "CONTENT drops file names, so the rename survives — the whole reason the variant exists");
  }

  @Test
  public void damageHasItsOwnIdentityBecauseAnUnreadableFileIsInvisibleToMerkle() {
    var damaged = rolled(List.of(hashed("d/a.txt", 1L, "a"), file("d/b.txt", 2L)), java.util.Set.of("d/b.txt"));
    var partial = rolled(List.of(hashed("d/a.txt", 1L, "a")), java.util.Set.of());

    assertEquals(1, damaged.get("d").unreadableFiles());
    assertEquals(0, damaged.get("d").pendingFiles(), "unreadable is an outcome; it does not keep the run open");
    assertArrayEquals(damaged.get("d").merkleHash(), partial.get("d").merkleHash(),
        "an unreadable file contributes no content, so merkle alone CANNOT tell these apart — which is why...");
    assertTrue(damaged.get("d").unreadableHash() != null && partial.get("d").unreadableHash() == null,
        "...the damage digest is the other half of the comparison");
  }

  @Test
  public void identicallyDamagedCopiesMatchAndDifferentlyDamagedOnesDoNot() {
    List<DataEntry> tree = List.of(hashed("x/keep.txt", 1L, "keep"), file("x/lost.txt", 2L), file("x/other.txt", 3L));
    var one = rolled(tree, java.util.Set.of("x/lost.txt"));
    // the same tree on another disc, damaged in the same place
    var two = rolled(List.of(hashed("elsewhere/x/keep.txt", 1L, "keep"), file("elsewhere/x/lost.txt", 2L),
        file("elsewhere/x/other.txt", 3L)), java.util.Set.of("elsewhere/x/lost.txt"));
    // and a third damaged somewhere else
    var three = rolled(tree, java.util.Set.of("x/other.txt"));

    assertArrayEquals(one.get("x").unreadableHash(), two.get("elsewhere/x").unreadableHash(),
        "identically damaged, and it survives relocation for the same reason content does");
    assertNotEquals(java.util.Arrays.toString(one.get("x").unreadableHash()),
        java.util.Arrays.toString(three.get("x").unreadableHash()),
        "different files missing is different damage, even though the counts match");
  }

  @Test
  public void damageRollsUpThroughTheDirectoriesAboveIt() {
    var r = rolled(List.of(hashed("top/mid/a.txt", 1L, "a"), file("top/mid/deep/b.txt", 2L)),
        java.util.Set.of("top/mid/deep/b.txt"));

    assertEquals(1, r.get("").unreadableFiles(), "the root knows the medium is not intact");
    assertTrue(r.get("top").unreadableHash() != null && r.get("top/mid/deep").unreadableHash() != null);
    assertTrue(r.get("top/mid").merkleHash() != null,
        "nothing is pending, so every merkle is computable even though the tree is damaged");
  }

  @Test
  public void everyImpliedDirectoryBecomesANodeIncludingTheRoot() {
    var nodes = byPath(DataTree.build(SHA, List.of(file("a/b/c.txt", 10L), file("a/d.txt", 20L))));
    assertEquals(List.of("", "a", "a/b"), nodes.keySet().stream().sorted().toList(),
        "every implied directory, the root included, and nothing invented");
  }

  @Test
  public void subtreeTotalsCountTheWholeSubtreeNotJustDirectChildren() {
    var nodes = byPath(
        DataTree.build(SHA, List.of(file("deep/a/b/c/one.txt", 100L), file("deep/a/b/c/two.txt", 200L))));
    assertEquals(2, nodes.get("").subtreeFiles(), "the root sees every file beneath it");
    assertEquals(300L, nodes.get("").subtreeBytes());
    assertEquals(2, nodes.get("deep").subtreeFiles(), "a thin deep tree is not a one-file directory");
    assertEquals(2, nodes.get("deep/a/b/c").subtreeFiles());
  }

  @Test
  public void relocatingASubtreeKeepsItsStructureHash() {
    var loose = byPath(DataTree.build(SHA, List.of(file("photos/2019/a.jpg", 10L), file("photos/2019/b.jpg", 20L))));
    var moved = byPath(DataTree.build(SHA,
        List.of(file("backup/old/photos/2019/a.jpg", 10L), file("backup/old/photos/2019/b.jpg", 20L))));
    assertArrayEquals(loose.get("photos/2019").structureHash(), moved.get("backup/old/photos/2019").structureHash(),
        "THE point of the whole design: the same folder in a different place is the same folder");
    assertNotEquals(loose.get("").structureHash().length, 0);
    assertTrue(!java.util.Arrays.equals(loose.get("").structureHash(), moved.get("").structureHash()),
        "but the MEDIA differ, because one has a backup/old wrapper the other does not");
  }

  @Test
  public void depthIsRecordedForTheSecondaryFilter() {
    var nodes = byPath(DataTree.build(SHA, List.of(file("a/b/c/d.txt", 1L))));
    assertEquals(0, nodes.get("").depth());
    assertEquals(1, nodes.get("a").depth());
    assertEquals(3, nodes.get("a/b/c").depth());
  }

  @Test
  public void anEmptyDirectoryIsCarriedAndChangesTheParent() {
    var without = byPath(DataTree.build(SHA, List.of(file("a/x.txt", 1L))));
    var with = byPath(DataTree.build(SHA, List.of(file("a/x.txt", 1L), file("a/empty/", 0L))));
    assertTrue(with.containsKey("a/empty"), "an empty directory has to survive the manifest to be hashed at all");
    assertEquals(0, with.get("a/empty").subtreeFiles());
    assertTrue(!java.util.Arrays.equals(without.get("a").structureHash(), with.get("a").structureHash()),
        "two trees differing only by an empty folder must not hash the same");
  }

  @Test
  public void identicalTreesOnDifferentMediaAgree() {
    var one = byPath(DataTree.build(SHA, List.of(file("d/a", 1L), file("d/b", 2L))));
    var two = byPath(DataTree.build(SHA, List.of(file("d/b", 2L), file("d/a", 1L))));
    assertArrayEquals(one.get("").structureHash(), two.get("").structureHash(),
        "manifest order is an artifact of the scan, never part of identity");
  }

  @Test
  public void sameNamesDifferentSizesAreDifferentTrees() {
    var small = byPath(DataTree.build(SHA, List.of(file("book/cover.jpg", 1000L))));
    var large = byPath(DataTree.build(SHA, List.of(file("book/cover.jpg", 9000L))));
    assertTrue(!java.util.Arrays.equals(small.get("book").structureHash(), large.get("book").structureHash()),
        "decision 2 in action: 10,569 identically-shaped calibre directories must not all collide");
  }
}
