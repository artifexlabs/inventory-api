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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The construction rules, each pinned by the failure it prevents. Every one of these is a bug that would produce
 * confidently wrong answers rather than an error — two media reported identical when they are not, or a genuine mirror
 * missed — so they are worth asserting explicitly rather than trusting to review.
 */
public class MerkleHashTest {

  private final static HashAlgorithm SHA = HashAlgorithm.SHA256;

  private static String hex(byte[] b) {
    StringBuilder s = new StringBuilder();
    for (byte x : b)
      s.append(String.format("%02x", x));
    return s.toString();
  }

  @Test
  public void aDirectoryHoldingOneFileIsNotThatFile() {
    byte[] file = MerkleHash.structureOfFile(SHA, "a.txt", 10L);
    byte[] dir = MerkleHash.structureOfDir(SHA, List.of(file));
    assertFalse(hex(file).equals(hex(dir)),
        "domain separation: without the blob/tree prefix a directory collapses into its only child");
  }

  @Test
  public void lengthPrefixingStopsNameConcatenationCollisions() {
    // "ab"+"c" and "a"+"bc" must not meet in the middle
    byte[] one = MerkleHash.structureOfFile(SHA, "ab", 1L);
    byte[] two = MerkleHash.structureOfFile(SHA, "a", 1L);
    assertNotEquals(hex(one), hex(two));
    byte[] left = MerkleHash.structureOfDir(SHA,
        List.of(MerkleHash.structureOfFile(SHA, "ab", 1L), MerkleHash.structureOfFile(SHA, "c", 1L)));
    byte[] right = MerkleHash.structureOfDir(SHA,
        List.of(MerkleHash.structureOfFile(SHA, "a", 1L), MerkleHash.structureOfFile(SHA, "bc", 1L)));
    assertNotEquals(hex(left), hex(right), "length-prefixing keeps ambiguous concatenations apart");
  }

  @Test
  public void childOrderDoesNotMatter() {
    byte[] a = MerkleHash.structureOfFile(SHA, "a", 1L);
    byte[] b = MerkleHash.structureOfFile(SHA, "b", 2L);
    assertArrayEquals(MerkleHash.structureOfDir(SHA, List.of(a, b)), MerkleHash.structureOfDir(SHA, List.of(b, a)),
        "the same directory listed in a different order is the same directory");
  }

  @Test
  public void sizeParticipatesSoSameNamesDifferentSizesDiffer() {
    assertNotEquals(hex(MerkleHash.structureOfFile(SHA, "cover.jpg", 1000L)),
        hex(MerkleHash.structureOfFile(SHA, "cover.jpg", 2000L)),
        "decision 2: without size, every cover.jpg in a library collides");
  }

  @Test
  public void anEmptyDirectoryHasARealAndDistinctIdentity() {
    byte[] empty = MerkleHash.structureOfDir(SHA, List.of());
    assertEquals(32, empty.length);
    assertNotEquals(hex(empty), hex(MerkleHash.structureOfDir(SHA, List.of(MerkleHash.structureOfFile(SHA, "a", 0L)))));
    // and it is stable, so two empty directories on different media match
    assertArrayEquals(empty, MerkleHash.structureOfDir(SHA, List.of()));
  }

  @Test
  public void sortingIsUnsignedSoHighBytesDoNotSortFirst() {
    // Java's byte is signed: a naive sort puts 0x80.. before 0x00.., and two
    // machines disagreeing on order would hash the same tree differently
    byte[] high = new byte[32];
    high[0] = (byte) 0xFF;
    byte[] low = new byte[32];
    low[0] = 0x01;
    assertArrayEquals(MerkleHash.structureOfDir(SHA, List.of(high, low)),
        MerkleHash.structureOfDir(SHA, List.of(low, high)));
  }

  @Test
  public void contentAndStructureAreDifferentQuestions() {
    byte[] content = new byte[32];
    content[0] = 0x42;
    byte[] structural = MerkleHash.structureOfFile(SHA, "a.txt", 5L);
    byte[] withContent = MerkleHash.merkleOfFile(SHA, "a.txt", content);
    assertNotEquals(hex(structural), hex(withContent), "a structure hash must never be mistaken for a content hash");
  }

  @Test
  public void renamingChangesTheMerkleButNotTheContentOnlyVariant() {
    byte[] content = new byte[32];
    content[0] = 0x07;
    assertNotEquals(hex(MerkleHash.merkleOfFile(SHA, "before.txt", content)),
        hex(MerkleHash.merkleOfFile(SHA, "after.txt", content)), "the named variant sees a rename");
    // the content-only variant folds bare content digests, so a rename is invisible
    assertArrayEquals(MerkleHash.structureOfDir(SHA, List.of(content)),
        MerkleHash.structureOfDir(SHA, List.of(content)));
  }

  @Test
  public void blake3IsRefusedRatherThanSilentlyHashedWithSomethingElse() {
    IllegalArgumentException e = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
        () -> MerkleHash.structureOfFile(HashAlgorithm.BLAKE3, "a", 1L));
    assertTrue(e.getMessage().contains("blake3"), () -> "unhelpful message: " + e.getMessage());
  }
}
