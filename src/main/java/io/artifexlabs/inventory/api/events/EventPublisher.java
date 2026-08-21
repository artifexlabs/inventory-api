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

import java.util.Optional;
import java.util.concurrent.CompletionStage;

import io.artifexlabs.inventory.api.AuditEvent;

/**
 * Fire-and-forget publication of domain facts (see VERTICLES.md). The event IS the audit event — same id, same shape —
 * so live consumers can dedupe against replay from the {@code audit_events} table, which remains the durable log of
 * record. Publication must never couple to mutation success: implementations never throw and never block, and callers
 * publish only AFTER their transaction has committed (announcing a change that might roll back is worse than dropping a
 * message a consumer will reconcile anyway).
 *
 * Consumers are idempotent and dedupe by event id; the {@code principal} field is provenance, never a credential to
 * re-authorize with.
 */
public interface EventPublisher {

  /** Publish a committed fact. Never throws, never blocks. */
  void publish(AuditEvent event);

  /** The default publisher: events go nowhere (the bus is off). */
  EventPublisher NOOP = event -> {
  };

  /**
   * Publish {@code event} when {@code stage} completes successfully with an affirmative result: any value, except
   * {@code false} and {@code Optional.empty()} (mutations that report "nothing happened" emit nothing). Returns a stage
   * with the identical outcome.
   */
  default <T> CompletionStage<T> announce(CompletionStage<T> stage, AuditEvent event) {
    return stage.whenComplete((result, failure) -> {
      if (failure == null && affirmative(result))
        publish(event);
    });
  }

  /** Multi-event variant of {@link #announce} for one-transaction bundles. */
  default <T> CompletionStage<T> announceAll(CompletionStage<T> stage, Iterable<AuditEvent> events) {
    return stage.whenComplete((result, failure) -> {
      if (failure == null && affirmative(result))
        for (AuditEvent e : events)
          publish(e);
    });
  }

  private static boolean affirmative(Object result) {
    if (result instanceof Boolean b)
      return b;
    if (result instanceof Optional<?> o)
      return o.isPresent();
    return true;
  }
}
