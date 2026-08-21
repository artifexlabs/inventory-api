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

import io.vertx.core.json.JsonObject;

/**
 * Metadata for an asset attached to an {@link Item} — a picture, an audio recording, a PDF, an archive: any content
 * type. The bytes themselves travel separately.
 *
 * {@code attachedAt} is fixed when the asset is first attached and never moves; {@code updatedAt} starts equal to it
 * and advances each time the asset is replaced. The superseded bytes are archived into the {@code asset.replace} audit
 * event, so history is recoverable without a versions table.
 *
 * {@code coordinates} is where a picture was taken when known — from an explicit client value (a phone's GPS at
 * capture) or, failing that, EXIF — and null otherwise; tiers above use it to SUGGEST a container, never to set one.
 *
 * {@code kind} distinguishes how an image asset is USED, not what it is: {@link #KIND_PHOTO} (default) is a picture of
 * a space or thing whose annotator boxes become items; {@link #KIND_MAP} is a floor plan or overview whose boxes mark
 * out named places (items of type=location). Free-form by design, like {@code Item.type}.
 *
 * @author mykel
 *
 */
public record AssetInfo(String id, String itemId, String filename, String contentType, long sizeBytes,
    Instant attachedAt, Instant updatedAt, LatLong coordinates, String kind) {

  public static final String KIND_PHOTO = "photo";
  public static final String KIND_MAP = "map";

  public AssetInfo {
    requireNonNull(id, "id");
    requireNonNull(itemId, "itemId");
    requireNonNull(filename, "filename");
    requireNonNull(contentType, "contentType");
    requireNonNull(attachedAt, "attachedAt");
    // an asset that has never been replaced was last updated when attached
    if (updatedAt == null)
      updatedAt = attachedAt;
    if (updatedAt.isBefore(attachedAt))
      throw new IllegalArgumentException("updatedAt " + updatedAt + " precedes attachedAt " + attachedAt);
    if (sizeBytes < 0)
      throw new IllegalArgumentException("sizeBytes must be non-negative: " + sizeBytes);
    if (kind == null || kind.isBlank())
      kind = KIND_PHOTO;
  }

  /** A freshly attached asset: updatedAt tracks attachedAt; default kind. */
  public AssetInfo(String id, String itemId, String filename, String contentType, long sizeBytes, Instant attachedAt,
      LatLong coordinates)
  {
    this(id, itemId, filename, contentType, sizeBytes, attachedAt, attachedAt, coordinates, null);
  }

  /** A freshly attached asset with no capture coordinates; default kind. */
  public AssetInfo(String id, String itemId, String filename, String contentType, long sizeBytes, Instant attachedAt) {
    this(id, itemId, filename, contentType, sizeBytes, attachedAt, attachedAt, null, null);
  }

  public java.util.Optional<LatLong> captureCoordinates() {
    return java.util.Optional.ofNullable(coordinates);
  }

  /** Whether this asset has been replaced since it was attached. */
  public boolean isRevised() {
    return this.updatedAt.isAfter(this.attachedAt);
  }

  /** The same asset with new content metadata and a fresh update stamp. */
  public AssetInfo revised(String filename, String contentType, long sizeBytes, Instant updatedAt,
      LatLong coordinates) {
    return new AssetInfo(this.id, this.itemId, filename, contentType, sizeBytes, this.attachedAt, updatedAt,
        coordinates, this.kind);
  }

  public JsonObject toJson() {
    JsonObject j = new JsonObject().put("id", id).put("itemId", itemId).put("filename", filename)
        .put("contentType", contentType).put("sizeBytes", sizeBytes).put("attachedAt", attachedAt)
        .put("updatedAt", updatedAt).put("kind", kind);
    if (coordinates != null)
      j.put("latitude", coordinates.latitude()).put("longitude", coordinates.longitude());
    return j;
  }

  public static AssetInfo fromJson(JsonObject j) {
    if (j == null)
      return null;
    Double lat = j.getDouble("latitude");
    Double lng = j.getDouble("longitude");
    return new AssetInfo(j.getString("id"), j.getString("itemId"), j.getString("filename"), j.getString("contentType"),
        j.getLong("sizeBytes"), j.getInstant("attachedAt"), j.getInstant("updatedAt"),
        lat != null && lng != null ? new LatLong(lat, lng) : null, j.getString("kind"));
  }
}
