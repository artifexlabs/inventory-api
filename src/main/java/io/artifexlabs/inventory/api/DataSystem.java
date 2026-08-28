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

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * The contents of data media: what files are on a disc, a backup disk, or a share, and where else those same files
 * already live.
 *
 * <p>
 * <b>A manifest is a snapshot, not a row-edit surface.</b> {@link #replaceManifest} takes the whole listing at once
 * because that is what producing one actually looks like — walk the medium, hash every file, submit. There is no
 * add-one-file operation: a medium whose contents changed gets re-described, and the one targeted edit that exists
 * ({@link #renamePath}) is there precisely because correcting a name should not force re-hashing a disc.
 *
 * <p>
 * <b>The inventory question, asked of data.</b> {@link #findByHash} is what the whole model is for: "which disc has
 * this file?" and "is this already archived anywhere?" Deduplication is per-algorithm by nature — the same bytes hashed
 * two ways are two unrelated digests — see {@link HashAlgorithm}.
 */
public interface DataSystem {

  /** Per-request attribution view; see {@link InventorySystem#actingAs}. */
  default DataSystem actingAs(String principal) {
    return this;
  }

  /**
   * Replace this medium's entire manifest, in one unit of work.
   *
   * Archive entries (see {@link DataEntry#archiveContents()}) are materialized as items in the same transaction: each
   * becomes an item with {@code DataInfo.archive() == true} contained by the medium, carrying its nested manifest as
   * its own. Re-describing a medium replaces those too, so an archive that is no longer present stops being an item.
   *
   * Refuses (empty result) when the item is unknown or is a plain {@link MediaKind#OBJECT} — a crate has no files.
   * Audited as {@code data.replace} carrying COUNTS, never the entries: {@code audit_events} is the replay feed every
   * bus consumer pages, and a manifest in there would tax every replay forever.
   *
   * @return how many entries were stored across every scope, or empty when refused
   */
  CompletionStage<java.util.Optional<Integer>> replaceManifest(String itemId, List<DataEntry> entries);

  /**
   * Correct a path, keeping the content hash untouched — the file did not change, only its name did. Renaming a
   * DIRECTORY renames every descendant path beneath it.
   *
   * @return how many entries were repathed; 0 when nothing matched
   */
  CompletionStage<Integer> renamePath(String itemId, String fromPath, String toPath);

  /**
   * A page of this medium's manifest, ordered by path. {@code query} is an optional substring matched against the path;
   * blank or null means everything.
   */
  CompletionStage<List<DataEntry>> entriesOf(String itemId, String query, int page, int size);

  /** How many entries this medium's manifest holds, and their total size. */
  CompletionStage<ManifestSummary> summaryOf(String itemId);

  /** Every medium whose manifest contains this digest, with the paths it sits at. */
  CompletionStage<List<DataLocation>> findByHash(HashAlgorithm algorithm, String hash);

  /**
   * Media that overlap this one, most-shared first — "how much of this disc do I already have somewhere else?".
   *
   * <p>
   * <b>This replaces {@code findMirrorsOf}, and the difference is the return type, not the rule.</b> The old call
   * answered a per-MEDIUM question with per-FILE rows: millions of them, to say what turns out to be four numbers and
   * two flags. It was a Parallel Seq Scan at every measured scale — 155 ms at 551k rows, 5,528 ms at 5.5M, and a
   * composite index moved it by 0.9x to 1.5x, because the mismatch was the shape of the answer rather than a missing
   * index.
   *
   * <p>
   * <b>Sameness here is still path AND content</b>, deliberately. That rule did not stop being useful; it stopped being
   * the ONLY rule. "Is this disc a faithful copy of that one" wants it — a file that moved is not in the same place —
   * while {@link #findDuplicateSections} exists precisely to ignore location. Both rules are now expressed, each where
   * it belongs.
   *
   * <p>
   * Overlap is computed from content digests, so a medium nobody has hashed yet overlaps nothing. That is not a gap:
   * {@code findDuplicateSections(STRUCTURE)} answers the same shape of question from the manifest alone, weeks earlier.
   */
  CompletionStage<List<MediumOverlap>> findOverlappingMedia(String itemId);

  /**
   * Directory subtrees that appear more than once — "where are there duplicated sections in my media inventory?".
   *
   * <p>
   * This is the question {@link #findMirrorsOf} could never answer. That one compares files at the SAME path, so a
   * folder copied to a different place on another disc is invisible to it by construction. Here a subtree carries its
   * own identity, so where it sits is irrelevant.
   *
   * <p>
   * <b>The size floor is not optional.</b> On a real media tree 46.7% of directories hold two files or fewer and 68.6%
   * share their child-name set with another directory; 7,492 held nothing but a {@code pom.xml}. Without a floor this
   * returns hundreds of thousands of coincidences and buries the relocated backup you were looking for. See
   * {@link SectionQuery#sane}.
   */
  CompletionStage<List<SectionMatch>> findDuplicateSections(SectionQuery query);

  /** Which identity to compare subtrees by. They answer genuinely different questions. */
  enum Match {
    /**
     * Names and sizes, no content. Available the moment a manifest lands, because it reads no bytes — which is what
     * makes a 120 TB tree answerable in the minutes {@code find} takes rather than the weeks hashing takes. A strong
     * hint, not proof.
     */
    STRUCTURE,
    /** Names and content. Proof that two folders are copies, and unavailable until their files have been hashed. */
    MERKLE,
    /**
     * Content only, names ignored — so a folder whose files were renamed still matches. The most powerful and the
     * noisiest: with names out of the digest, every directory holding one identical file matches every other one, which
     * is why the floor matters most here.
     */
    CONTENT
  }

  /** Where to look for the other copy. */
  enum Scope {
    /** Different media only — "do I have this twice, on two discs?" */
    ACROSS_MEDIA,
    /**
     * Within one medium — "did I back this folder up into itself?". Not an edge case: a snapshotting filesystem holds
     * generations of the same tree on ONE medium, which is exactly where its duplication lives.
     */
    WITHIN_MEDIUM, BOTH
  }

  /**
   * What to compare, where, and how big a section has to be to be worth reporting. {@code itemId} may be null, meaning
   * the whole inventory rather than one medium's duplicates.
   */
  record SectionQuery(String itemId, Match match, Scope scope, int minFiles, long minBytes, int minDepth, int page,
      int size) {

    /**
     * Defaults chosen from the measured tree rather than taste. {@code minFiles = 8} is the floor the partial indexes
     * are built around; {@code minDepth} is deliberately 0 because depth alone does NOT do this job — those 7,492
     * lone-{@code pom.xml} directories sit at many different depths, and only size separates them out.
     */
    public static SectionQuery sane(String itemId, Match match) {
      return new SectionQuery(itemId, match, Scope.BOTH, 8, 0L, 0, 0, 50);
    }
  }

  /**
   * One subtree found in more than one place. The result is grouped by identity rather than listed per location,
   * because "this folder appears 3 times" is the answer and a flat list of directories is only the evidence.
   */
  record SectionMatch(String hash, long subtreeFiles, long subtreeBytes, List<SectionLocation> locations) {
  }

  /** One place a duplicated subtree sits. */
  record SectionLocation(String itemId, String itemName, String path, int depth) {
  }

  /** Roll-up of one medium's manifest. */
  record ManifestSummary(int entryCount, long totalBytes, int archiveCount) {
    public final static ManifestSummary EMPTY = new ManifestSummary(0, 0L, 0);
  }

  /** Where a particular file was found: which medium item, at which path. */
  record DataLocation(String itemId, String itemName, String path, long sizeBytes) {
  }

  /**
   * How much another medium has in common with this one.
   *
   * <p>
   * The three cases the old per-file return type conflated are now distinguishable. {@code identical} means the two
   * root Merkle digests are equal — the same tree, by name and content, all the way down. {@code contains} means every
   * hashed entry of OURS is also on theirs, so theirs is the superset: that is the "which backup is newer" answer, and
   * it is true whenever {@code identical} is. Neither flag set, with {@code sharedEntries} above zero, is partial
   * overlap.
   *
   * @param sharedEntries files at the same path with the same digest on both media
   * @param theirEntries  hashed files on the other medium, so the caller can see how much of it is NOT shared
   * @param ourEntries    hashed files on this one, which is what {@code contains} is measured against
   */
  record MediumOverlap(String itemId, String itemName, long sharedEntries, long sharedBytes, long theirEntries,
      long ourEntries, boolean identical, boolean contains) {
  }
}
