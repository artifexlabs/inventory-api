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
 * Primary inventory service contract. Implementations must make every mutation
 * transactionally complete and record an audit trail entry for it.
 */
public interface InventorySystem {

  /**
   * A view of this system whose audit entries attribute mutations to the
   * given acting principal — how a per-request actor (a bus envelope's
   * authenticated user) flows into the audit trail. Views share state with
   * their parent; the default returns {@code this} (service-level
   * attribution).
   */
  default InventorySystem actingAs(String principal) {
    return this;
  }

  /**
   * Fetch every item from the system
   *
   * @return
   */
  CompletionStage<List<Item>> getAllItems();

  CompletionStage<List<Item>> getItemsOfType(String type);

  CompletionStage<Optional<Item>> getItem(String id);

  CompletionStage<Item> createItem(String name, String displayName, String type);

  CompletionStage<Boolean> updateItem(Item item);

  CompletionStage<Boolean> deleteItem(String id);

  /**
   * The single container this item sits in — the reverse of
   * {@link Item#getContainedItems()}. Empty when the item is a root (or does
   * not exist).
   *
   * Containment is a TREE since Phase 15; this replaced {@code
   * getContainersOf}, which could return several.
   *
   * @param itemId
   * @return
   */
  CompletionStage<Optional<Item>> getContainer(String itemId);

  /**
   * Put an item into a container, RE-PARENTING it if it was already
   * somewhere else — a thing is in exactly one place, so this is a move, not
   * an addition. False when either id is unknown, they are equal, or the
   * move would create a cycle (a container cannot be placed inside its own
   * descendant).
   *
   * @param containerId
   * @param itemId
   * @return
   */
  CompletionStage<Boolean> addToContainer(String containerId, String itemId);

  /**
   * Take an item out of its container, making it a root; false if it was not
   * in that container.
   *
   * @param containerId
   * @param itemId
   * @return
   */
  CompletionStage<Boolean> removeFromContainer(String containerId, String itemId);

  /**
   * Move an item to a container. Identical in effect to
   * {@link #addToContainer} now that containment is single-parent; retained
   * because it reads better at call sites that mean "relocate".
   *
   * @param itemId
   * @param targetContainerId
   * @return
   */
  CompletionStage<Boolean> moveToContainer(String itemId, String targetContainerId);

  /**
   * Where this item actually is: its own coordinates when pinned, otherwise
   * the nearest ancestor container's, walking up the containment chain.
   * Empty when nothing in the chain is pinned.
   *
   * @param itemId
   * @return
   */
  CompletionStage<Optional<LatLong>> effectiveCoordinates(String itemId);

  /**
   * Attach a tag, replacing any existing tag with the same key. False if the
   * item is unknown.
   *
   * @param itemId
   * @param tag
   * @return
   */
  CompletionStage<Boolean> tag(String itemId, ItemTag tag);

  /**
   * Remove a tag by key; false if the item or the key was not there.
   *
   * @param itemId
   * @param key
   * @return
   */
  CompletionStage<Boolean> untag(String itemId, String key);

  /**
   * Items carrying a tag matching the query (existence, glob, or regex).
   *
   * @param query
   * @return
   */
  CompletionStage<List<Item>> findByTag(TagQuery query);

  /**
   * Claim a physical-marker identity for an item. Idempotent when the item
   * already holds this exact identity; false when the item is unknown. When
   * the identity already claims a DIFFERENT item, the stage completes
   * exceptionally with {@link IllegalStateException} — a marker reused on a
   * second item is a mistake to surface, never a silent re-point (remove it
   * from the first item explicitly to migrate a marker).
   *
   * @param itemId
   * @param identity
   * @return
   */
  CompletionStage<Boolean> addIdentity(String itemId, ItemIdentity identity);

  /**
   * Release an identity from an item; false when the item is unknown or the
   * identity is not among that item's markers.
   *
   * @param itemId
   * @param identity
   * @return
   */
  CompletionStage<Boolean> removeIdentity(String itemId, ItemIdentity identity);

  /**
   * The item a scanned marker resolves to, if any.
   *
   * @param kind
   * @param value
   * @return
   */
  CompletionStage<Optional<Item>> findByIdentity(String kind, String value);

  /**
   * Every marker claimed for an item, sorted; empty for an unknown item.
   *
   * @param itemId
   * @return
   */
  CompletionStage<List<ItemIdentity>> identitiesOf(String itemId);
}
