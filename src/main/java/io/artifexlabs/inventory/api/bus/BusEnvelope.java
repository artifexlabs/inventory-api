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
package io.artifexlabs.inventory.api.bus;

import java.util.Optional;
import java.util.Set;

import io.vertx.core.json.JsonObject;

/**
 * The parent of every request that crosses the service bus: it identifies the
 * work (action + optional target id), carries the payload, and binds the
 * request to the authenticated user it acts for.
 *
 * External inputs (browser, mobile) are authenticated at the HTTP tier; a
 * service already on the bus is considered authenticated. Either way the
 * envelope must name the acting user ({@link #userId()}/{@link #principal()})
 * so every action is attributable, must assert the user's {@link #roles()}
 * (the worker refuses actions whose required role is missing), and must
 * present the shared fabric {@link #token()} before any worker will touch it.
 */
public interface BusEnvelope {

  /** Envelope schema version; additive changes only. */
  int version();

  /** The shared bus-fabric token; workers refuse envelopes without it. */
  String token();

  /** Id of the acting user; empty string for pre-auth operations only. */
  String userId();

  /** Human-readable actor (email or service name) for audit provenance. */
  String principal();

  /** Roles asserted by the authenticated edge; see {@link Roles}. */
  Set<String> roles();

  /** What to do; one of the {@link BusActions} constants. */
  String action();

  /** The identifier the action targets, when it targets one thing. */
  Optional<String> targetId();

  /** Action payload (update data, creation fields); never null, may be empty. */
  JsonObject data();

  /** Wire form; the inverse lives in the impl module. */
  JsonObject toJson();
}
