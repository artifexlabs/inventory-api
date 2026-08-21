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
 * A physical marker resolving to exactly one item: an NFC tag's factory UID, a product's UPC, a foreign QR payload —
 * one {@code (kind, value)} mapping for every marker type rather than a bespoke column per type, the same shape as the
 * {@code user_identities(provider, subject)} table. Kinds are free-form lowercase slugs by convention ({@code nfc-uid},
 * {@code upc}, {@code qr}); a given {@code (kind, value)} pair claims at most one item.
 *
 * NOT the item's own ULID (which the printed QR encodes) and not an {@link ItemTag} (metadata markup); this is how a
 * scanned marker finds its item.
 */
public record ItemIdentity(String kind, String value) implements Comparable<ItemIdentity> {

  public ItemIdentity {
    if (kind == null || kind.isBlank())
      throw new IllegalArgumentException("an identity needs a kind");
    if (value == null || value.isBlank())
      throw new IllegalArgumentException("an identity needs a value");
    kind = kind.trim().toLowerCase(java.util.Locale.ROOT);
    value = value.trim();
  }

  public JsonObject toJson() {
    return new JsonObject().put("kind", this.kind).put("value", this.value);
  }

  public static ItemIdentity fromJson(JsonObject json) {
    return new ItemIdentity(json.getString("kind"), json.getString("value"));
  }

  @Override
  public int compareTo(ItemIdentity other) {
    int byKind = this.kind.compareTo(other.kind);
    return byKind != 0 ? byKind : this.value.compareTo(other.value);
  }
}
