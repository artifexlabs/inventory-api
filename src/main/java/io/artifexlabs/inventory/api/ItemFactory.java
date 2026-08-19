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

import org.slf4j.*;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ItemFactory {
  private final static Logger log = LoggerFactory.getLogger(ItemFactory.class);

  public final static Item deserialize(JsonObject j) {
    if (j == null)
      return null;
    try {
      return new DefaultItem(j);
    } catch (RuntimeException e) {
      log.error("Could not deserialize Item from {}", j.encode(), e);
      throw e;
    }
  }

  public final static JsonObject serialize(Item i) {
    JsonObject j = new JsonObject().put("id", i.getId()).put("name", i.getName()).put("type", i.getType())
        .put("timestamp", i.getTimestamp());
    i.getDisplayName().ifPresent(d -> j.put("displayName", d));
    i.getDescription().ifPresent(d -> j.put("description", d));
    i.getContainerId().ifPresent(c -> j.put("containerId", c));
    i.getCoordinates().ifPresent(c -> j.put("latitude", c.latitude()).put("longitude", c.longitude()));
    if (i.isHeavy())
      j.put("heavy", true);
    i.getExpiration().ifPresent(e -> j.put("expiration", e.toJson()));
    if (!i.getTags().isEmpty())
      j.put("tags", new JsonArray(i.getTags().stream().map(ItemTag::toJson).toList()));
    i.getDataInfo().ifPresent(d -> j.put("dataInfo", d.toJson()));
    i.getQuantity().ifPresent(q -> j.put("quantity", q));
    i.getWeight().ifPresent(w -> j.put("weightGrams", w.grams()));
    i.getDimensions().ifPresent(d -> j.put("dimensionsCm", d.toJson()));
    i.getParValues().ifPresent(p -> j.put("parValues", p.toJson()));
    i.getContainedItems()
        .ifPresent(items -> j.put("containedItems", new JsonArray(items.stream().map(ItemFactory::serialize).toList())));
    return j;
  }
}
