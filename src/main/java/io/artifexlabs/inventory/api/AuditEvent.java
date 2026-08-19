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

import java.time.Instant;
import java.util.Optional;

import io.vertx.core.json.JsonObject;

/**
 * One entry in the audit trail. Every change the system makes produces exactly one
 * of these, recorded in the same transaction as the change itself.
 *
 * @author mykel
 *
 */
public interface AuditEvent {

  String getId();

  Instant getTimestamp();

  /** Who performed the action (user id, token id, or system principal). */
  String getPrincipal();

  /** What was done, e.g. "item.create", "item.delete", "user.add". */
  String getAction();

  /** The id of the object acted upon. */
  String getTargetId();

  /** Arbitrary detail payload, such as the before/after state. */
  default Optional<JsonObject> getDetails() {
    return Optional.empty();
  }
}
