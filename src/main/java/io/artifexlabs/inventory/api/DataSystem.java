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
   * Media that hold the same file at the same path as {@code itemId} — the mirror question. Both halves are compared as
   * stored hashes, so this never walks a path string.
   */
  CompletionStage<List<DataLocation>> findMirrorsOf(String itemId);

  /** Roll-up of one medium's manifest. */
  record ManifestSummary(int entryCount, long totalBytes, int archiveCount) {
    public final static ManifestSummary EMPTY = new ManifestSummary(0, 0L, 0);
  }

  /** Where a particular file was found: which medium item, at which path. */
  record DataLocation(String itemId, String itemName, String path, long sizeBytes) {
  }
}
