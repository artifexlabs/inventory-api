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

/**
 * Everything the one-shot create-from-UPC needs, already merged: request fields have won over catalog prefill by the
 * time this is built (the worker does the merging), tags carry the catalog metadata ({@code brand=}, {@code category=},
 * {@code source=}), and {@code gtin13} is the canonical identity the new item claims. {@code name} and {@code type} are
 * required; the rest may be null/empty.
 */
public record UpcItemCreation(String gtin13, String name, String displayName, String type, String description,
    Double weightGrams, String containerId, List<ItemTag> tags) {

  public UpcItemCreation {
    if (gtin13 == null || gtin13.isBlank())
      throw new IllegalArgumentException("a UPC creation needs its gtin13");
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("a UPC creation needs a name");
    if (type == null || type.isBlank())
      throw new IllegalArgumentException("a UPC creation needs a type");
    tags = tags == null ? List.of() : List.copyOf(tags);
  }
}
