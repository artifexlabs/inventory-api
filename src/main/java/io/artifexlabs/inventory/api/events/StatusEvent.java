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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * An operational outcome a USER should learn about, carrying both faces of the same fact at once (PLAN.md Phase 21): a
 * machine-parsable {@link #code} plus {@link #subject} parameters, and a human-readable {@link #message} plus optional
 * {@link #detail}. One event, two audiences — because the builder demands both at the same call site, they cannot drift
 * apart the way a log line and a metric do.
 *
 * <p>
 * <b>Not audit.</b> {@link AuditEvent}s are committed domain facts, durable, the record of what happened. StatusEvents
 * are operational outcomes — mostly failures and refusals — that are best-effort, transient, and exist so a human hears
 * about a problem without reading log files. A single action may produce both.
 *
 * <p>
 * {@link #id} and {@link #ts} are stamped at PUBLICATION (see {@link StatusPublisher}), not at construction: publish
 * order is the order consumers see, so stamping anywhere else would let ids and timestamps disagree with delivery.
 * Builders therefore leave them null and callers must not depend on them being set before publishing.
 */
public record StatusEvent(String id, Instant ts, Severity severity, String code, String source,
    Map<String, String> subject, String message, String detail, String correlationId, String actor) {

  /** How loudly the consumer should react. */
  public enum Severity {
    /** Something succeeded, or is merely worth knowing. */
    INFO,
    /** Degraded, refused, or skipped — the user's intent did not fully happen. */
    WARNING,
    /** Failed outright; the user's intent did not happen at all. */
    ERROR
  }

  public StatusEvent {
    if (severity == null)
      throw new IllegalArgumentException("severity is required");
    if (code == null || code.isBlank())
      throw new IllegalArgumentException("code is required (the machine face)");
    if (message == null || message.isBlank())
      throw new IllegalArgumentException("message is required (the human face)");
    subject = subject == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(subject));
  }

  public Optional<String> detailText() {
    return Optional.ofNullable(this.detail);
  }

  public Optional<String> correlation() {
    return Optional.ofNullable(this.correlationId);
  }

  public Optional<String> actorId() {
    return Optional.ofNullable(this.actor);
  }

  /** A copy stamped with its publication identity; see the class note on stamping. */
  public StatusEvent stamped(String id, Instant ts) {
    return new StatusEvent(id, ts, this.severity, this.code, this.source, this.subject, this.message, this.detail,
        this.correlationId, this.actor);
  }

  /**
   * Start an event. Both faces are demanded here, together: {@code code} is the stable, namespaced machine key
   * ({@code printer.tape-mismatch}) and {@code message} the sentence a person reads.
   */
  public static Builder of(Severity severity, String code, String message) {
    return new Builder(severity, code, message);
  }

  /** {@link Severity#ERROR} shorthand. */
  public static Builder error(String code, String message) {
    return of(Severity.ERROR, code, message);
  }

  /** {@link Severity#WARNING} shorthand. */
  public static Builder warning(String code, String message) {
    return of(Severity.WARNING, code, message);
  }

  /** {@link Severity#INFO} shorthand. */
  public static Builder info(String code, String message) {
    return of(Severity.INFO, code, message);
  }

  public final static class Builder {
    private final Severity severity;
    private final String code;
    private final String message;
    private final Map<String, String> subject = new LinkedHashMap<>();
    private String source;
    private String detail;
    private String correlationId;
    private String actor;

    private Builder(Severity severity, String code, String message) {
      this.severity = severity;
      this.code = code;
      this.message = message;
    }

    /** The emitting component, e.g. {@code printer.brother}. */
    public Builder source(String source) {
      this.source = source;
      return this;
    }

    /** One structured parameter of the machine face; null values are dropped. */
    public Builder subject(String key, String value) {
      if (key != null && value != null)
        this.subject.put(key, value);
      return this;
    }

    /** Longer human text: what to do about it, what was tried. */
    public Builder detail(String detail) {
      this.detail = detail;
      return this;
    }

    /** The originating request/envelope id, so a frontend can say "YOUR print failed". */
    public Builder correlationId(String correlationId) {
      this.correlationId = correlationId;
      return this;
    }

    /** The user whose action caused this, when attributable — the SSE fan-out scopes on it. */
    public Builder actor(String actor) {
      this.actor = actor;
      return this;
    }

    public StatusEvent build() {
      return new StatusEvent(null, null, this.severity, this.code, this.source, this.subject, this.message, this.detail,
          this.correlationId, this.actor);
    }
  }
}
