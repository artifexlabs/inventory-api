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

import java.util.Optional;

import io.vertx.core.json.JsonObject;

/**
 * A metadata markup on an item: a key that may or may not carry a value. {@code scuba} renders as "scuba";
 * {@code color=orange} renders as "color=orange". Keys are unique per item (re-tagging the same key replaces its
 * value).
 *
 * NOT to be confused with a printed <em>label</em> (the physical thing a printer produces) or an NFC tag; this is data
 * attached to the item.
 */
public record ItemTag(String key, String value) implements Comparable<ItemTag> {

  public ItemTag {
    if (key == null || key.isBlank())
      throw new IllegalArgumentException("a tag needs a key");
    key = key.trim();
    if (value != null && value.isBlank())
      value = null;
  }

  /** A bare tag with no value. */
  public static ItemTag of(String key) {
    return new ItemTag(key, null);
  }

  public Optional<String> optionalValue() {
    return Optional.ofNullable(this.value);
  }

  /** Display form: {@code key} or {@code key=value}. */
  public String render() {
    return this.value == null ? this.key : this.key + '=' + this.value;
  }

  /** Parse the display form; anything after the first {@code =} is the value. */
  public static ItemTag parse(String rendered) {
    if (rendered == null || rendered.isBlank())
      throw new IllegalArgumentException("cannot parse a blank tag");
    int eq = rendered.indexOf('=');
    return eq < 0 ? new ItemTag(rendered, null) : new ItemTag(rendered.substring(0, eq), rendered.substring(eq + 1));
  }

  public JsonObject toJson() {
    JsonObject j = new JsonObject().put("key", this.key);
    if (this.value != null)
      j.put("value", this.value);
    return j;
  }

  public static ItemTag fromJson(JsonObject j) {
    return new ItemTag(j.getString("key"), j.getString("value"));
  }

  @Override
  public int compareTo(ItemTag other) {
    return this.key.compareToIgnoreCase(other.key);
  }
}
