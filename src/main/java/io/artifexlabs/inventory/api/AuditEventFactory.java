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
 * JSON wire format for {@link AuditEvent} objects.
 *
 * @author mykel
 *
 */
public class AuditEventFactory {

  public final static AuditEvent deserialize(JsonObject j) {
    return j == null ? null
        : new DefaultAuditEvent(j.getString("id"), j.getInstant("timestamp"), j.getString("principal"),
            j.getString("action"), j.getString("targetId"), j.getJsonObject("details"));
  }

  public final static JsonObject serialize(AuditEvent e) {
    JsonObject j = new JsonObject().put("id", e.getId()).put("timestamp", e.getTimestamp())
        .put("principal", e.getPrincipal()).put("action", e.getAction()).put("targetId", e.getTargetId());
    e.getDetails().ifPresent(d -> j.put("details", d));
    return j;
  }
}
