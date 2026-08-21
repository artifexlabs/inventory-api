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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import io.vertx.core.json.JsonObject;

/**
 * Event-bus addressing and wire format for {@link StatusEvent} (PLAN.md Phase 21). Everything PUBLISHES (fan-out) to
 * {@link #ADDRESS}; a severity address exists so a consumer can subscribe to errors alone without filtering the
 * firehose. Payloads are additive-only — a breaking change bumps {@link #VERSION} and publishes both shapes for a
 * deprecation window, exactly as {@link InventoryEvents} does.
 *
 * <p>
 * This is a TOPIC, never request/reply: nothing acknowledges a status event, and nothing may fail because one could not
 * be delivered.
 */
public final class StatusEvents {

  /** The firehose address: every status event. */
  public final static String ADDRESS = "status.events";

  /** Payload schema version carried in the {@code v} field. */
  public final static int VERSION = 1;

  private StatusEvents() {
  }

  /** {@code status.events.<severity>}, lowercased — e.g. {@code status.events.error}. */
  public final static String severityAddress(StatusEvent.Severity severity) {
    return ADDRESS + '.' + severity.name().toLowerCase(java.util.Locale.ROOT);
  }

  /** The bus payload: the event plus a schema version. */
  public final static JsonObject toWire(StatusEvent event) {
    JsonObject subject = new JsonObject();
    event.subject().forEach(subject::put);
    return new JsonObject().put("v", VERSION).put("id", event.id())
        .put("ts", event.ts() == null ? null : event.ts().toString()).put("severity", event.severity().name())
        .put("code", event.code()).put("source", event.source()).put("subject", subject).put("message", event.message())
        .put("detail", event.detail()).put("correlationId", event.correlationId()).put("actor", event.actor());
  }

  /** Inverse of {@link #toWire}; ignores the version field. */
  public final static StatusEvent fromWire(JsonObject wire) {
    Map<String, String> subject = new LinkedHashMap<>();
    JsonObject raw = wire.getJsonObject("subject");
    if (raw != null)
      raw.forEach(e -> subject.put(e.getKey(), e.getValue() == null ? null : String.valueOf(e.getValue())));
    String ts = wire.getString("ts");
    return new StatusEvent(wire.getString("id"), ts == null ? null : Instant.parse(ts),
        StatusEvent.Severity.valueOf(wire.getString("severity")), wire.getString("code"), wire.getString("source"),
        subject, wire.getString("message"), wire.getString("detail"), wire.getString("correlationId"),
        wire.getString("actor"));
  }
}
