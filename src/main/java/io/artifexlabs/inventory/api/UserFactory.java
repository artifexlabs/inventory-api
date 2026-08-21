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
 * JSON wire format for {@link InventoryUser} objects. Never carries credentials — those live in storage only.
 *
 * @author mykel
 *
 */
public class UserFactory {

  public final static InventoryUser deserialize(JsonObject j) {
    return j == null ? null
        : new DefaultInventoryUser(j.getString("id"), j.getString("email"), j.getString("displayName"),
            Boolean.TRUE.equals(j.getBoolean("admin")));
  }

  public final static JsonObject serialize(InventoryUser u) {
    JsonObject j = new JsonObject().put("id", u.getId()).put("email", u.getEmail()).put("admin", u.isAdmin());
    u.getDisplayName().ifPresent(d -> j.put("displayName", d));
    return j;
  }
}
