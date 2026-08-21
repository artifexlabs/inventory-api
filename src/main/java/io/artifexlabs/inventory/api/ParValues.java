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

import io.vertx.core.json.JsonObject;

/**
 * Stocking targets for a physical item: minimum and maximum desired on-hand. The current on-hand count is
 * {@link Item#getQuantity()}; an item is below par when its quantity drops under {@code minOnHand}.
 *
 * @author mykel
 *
 */
public record ParValues(long minOnHand, long maxOnHand) {

  public ParValues {
    if (minOnHand < 0 || maxOnHand < 0 || maxOnHand < minOnHand)
      throw new IllegalArgumentException(
          "par values require 0 <= min <= max, got min=" + minOnHand + " max=" + maxOnHand);
  }

  public boolean isBelowMin(long quantityOnHand) {
    return quantityOnHand < minOnHand;
  }

  public JsonObject toJson() {
    return new JsonObject().put("minOnHand", minOnHand).put("maxOnHand", maxOnHand);
  }

  public static ParValues fromJson(JsonObject j) {
    return j == null ? null : new ParValues(j.getLong("minOnHand"), j.getLong("maxOnHand"));
  }
}
