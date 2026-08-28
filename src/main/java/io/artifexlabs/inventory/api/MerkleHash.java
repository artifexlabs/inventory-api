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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Merkle identity for a data medium's tree. Lives in the api, not in a backend, because two backends computing "the
 * same" hash slightly differently is a divergence no test would catch until two media that ARE mirrors failed to match
 * — and by then the hashes are in a database.
 *
 * <p>
 * <b>Three digests, differing in when they can exist.</b> {@link #structureOfFile} needs only a manifest, so a whole
 * tree can be identified the moment {@code find} finishes; the other two need every byte read, which for a real medium
 * is weeks of work. Ship the structural answer first and let the content answer upgrade it.
 *
 * <pre>
 *   structure(f) = H("blob" ‖ len(name)‖name ‖ size)          -- at ingest, no reads
 *   structure(D) = H("tree" ‖ sorted[structure(c)])
 *
 *   merkle(f)    = H("blob" ‖ len(name)‖name ‖ content(f))    -- needs the file read
 *   merkle(D)    = H("tree" ‖ sorted[merkle(c)])
 *
 *   contentOnly(f) = content(f)                               -- rename-surviving
 *   contentOnly(D) = H("tree" ‖ sorted[contentOnly(c)])
 *
 *   damage(f)    = H("gone" ‖ len(name)‖name)                  -- a file that could not be read
 *   damage(D)    = H("tree" ‖ sorted[damage(c)])               -- null when nothing below is damaged
 * </pre>
 *
 * <p>
 * <b>A directory's own NAME is in none of them.</b> Only file names are. That is deliberate and it is what makes a
 * folder that was moved AND renamed still match its twin — the identity of a directory is what it holds, not what it is
 * called. {@code Match.CONTENT} then goes one step further and drops file names too.
 *
 * <p>
 * <b>Why damage has its own digest.</b> An unreadable file is simply absent from {@code merkle(D)} — it has no content
 * to contribute. So a damaged directory and an intact one holding only the readable half hash identically, and
 * comparing merkles alone would call them copies. The damage digest is the other half of that comparison: equal merkles
 * AND equal damage means genuinely the same, equal merkles and different damage means one of them is missing something
 * the other has. That makes partial equality sound BY CONSTRUCTION rather than by every query remembering to check a
 * flag.
 *
 * <p>
 * <b>Why each rule is here.</b> Get any of them wrong and the hashes are confidently useless:
 * <ul>
 * <li><b>Domain separation</b> ({@code "blob"} / {@code "tree"}, as git does it): without it a directory holding one
 * file hashes to that file's own digest, so a file and the folder containing it become indistinguishable.
 * <li><b>Length-prefixing:</b> {@code H(a‖b)} is ambiguous — {@code ("ab","c")} and {@code ("a","bc")} would produce
 * the same bytes. Every variable-length field is preceded by its length.
 * <li><b>Sorting by child HASH bytes,</b> unsigned. The name is already inside each child's digest, so this
 * discriminates exactly as well as sorting by name while letting a parent hold only hashes. Crucially it never consults
 * locale collation or Unicode normalization, either of which would make the same tree hash differently on two machines.
 * <li><b>Size in the structure digest</b> (decision 2, PLAN.md Phase 23): free, since size is already in the manifest,
 * and it breaks up the coincidental matches that dominate a real tree — 46.7% of measured directories hold two files or
 * fewer, and 7,492 held nothing but a {@code pom.xml}.
 * </ul>
 *
 * <p>
 * <b>An empty directory is not the empty digest.</b> {@code structureOfDir(List.of())} is a real, distinct value —
 * {@code H("tree")} — so a tree containing an empty folder cannot collide with one that does not.
 */
public final class MerkleHash {

  private final static byte[] BLOB = "blob".getBytes(StandardCharsets.US_ASCII);
  private final static byte[] TREE = "tree".getBytes(StandardCharsets.US_ASCII);
  private final static byte[] GONE = "gone".getBytes(StandardCharsets.US_ASCII);

  /** Unsigned lexicographic order over digests — Java's byte is signed, which would sort 0x80.. before 0x00.. */
  private final static Comparator<byte[]> UNSIGNED = (a, b) -> {
    int n = Math.min(a.length, b.length);
    for (int i = 0; i < n; i++) {
      int c = Integer.compare(a[i] & 0xFF, b[i] & 0xFF);
      if (c != 0)
        return c;
    }
    return Integer.compare(a.length, b.length);
  };

  private MerkleHash() {
  }

  /**
   * A file's structural identity: its name and size, no content. Computable from a manifest alone, which is what makes
   * a 120 TB tree identifiable in the minutes {@code find} takes rather than the weeks hashing takes.
   */
  public static byte[] structureOfFile(HashAlgorithm algorithm, String name, long size) {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    write(buf, BLOB);
    writeLengthPrefixed(buf, name.getBytes(StandardCharsets.UTF_8));
    write(buf, ByteBuffer.allocate(Long.BYTES).putLong(size).array());
    return digest(algorithm, buf.toByteArray());
  }

  /**
   * A file's content identity, name included: what {@code merkle(D)} is built from once the bytes have been read.
   * {@code contentDigest} is the raw digest of the file's bytes.
   */
  public static byte[] merkleOfFile(HashAlgorithm algorithm, String name, byte[] contentDigest) {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    write(buf, BLOB);
    writeLengthPrefixed(buf, name.getBytes(StandardCharsets.UTF_8));
    writeLengthPrefixed(buf, contentDigest);
    return digest(algorithm, buf.toByteArray());
  }

  /**
   * An unreadable file's contribution to its directory's damage identity: its name, and the fact that it is missing.
   *
   * <p>
   * Fold these with {@link #structureOfDir} exactly as the other flavours are folded, and pass a subdirectory's own
   * damage digest up unchanged — a directory name is not part of any digest here, so damage stays comparable across a
   * relocated copy for the same reason content does.
   */
  public static byte[] damageOfFile(HashAlgorithm algorithm, String name) {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    write(buf, GONE);
    writeLengthPrefixed(buf, name.getBytes(StandardCharsets.UTF_8));
    return digest(algorithm, buf.toByteArray());
  }

  /**
   * Fold a directory's children into one digest. Works for all four flavours — pass structure hashes to get a structure
   * hash, merkle hashes to get a merkle hash, bare content digests to get the rename-surviving variant, or
   * {@link #damageOfFile} results to get a damage digest. The caller's choice of what to put in decides what comes out,
   * which is why there is one method and not four.
   *
   * <p>
   * An empty collection is legal and meaningful: it is the digest of an empty directory.
   */
  public static byte[] structureOfDir(HashAlgorithm algorithm, Collection<byte[]> childHashes) {
    List<byte[]> sorted = new ArrayList<>(childHashes);
    sorted.sort(UNSIGNED);
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    write(buf, TREE);
    for (byte[] child : sorted)
      writeLengthPrefixed(buf, child);
    return digest(algorithm, buf.toByteArray());
  }

  private static void write(ByteArrayOutputStream buf, byte[] bytes) {
    buf.write(bytes, 0, bytes.length);
  }

  /** Four-byte big-endian length, then the bytes. This is what makes concatenation unambiguous. */
  private static void writeLengthPrefixed(ByteArrayOutputStream buf, byte[] bytes) {
    write(buf, ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
    write(buf, bytes);
  }

  private static byte[] digest(HashAlgorithm algorithm, byte[] input) {
    try {
      // BLAKE3 is in HashAlgorithm as a reserved escape hatch and has no JCA
      // provider in a stock JVM; fail loudly here rather than silently hashing
      // a tree with the wrong function.
      return MessageDigest.getInstance(jcaName(algorithm)).digest(input);
    } catch (NoSuchAlgorithmException missing) {
      throw new IllegalStateException("no JCA provider for " + algorithm.algorithmName(), missing);
    }
  }

  private static String jcaName(HashAlgorithm algorithm) {
    if (algorithm == HashAlgorithm.SHA256)
      return "SHA-256";
    throw new IllegalArgumentException("no Merkle support for " + algorithm.algorithmName() + " yet");
  }
}
