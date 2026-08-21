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

import io.artifexlabs.inventory.api.Item;

import io.vertx.core.json.JsonObject;

/**
 * An item update: the identifier of the item being updated together with its full replacement state
 * ({@link BusActions#ITEMS_UPDATE}). The id is carried separately from the state so the worker can verify they agree.
 */
public interface ItemUpdate {

  /** The id of the item being updated. */
  String itemId();

  /** The complete replacement state. */
  Item item();

  JsonObject toJson();
}
