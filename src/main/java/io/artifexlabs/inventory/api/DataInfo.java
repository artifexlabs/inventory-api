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

import io.vertx.core.json.JsonObject;

/**
 * Describes what kind of thing an {@link Item} is. A plain physical object is {@link #OBJECT} (equivalently, an Item
 * with no DataInfo at all). For data items it records the medium, whether the data can change (a disk) or not (a
 * write-once CD), and whether it is an archive acting as a sub-container for data on the same medium.
 *
 * @author mykel
 *
 */
public record DataInfo(MediaKind kind, boolean mutable, boolean archive, String locator) {

  /** A plain physical object; mutable/archive do not apply. */
  public final static DataInfo OBJECT = new DataInfo(MediaKind.OBJECT, false, false, null);

  public DataInfo {
    requireNonNull(kind, "kind");
    if (locator != null) {
      locator = locator.trim();
      if (locator.isEmpty())
        locator = null;
    }
  }

  /** Without a locator; the common case for a disc you hold in your hand. */
  public DataInfo(MediaKind kind, boolean mutable, boolean archive) {
    this(kind, mutable, archive, null);
  }

  /**
   * Where this medium is reached when it is not simply in your hands: a mount point, share URI, volume label, or
   * bucket. Free text on purpose — it names things this system does not manage and cannot verify.
   */
  public java.util.Optional<String> where() {
    return java.util.Optional.ofNullable(this.locator);
  }

  /** True when this item's contents can be described by a manifest at all. */
  public boolean holdsData() {
    return this.kind != MediaKind.OBJECT;
  }

  public JsonObject toJson() {
    JsonObject j = new JsonObject().put("kind", kind.name()).put("mutable", mutable).put("archive", archive);
    if (locator != null)
      j.put("locator", locator);
    return j;
  }

  public static DataInfo fromJson(JsonObject j) {
    return j == null ? null
        : new DataInfo(MediaKind.valueOf(j.getString("kind")), Boolean.TRUE.equals(j.getBoolean("mutable")),
            Boolean.TRUE.equals(j.getBoolean("archive")), j.getString("locator"));
  }
}
