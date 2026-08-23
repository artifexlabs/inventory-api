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

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * One file on a data medium: where it sits, how big it is, and what it hashes to.
 *
 * <p>
 * <b>Files are not items.</b> A medium's contents are a MANIFEST of these, not a subtree of {@link Item}s — files have
 * no par values, no labels, and no containment of their own, so minting an item per file would explode the containment
 * tree and the audit log to no purpose. The medium itself stays an ordinary item ({@link MediaKind#PHYSICAL_MEDIA} or
 * {@link MediaKind#REMOTE_STORAGE}), which is what "the physical object that contains the data" means here.
 *
 * <p>
 * <b>Archives are the one exception,</b> because an archive is both a file and a container. An entry whose
 * {@link #archiveContents()} is non-empty IS an archive: it stays a normal row in its medium's manifest (one file, its
 * own hash and size) AND becomes an item of its own with {@code DataInfo.archive() == true}, contained by the medium,
 * carrying the nested manifest as ITS manifest. Archives inside archives nest the same way, to any depth. Paths inside
 * {@code archiveContents} are relative to the archive root, not the medium.
 *
 * <p>
 * <b>Paths are scope-relative and normalized:</b> {@code /}-separated, no leading slash, no {@code .} or {@code ..}
 * segments, no empty segments. The medium's own name is never part of a path — that is what lets two media be compared
 * for mirror-ness at all.
 */
public record DataEntry(String path, long sizeBytes, HashAlgorithm hashAlgorithm, String hash, String mimeType,
    Instant modifiedAt, List<DataEntry> archiveContents) {

  public DataEntry {
    path = normalizePath(path);
    requireNonNull(hashAlgorithm, "hashAlgorithm");
    if (hash == null || hash.isBlank())
      throw new IllegalArgumentException("a data entry requires a content hash");
    hash = hash.trim().toLowerCase(Locale.ROOT);
    if (!hashAlgorithm.accepts(hash))
      throw new IllegalArgumentException("hash is not a valid " + hashAlgorithm.algorithmName() + " digest: " + hash);
    if (sizeBytes < 0)
      throw new IllegalArgumentException("sizeBytes cannot be negative");
    if (mimeType != null) {
      mimeType = mimeType.trim().toLowerCase(Locale.ROOT);
      if (mimeType.isEmpty())
        mimeType = null;
    }
    archiveContents = archiveContents == null ? List.of() : List.copyOf(archiveContents);
  }

  /** A plain file: no mime type, no timestamp, not an archive. */
  public static DataEntry of(String path, long sizeBytes, HashAlgorithm algorithm, String hash) {
    return new DataEntry(path, sizeBytes, algorithm, hash, null, null, List.of());
  }

  /**
   * Normalize a scope-relative path, or refuse it. Refusing beats silently rewriting: a path that escapes its scope
   * ({@code ../}) or arrives absolute means the producing side is describing something other than what we asked about.
   */
  public static String normalizePath(String raw) {
    if (raw == null || raw.isBlank())
      throw new IllegalArgumentException("a data entry requires a path");
    String cleaned = raw.trim().replace('\\', '/');
    while (cleaned.startsWith("./"))
      cleaned = cleaned.substring(2);
    StringBuilder out = new StringBuilder(cleaned.length());
    for (String segment : cleaned.split("/")) {
      if (segment.isEmpty() || segment.equals("."))
        continue; // collapse // and ./
      if (segment.equals(".."))
        throw new IllegalArgumentException("a data entry path may not leave its medium: " + raw);
      if (out.length() > 0)
        out.append('/');
      out.append(segment);
    }
    if (out.length() == 0)
      throw new IllegalArgumentException("a data entry requires a path");
    return out.toString();
  }

  /** The path split into the components a normalized path is made of. */
  public List<String> pathElements() {
    return List.of(this.path.split("/"));
  }

  /** The file name alone. */
  public String fileName() {
    int slash = this.path.lastIndexOf('/');
    return slash < 0 ? this.path : this.path.substring(slash + 1);
  }

  public Optional<String> mime() {
    return Optional.ofNullable(this.mimeType);
  }

  public Optional<Instant> modified() {
    return Optional.ofNullable(this.modifiedAt);
  }

  /** True when this file is itself a container of files. */
  public boolean isArchive() {
    return !this.archiveContents.isEmpty();
  }

  /** Every entry in this subtree, archives included, depth first. */
  public List<DataEntry> flatten() {
    if (!isArchive())
      return List.of(this);
    java.util.List<DataEntry> all = new java.util.ArrayList<>();
    all.add(this);
    for (DataEntry nested : this.archiveContents)
      all.addAll(nested.flatten());
    return List.copyOf(all);
  }

  public JsonObject toJson() {
    JsonObject j = new JsonObject().put("path", this.path).put("sizeBytes", this.sizeBytes)
        .put("hashAlgorithm", this.hashAlgorithm.algorithmName()).put("hash", this.hash);
    if (this.mimeType != null)
      j.put("mimeType", this.mimeType);
    if (this.modifiedAt != null)
      j.put("modifiedAt", this.modifiedAt.toString());
    if (isArchive())
      j.put("archiveContents", new JsonArray(this.archiveContents.stream().map(DataEntry::toJson).toList()));
    return j;
  }

  public static DataEntry fromJson(JsonObject j) {
    if (j == null)
      return null;
    HashAlgorithm algorithm = HashAlgorithm.byName(j.getString("hashAlgorithm", HashAlgorithm.SHA256.algorithmName()))
        .orElseThrow(() -> new IllegalArgumentException("unknown hash algorithm: " + j.getString("hashAlgorithm")));
    String modified = j.getString("modifiedAt");
    JsonArray nested = j.getJsonArray("archiveContents");
    List<DataEntry> contents = nested == null ? List.of()
        : nested.stream().map(JsonObject.class::cast).map(DataEntry::fromJson).toList();
    return new DataEntry(j.getString("path"), j.getLong("sizeBytes", 0L), algorithm, j.getString("hash"),
        j.getString("mimeType"), modified == null ? null : Instant.parse(modified), contents);
  }
}
