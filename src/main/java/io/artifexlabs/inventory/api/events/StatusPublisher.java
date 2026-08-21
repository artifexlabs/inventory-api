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

/**
 * Fire-and-forget publication of {@link StatusEvent}s — the channel that carries operational trouble to a HUMAN instead
 * of to a log file (MORE_VERTX ask 3).
 *
 * <p>
 * Implementations stamp {@link StatusEvent#id()} and {@link StatusEvent#ts()} at publication (so identity and time
 * agree with delivery order), never throw, and never block: an action must not fail because its notification could not
 * be delivered. Nothing acknowledges a status event.
 */
public interface StatusPublisher {

  /** Publish an operational outcome. Never throws, never blocks. */
  void publish(StatusEvent event);

  /** The default publisher: status events go nowhere (no bus, or a plain unit test). */
  StatusPublisher NOOP = event -> {
  };

  /** Convenience: build and publish in one call, so emitting stays a one-liner at refusal sites. */
  default void publish(StatusEvent.Builder builder) {
    publish(builder.build());
  }
}
