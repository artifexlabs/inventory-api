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
 * Region operations for spatial annotation: boxes drawn on picture assets and
 * their promotion into items. Kept beside {@link AssetStore} and
 * {@link InventorySystem} because promotion spans both — implementations must
 * make {@link #createItemFromRegion} and {@link #makeItemFromRegion}
 * transactionally complete (item + containment + region link + audit as one
 * mutation).
 *
 * @author mykel
 *
 */
public interface RegionSystem {

  /** Per-request attribution view; see {@link InventorySystem#actingAs}. */
  default RegionSystem actingAs(String principal) {
    return this;
  }

  /** All regions on an asset, drawn order; empty list for unknown assets. */
  CompletionStage<List<AssetRegion>> listRegions(String assetId);

  /**
   * Persist a bare box (draw-then-describe: no item yet). Empty if the asset
   * does not exist. Audited as {@code region.create}.
   */
  CompletionStage<Optional<AssetRegion>> createRegion(String assetId, double x, double y, double w, double h,
      String label);

  /** Remove a box; false if unknown. Audited as {@code region.delete}. */
  CompletionStage<Boolean> deleteRegion(String regionId);

  /**
   * One-shot: create an item, contain it in {@code containerId} (skipped when
   * null), and record the linked region — one transaction. Empty if the asset
   * (or non-null container) does not exist. Audited as
   * {@code item.create-from-region}.
   */
  CompletionStage<Optional<Item>> createItemFromRegion(String assetId, double x, double y, double w, double h,
      String name, String type, String containerId);

  /**
   * Describe an EXISTING bare box: create an item, contain it in
   * {@code containerId} (skipped when null), and link the region — one
   * transaction. Empty if the region is unknown or already linked. Audited as
   * {@code item.create-from-region}.
   */
  CompletionStage<Optional<Item>> makeItemFromRegion(String regionId, String name, String type, String containerId);
}
