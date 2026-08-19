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
package io.artifexlabs.inventory.api.events;

import io.artifexlabs.inventory.api.AuditEvent;
import io.artifexlabs.inventory.api.AuditEventFactory;

import io.vertx.core.json.JsonObject;

/**
 * Event-bus addressing and wire format. Everything publishes (fan-out) to
 * {@link #ADDRESS} and to the per-category address ({@code
 * inventory.events.<category>}, category = the action's prefix: item,
 * location, asset, region, user, token, label) so consumers pick their
 * granularity. Payloads are the serialized audit event plus a version field;
 * additive changes only — a breaking change bumps {@code VERSION} and
 * publishes both shapes for a deprecation window.
 */
public final class InventoryEvents {

  /** The firehose address: every domain fact. */
  public final static String ADDRESS = "inventory.events";

  /** Payload schema version carried in the {@code v} field. */
  public final static int VERSION = 1;

  private InventoryEvents() {
  }

  /** {@code inventory.events.<category>} for an action like {@code item.create}. */
  public final static String categoryAddress(String action) {
    int dot = action.indexOf('.');
    return ADDRESS + '.' + (dot < 0 ? action : action.substring(0, dot));
  }

  /** The bus payload: serialized audit event + schema version. */
  public final static JsonObject toWire(AuditEvent event) {
    return AuditEventFactory.serialize(event).put("v", VERSION);
  }

  /** Inverse of {@link #toWire}; ignores the version field. */
  public final static AuditEvent fromWire(JsonObject wire) {
    return AuditEventFactory.deserialize(wire.copy());
  }
}
