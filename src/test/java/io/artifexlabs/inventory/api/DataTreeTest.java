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
