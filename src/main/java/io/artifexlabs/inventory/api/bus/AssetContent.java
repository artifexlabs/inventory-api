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
package io.artifexlabs.inventory.api.bus;

import java.util.Optional;

import io.artifexlabs.inventory.api.LatLong;

import io.vertx.core.json.JsonObject;

/**
 * File content crossing the bus: the bytes and what they are. The parent of
 * {@link AssetUpload} (which adds the owning item) — replacement
 * ({@link BusActions#ASSETS_REPLACE}) needs only the content, because the
 * asset it replaces already knows its item.
 *
 * Bytes ride base64 inside the envelope; bus messages are fully buffered,
 * accepted for the photo-and-document-sized assets this system stores.
 */
public interface AssetContent {

  String filename();

  String contentType();

  byte[] bytes();

  /** Explicit capture coordinates (client GPS); they beat EXIF when present. */
  Optional<LatLong> coordinates();

  /**
   * The asset kind ({@code photo}/{@code map}); null lets the store default.
   * Replacement content leaves it null — a replace never changes an asset's
   * kind.
   */
  default String kind() {
    return null;
  }

  JsonObject toJson();
}
