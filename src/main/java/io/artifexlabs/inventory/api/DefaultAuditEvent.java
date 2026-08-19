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

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import io.vertx.core.json.JsonObject;

/**
 * Stock immutable {@link AuditEvent}. Identity is the id alone.
 *
 * @author mykel
 *
 */
public final class DefaultAuditEvent implements AuditEvent {
  private final String id;
  private final Instant timestamp;
  private final String principal;
  private final String action;
  private final String targetId;
  private final JsonObject details;

  public DefaultAuditEvent(String id, Instant timestamp, String principal, String action, String targetId,
      JsonObject details) {
    this.id = requireNonNull(id, "id");
    this.timestamp = requireNonNull(timestamp, "timestamp");
    this.principal = requireNonNull(principal, "principal");
    this.action = requireNonNull(action, "action");
    this.targetId = requireNonNull(targetId, "targetId");
    this.details = details;
  }

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public Instant getTimestamp() {
    return this.timestamp;
  }

  @Override
  public String getPrincipal() {
    return this.principal;
  }

  @Override
  public String getAction() {
    return this.action;
  }

  @Override
  public String getTargetId() {
    return this.targetId;
  }

  @Override
  public Optional<JsonObject> getDetails() {
    return Optional.ofNullable(this.details);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.id);
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || (obj instanceof AuditEvent other && this.id.equals(other.getId()));
  }

  @Override
  public String toString() {
    return AuditEventFactory.serialize(this).encode();
  }
}
