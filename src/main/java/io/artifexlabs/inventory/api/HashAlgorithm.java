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

import java.util.Optional;

/**
 * How a {@link DataEntry}'s content hash was computed.
 *
 * <p>
 * <b>Why an enum with a number.</b> A manifest has one row per FILE — a backup disk is easily hundreds of thousands —
 * so the algorithm name is stored as {@link #id()}, a small integer, rather than repeating the text on every row. The
 * wire and API forms always speak the {@link #algorithmName()}; the number is a storage detail.
 *
 * <p>
 * <b>Why SHA-256 is the default.</b> Manifests are produced by hashing whole media — optical discs, spinning disks,
 * network shares — where the media, not the hash, is the bottleneck: with SHA-NI (x86) or the ARMv8 crypto extensions,
 * SHA-256 outruns every one of those sources, so a faster hash would buy nothing. It is cryptographically
 * collision-resistant, which matters because {@code findByHash} answers "is this file already archived?" and a false
 * yes silently loses data. And it is what {@code sha256sum} / {@code shasum -a 256} / {@code CertUtil} already emit,
 * which is what makes plain-text manifest ingestion need no client tool at all.
 *
 * <p>
 * <b>Adding one.</b> {@link #BLAKE3} is the escape hatch if local NVMe trees ever make hashing the bottleneck
 * (cryptographic, 256-bit, several GB/s, and {@code b3sum} emits the same line format). Note that deduplication only
 * works WITHIN an algorithm — the same file hashed two ways looks like two unrelated files — so the default is sticky
 * on purpose: switch deliberately, not per ingest.
 */
public enum HashAlgorithm {

  /** The default; see the class note for why. */
  SHA256(1, "sha256", 64),
  /** Reserved for the fast-local-tree case; no ingest path selects it yet. */
  BLAKE3(2, "blake3", 64);

  private final int id;
  private final String algorithmName;
  private final int hexLength;

  private HashAlgorithm(int id, String algorithmName, int hexLength) {
    this.id = id;
    this.algorithmName = algorithmName;
    this.hexLength = hexLength;
  }

  /** The stored form: small and stable. NEVER renumber a released value. */
  public int id() {
    return this.id;
  }

  /** The spoken form, lowercase, as it appears on the wire and in the API. */
  public String algorithmName() {
    return this.algorithmName;
  }

  /** How many hex characters a digest of this algorithm has. */
  public int hexLength() {
    return this.hexLength;
  }

  /** True when {@code hash} could be a digest of this algorithm. */
  public boolean accepts(String hash) {
    if (hash == null || hash.length() != this.hexLength)
      return false;
    for (int i = 0; i < hash.length(); i++) {
      char c = hash.charAt(i);
      boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
      if (!hex)
        return false;
    }
    return true;
  }

  public static Optional<HashAlgorithm> byId(int id) {
    for (HashAlgorithm a : values())
      if (a.id == id)
        return Optional.of(a);
    return Optional.empty();
  }

  /** Case-insensitive lookup by spoken name; empty when unknown. */
  public static Optional<HashAlgorithm> byName(String name) {
    if (name == null)
      return Optional.empty();
    String wanted = name.trim().toLowerCase(java.util.Locale.ROOT).replace("-", "");
    for (HashAlgorithm a : values())
      if (a.algorithmName.equals(wanted))
        return Optional.of(a);
    return Optional.empty();
  }
}
