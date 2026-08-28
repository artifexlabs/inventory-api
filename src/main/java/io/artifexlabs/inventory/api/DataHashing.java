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

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * The claim/complete protocol a hashing worker speaks. Separate from {@link DataSystem} on purpose: that interface
 * describes and queries media, this one runs a job over them, and the two have different callers and different
 * lifetimes.
 *
 * <p>
 * <b>Hashing a real medium takes weeks, so interruption is the normal case.</b> A disc gets unplugged, a machine
 * reboots, a worker is killed. Every part of this protocol assumes that:
 *
 * <ul>
 * <li><b>Claims are committed, not held.</b> Hashing one large file can take minutes; a transaction must not stay open
 * that long. {@link #claim} marks rows CLAIMED and returns — the worker then reads bytes outside any transaction.
 * <li><b>A dead worker's claims expire.</b> {@link #reclaimStale} returns work whose lease has lapsed. Without it a
 * crash would strand rows in CLAIMED forever, and the medium would never finish.
 * <li><b>Completion is verified, not trusted.</b> {@link #complete} re-checks size and mtime before storing a digest. A
 * file that changed between the manifest and the read is a DIFFERENT file, and recording the new bytes against the old
 * description would be a lie nothing could detect afterwards.
 * <li><b>Unreadable is an outcome, not an error.</b> {@link #unreadable} records what could not be read so it becomes a
 * repair target — a file this medium cannot read may be readable on a sibling copy.
 * </ul>
 *
 * <p>
 * Note what is deliberately NOT here: reading bytes. This interface never touches a filesystem. The worker supplies
 * content, which is what lets the whole protocol be tested without one and lets an archive be hashed through the same
 * path as a loose file.
 */
public interface DataHashing {

  /** Attribution, matching the rest of the api. */
  default DataHashing actingAs(String principal) {
    return this;
  }

  /**
   * Take up to {@code limit} unhashed entries from this medium and mark them claimed by {@code worker}.
   *
   * <p>
   * Concurrent workers on one medium must not collide, and must not queue behind each other either — the implementation
   * is expected to skip rows another worker holds rather than wait for them.
   */
  CompletionStage<List<PendingFile>> claim(String itemId, String worker, int limit);

  /**
   * Record a digest, but only if the entry still matches the {@code claimed} description it was handed out under.
   *
   * <p>
   * Passing the claim back is what makes verification possible at all: between claim and complete the medium may have
   * been re-described, and attaching a digest to a row that now names a different file would be a lie nothing could
   * detect afterwards. The worker separately refuses to complete when the file it actually read differs from what it
   * claimed — this guards the other side of the same window.
   *
   * <p>
   * A refused completion leaves the claim STANDING rather than releasing it, which is deliberate: the row lapses back
   * to pending only when its lease expires, so a worker reading a medium whose manifest has gone stale cannot pick the
   * same file up again and loop on it.
   *
   * @return false when the entry no longer matches, and the digest is discarded
   */
  CompletionStage<Boolean> complete(String itemId, PendingFile claimed, HashAlgorithm algorithm, String hash);

  /**
   * Record that a file could not be read, with a reason worth showing a human. Repeated failures increment an attempt
   * count rather than piling up rows, so a permanently bad sector stays one line in the repair index.
   */
  CompletionStage<Void> unreadable(String itemId, String path, String reason);

  /**
   * Return claims older than {@code leaseSeconds} to the pending pool.
   *
   * @return how many were reclaimed
   */
  CompletionStage<Integer> reclaimStale(String itemId, long leaseSeconds);

  /** What is left to do on this medium — the progress bar, which {@code pending_files} already pays for. */
  CompletionStage<Progress> progressOf(String itemId);

  /**
   * Recompute this medium's directory rollup — Merkle identity, its name-independent variant, and the shape of whatever
   * could not be read — from the manifest as it stands right now.
   *
   * <p>
   * <b>A sweep, never an incremental counter.</b> Keeping the rollup current per completed file would cost 781 million
   * ancestor updates on a 50M-file medium — 91x the writes a sweep does — and would make each medium's root a row that
   * every hasher contends on, defeating the {@code SKIP LOCKED} that lets several workers share a disc. Recomputing
   * from ground truth also makes crash recovery free: there is no partially-applied increment to reconcile, because
   * nothing is remembered between runs.
   *
   * <p>
   * Cheap enough to run after every batch and correct enough to run once at the end. Directories with unhashed
   * descendants simply get no Merkle digest, so a rollup at any moment is a true statement about that moment.
   *
   * @return how many directory rows were rewritten
   */
  CompletionStage<Integer> rollUp(String itemId);

  /**
   * What this medium could not read, and which sibling media can supply it.
   *
   * <p>
   * The reason unreadable is an outcome rather than an error. A file this disc cannot read may be perfectly readable on
   * a copy, and that makes damage actionable instead of merely regrettable.
   *
   * <p>
   * <b>Matched by PATH, because there is no alternative.</b> An unreadable file has no content digest — that is what
   * unreadable means — so the only handle it has is where it sat. Which gives the path-equality rule being retired from
   * the old mirror query a second, better-motivated life.
   */
  CompletionStage<List<Repair>> findRepairs(String itemId);

  /**
   * One file to hash. Size and mtime travel with it because {@link #complete} verifies against them: the worker does
   * not need to re-stat, and cannot accidentally verify against a fresher description than the one it claimed.
   */
  record PendingFile(String path, long sizeBytes, Instant modifiedAt) {

    public PendingFile {
      // micros, matching DataEntry: this record's whole job is to be compared
      // against a stored description, and timestamptz holds micros
      if (modifiedAt != null)
        modifiedAt = modifiedAt.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }
  }

  /**
   * One damaged file and where an intact copy of it lives. {@code availableOn} being empty is itself the answer worth
   * having: nothing else in the inventory holds that path, so this damage is unrecoverable from what is catalogued.
   */
  record Repair(String path, String reason, int attempts, List<DataSystem.DataLocation> availableOn) {
  }

  /**
   * How far along a medium is. {@code unreadable} is counted separately from {@code done} because a medium that
   * finished with unreadable files is finished but NOT intact, and conflating the two would hide damage.
   */
  record Progress(long pending, long claimed, long done, long unreadable, long pendingBytes) {

    public boolean complete() {
      return this.pending == 0 && this.claimed == 0;
    }

    /** 0.0 to 1.0 by file count; a medium with nothing to hash is trivially done. */
    public double fraction() {
      long total = this.pending + this.claimed + this.done + this.unreadable;
      return total == 0 ? 1.0 : (double) (this.done + this.unreadable) / total;
    }
  }
}
