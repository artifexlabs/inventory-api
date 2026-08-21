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
import java.util.Optional;

import io.vertx.core.json.JsonObject;

/**
 * A rectangular annotation drawn on a picture asset. Coordinates are NORMALIZED to the image (0–1 for x/y/w/h) so they
 * survive any display size. A region may exist bare — drawn but not yet described — in which case {@code itemId} is
 * null; linking it to an {@link Item} happens when the box's data is set (draw-then-describe).
 *
 * @author mykel
 *
 */
public record AssetRegion(String id, String assetId, double x, double y, double w, double h, String itemId,
    String label, Instant timestamp) {

  public AssetRegion {
    requireNonNull(id, "id");
    requireNonNull(assetId, "assetId");
    requireNonNull(timestamp, "timestamp");
    if (x < 0 || x > 1 || y < 0 || y > 1)
      throw new IllegalArgumentException("origin out of [0,1]: " + x + "," + y);
    // zero-BY-zero is a DOT — a point denoting an item without a box
    // (ongoing item 8); anything else needs both sides positive
    if ((w != 0 || h != 0) && (w <= 0 || h <= 0))
      throw new IllegalArgumentException("width/height must both be positive, or both zero for a dot: " + w + "x" + h);
    if (x + w > 1.0000001 || y + h > 1.0000001)
      throw new IllegalArgumentException("region exceeds the image: " + x + "+" + w + "," + y + "+" + h);
  }

  /** A point marker rather than a box: "the item is HERE" (ongoing item 8). */
  public boolean isDot() {
    return w == 0 && h == 0;
  }

  /** The linked item, once the box has been described. */
  public Optional<String> linkedItemId() {
    return Optional.ofNullable(itemId);
  }

  public JsonObject toJson() {
    JsonObject j = new JsonObject().put("id", id).put("assetId", assetId).put("x", x).put("y", y).put("w", w)
        .put("h", h).put("timestamp", timestamp);
    if (itemId != null)
      j.put("itemId", itemId);
    if (label != null)
      j.put("label", label);
    return j;
  }

  public static AssetRegion fromJson(JsonObject j) {
    return j == null ? null
        : new AssetRegion(j.getString("id"), j.getString("assetId"), j.getDouble("x"), j.getDouble("y"),
            j.getDouble("w"), j.getDouble("h"), j.getString("itemId"), j.getString("label"), j.getInstant("timestamp"));
  }
}
