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
 * What an external product catalog knows about a GTIN: prefill for a new item, never authority over one (every field is
 * user-editable before create, and a catalog miss never blocks creation). {@code sourceUrl} is the stable outside
 * product page — it rides the created item as a {@code source=} tag, which is also our ODbL attribution to the
 * open-data catalogs. Nullable fields are simply unknown.
 */
public record CatalogEntry(String gtin, String name, String brand, String description, String category,
    Double weightGrams, String imageUrl, String sourceUrl, String source) {

  public JsonObject toJson() {
    JsonObject j = new JsonObject().put("gtin", this.gtin);
    if (this.name != null)
      j.put("name", this.name);
    if (this.brand != null)
      j.put("brand", this.brand);
    if (this.description != null)
      j.put("description", this.description);
    if (this.category != null)
      j.put("category", this.category);
    if (this.weightGrams != null)
      j.put("weightGrams", this.weightGrams);
    if (this.imageUrl != null)
      j.put("imageUrl", this.imageUrl);
    if (this.sourceUrl != null)
      j.put("sourceUrl", this.sourceUrl);
    if (this.source != null)
      j.put("source", this.source);
    return j;
  }

  public static CatalogEntry fromJson(JsonObject j) {
    return new CatalogEntry(j.getString("gtin"), j.getString("name"), j.getString("brand"), j.getString("description"),
        j.getString("category"), j.getDouble("weightGrams"), j.getString("imageUrl"), j.getString("sourceUrl"),
        j.getString("source"));
  }
}
