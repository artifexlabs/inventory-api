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
import java.util.Optional;
import java.util.Set;

/**
 * Inventory is made of Item objects
 *
 * Any given Item can be a container for other Item objects, as well as an object itself
 *
 * Item objects have the following properties
 *
 * <ul>
 * <li>A unique id that cannot change over time</li>
 * <li>A non-null name and possibly a display name (but probably not)</li>
 * <li>A timestamp that is the last-updated time</li>
 * </ul>
 * @author mykel
 *
 */
public interface Item {

  /**
   * The unique id of this item, which cannot change over time.
   *
   * @return
   */
  String getId();

  /**
   * The non-null name of this item.
   *
   * @return
   */
  String getName();

  /**
   * The last-updated time of this item.
   *
   * @return
   */
  Instant getTimestamp();

  default Optional<String> getDisplayName() {
    return Optional.ofNullable(getName());
  }

  /**
   * The type of the object is the indicator of which subsystem will process any
   * requests for it.  This is a simple string switch within event processing.
   *
   * The default value for "Generic Object" is InventoryConstants.DEFAULT_TYPE ( "_" )
   * @return
   */
   String getType() ;

  /**
   * A free-form human description of this item.
   *
   * @return
   */
  default Optional<String> getDescription() {
    return Optional.empty();
  }

  /**
   * The single container this item sits inside, if any. Containment is a
   * TREE since Phase 15: a physical thing is in exactly one place, so this
   * replaces both the old {@code locationId} and the many-to-many
   * containment join. An item with no container is a root — typically a
   * building or a site.
   *
   * @return
   */
  default Optional<String> getContainerId() {
    return Optional.empty();
  }

  /**
   * This item's OWN coordinates, when it has been pinned. Most items are not
   * pinned; they inherit from their container chain — resolve with
   * {@code InventorySystem.effectiveCoordinates(id)} rather than reading this
   * directly when you want "where is this thing".
   *
   * A "location" in the old sense is simply a container that carries
   * coordinates.
   *
   * @return
   */
  default Optional<LatLong> getCoordinates() {
    return Optional.empty();
  }

  /**
   * Whether moving this item is a two-person job. A human judgment, NOT
   * derived from {@link #getWeight()} — an awkward empty cabinet can be
   * heavy while a dense small thing is not.
   *
   * @return
   */
  default boolean isHeavy() {
    return false;
  }

  /**
   * When this item expires, if it does. See {@link Expiration#absolute()} for
   * the hard-stop vs recommendation distinction.
   *
   * @return
   */
  default Optional<Expiration> getExpiration() {
    return Optional.empty();
  }

  /**
   * Free-form key/value markup, keys unique per item. See {@link ItemTag}.
   *
   * @return
   */
  default Set<ItemTag> getTags() {
    return Set.of();
  }

  /**
   * Present only when this item is (or carries) data, describing the medium it
   * lives on and how it may change. An Item is any physical thing; most items
   * are not data and this is empty for them.
   *
   * @return
   */
  default Optional<DataInfo> getDataInfo() {
    return Optional.empty();
  }

  /**
   * How many of this item are on hand, when counted.
   *
   * @return
   */
  default Optional<Long> getQuantity() {
    return Optional.empty();
  }

  /**
   * The weight of one of this item, when known. Canonical unit is grams.
   *
   * @return
   */
  default Optional<Weight> getWeight() {
    return Optional.empty();
  }

  /**
   * The size of this item as length x width x height, when known. Canonical
   * unit is centimeters.
   *
   * @return
   */
  default Optional<Dimensions> getDimensions() {
    return Optional.empty();
  }

  /**
   * Stocking targets (min/max on-hand) for this item, when management wants
   * them tracked. Current on-hand is {@link #getQuantity()}.
   *
   * @return
   */
  default Optional<ParValues> getParValues() {
    return Optional.empty();
  }

  /**
   *
   * Optional collections are weird, but in this case it means something.  If this item has no
   * contained items, it's not a container.
   *
   * @return
   */
  Optional<Set<Item>> getContainedItems();

  default boolean isContainer() {
    return getContainedItems().isPresent();
  }

}
