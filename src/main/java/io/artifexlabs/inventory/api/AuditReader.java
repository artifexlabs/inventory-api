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

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Read access to the audit trail. The trail is append-only; readers never mutate it.
 *
 * @author mykel
 *
 */
public interface AuditReader {

  /** Most recent events first, paged. */
  CompletionStage<List<AuditEvent>> recent(int limit, int offset);

  /** Most recent events first for one target (item, user, or token). */
  CompletionStage<List<AuditEvent>> byTarget(String targetId, int limit);

  /**
   * An event with its commit-order cursor position. {@code seq} is assigned at insert, so it tracks commit order where
   * the ULID {@code id} (assigned pre-commit) may not; consumers page by {@code seq} and re-scan a small overlap
   * (VERTICLES.md).
   */
  record SequencedEvent(long seq, AuditEvent event) {
  }

  /**
   * Events with {@code seq > afterSeq}, oldest first, up to {@code limit} — the replay/catch-up feed for event
   * consumers. Start from 0 for the full history.
   */
  CompletionStage<List<SequencedEvent>> since(long afterSeq, int limit);
}
