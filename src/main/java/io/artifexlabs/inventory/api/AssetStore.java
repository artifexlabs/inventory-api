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
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Stores assets (pictures, audio, documents) attached to items. Implementations must remove an item's assets when the
 * item itself is deleted.
 *
 * @author mykel
 *
 */
public interface AssetStore {

  /** Per-request attribution view; see {@link InventorySystem#actingAs}. */
  default AssetStore actingAs(String principal) {
    return this;
  }

  record StoredAsset(AssetInfo info, byte[] data) {
  }

  /** An item created from a photo, with the photo attached — one transaction. */
  record PhotoItem(Item item, AssetInfo asset) {
  }

  /** Attach bytes to an item; empty if the item does not exist. */
  default CompletionStage<Optional<AssetInfo>> store(String itemId, String filename, String contentType, byte[] data) {
    return store(itemId, filename, contentType, data, null, null);
  }

  /** Attach bytes with capture coordinates; default {@code kind}. */
  default CompletionStage<Optional<AssetInfo>> store(String itemId, String filename, String contentType, byte[] data,
      LatLong explicitCoordinates) {
    return store(itemId, filename, contentType, data, explicitCoordinates, null);
  }

  /**
   * Attach bytes with capture coordinates when the CLIENT knows them (a phone's GPS at capture time). When
   * {@code explicitCoordinates} is null, implementations extract GPS EXIF from image data as a best effort; explicit
   * coordinates always win over EXIF. {@code kind} distinguishes the asset's use ({@link AssetInfo#KIND_PHOTO} default,
   * {@link AssetInfo#KIND_MAP} for boxes-become-locations annotation).
   */
  CompletionStage<Optional<AssetInfo>> store(String itemId, String filename, String contentType, byte[] data,
      LatLong explicitCoordinates, String kind);

  /**
   * A picture that IS a thing: create a NEW item and attach the uploaded bytes to it as its first asset,
   * transactionally complete with audit ({@code item.create} + {@code asset.attach}, plus {@code item.contain} when
   * {@code containerId} places it). The item's own coordinates are pinned from {@code explicitCoordinates}, else image
   * EXIF, else left unset — so a location-typed item made this way is immediately a coordinate-bearing container whose
   * annotator boxes furnish it.
   *
   * Empty when {@code containerId} is given but unknown.
   */
  CompletionStage<Optional<PhotoItem>> createItemFromPhoto(String name, String displayName, String type,
      String containerId, String filename, String contentType, byte[] data, LatLong explicitCoordinates, String kind);

  /**
   * A scanned barcode that IS a thing (ongoing item 7's catalog tail): create a NEW item from merged catalog-prefill +
   * user fields, claim the {@code (upc, gtin13)} identity for it, apply the catalog tags, and — when {@code imageBytes}
   * is present — attach the already-downloaded catalog image as its first asset. One transaction, audited with the
   * existing vocabulary ({@code item.create}, {@code item.contain}, {@code item.identity-add}, {@code item.tag},
   * {@code asset.attach}).
   *
   * Empty when {@code containerId} names an unknown item. Completes exceptionally with {@link IllegalStateException}
   * when the GTIN already claims another item — rescan that package and it OPENS instead.
   * {@code imageFilename}/{@code imageContentType}/{@code imageBytes} may all be null (catalog miss or imageless
   * entry); the created {@link PhotoItem#asset()} is then null.
   */
  CompletionStage<Optional<PhotoItem>> createItemFromUpc(UpcItemCreation spec, String imageFilename,
      String imageContentType, byte[] imageBytes);

  /**
   * Replace an existing asset's content in place — same asset id, same owning item, so every reference to it (regions
   * drawn on a picture, links in notes) keeps pointing at the right thing. {@link AssetInfo#attachedAt()} is preserved
   * and {@link AssetInfo#updatedAt()} advances.
   *
   * Implementations MUST archive the superseded bytes and metadata (into the asset archive, in the same transaction)
   * before overwriting, and the {@code asset.replace} audit event references that archive entry — replacing a photo
   * must never destroy the one it replaced, and the audit log must never carry blobs (it is the replay feed every
   * consumer pages). Empty when the asset is unknown.
   */
  CompletionStage<Optional<AssetInfo>> replace(String assetId, String filename, String contentType, byte[] data,
      LatLong explicitCoordinates);

  CompletionStage<Optional<StoredAsset>> get(String assetId);

  CompletionStage<List<AssetInfo>> listFor(String itemId);

  CompletionStage<Boolean> delete(String assetId);
}
