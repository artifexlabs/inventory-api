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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.artifexlabs.inventory.api.events.StatusEvent.Severity;

public class StatusEventTest {

  @Test
  public void bothFacesAreMandatory() {
    // the whole point of the type: a machine key and a human sentence,
    // demanded at the SAME call site so they cannot drift apart
    assertThrows(IllegalArgumentException.class,
        () -> new StatusEvent(null, null, Severity.ERROR, null, "s", null, "readable", null, null, null),
        "no code -> no machine face");
    assertThrows(IllegalArgumentException.class,
        () -> new StatusEvent(null, null, Severity.ERROR, "a.code", "s", null, "  ", null, null, null),
        "no message -> no human face");
    assertThrows(IllegalArgumentException.class,
        () -> new StatusEvent(null, null, null, "a.code", "s", null, "readable", null, null, null),
        "severity is required");
  }

  @Test
  public void builderLeavesIdentityForThePublisherToStamp() {
    StatusEvent e = StatusEvent.error("printer.tape-mismatch", "Label refused: wrong tape.").source("printer.brother")
        .subject("itemId", "01ARZ3NDEKTSV4RRFFQ69G5FAV").build();
    assertNull(e.id(), "id is stamped at publication, not construction");
    assertNull(e.ts(), "ts is stamped at publication, not construction");

    StatusEvent stamped = e.stamped("01M0STAMPED", Instant.parse("2026-08-21T14:03:22.123456Z"));
    assertEquals("01M0STAMPED", stamped.id());
    assertEquals("printer.tape-mismatch", stamped.code(), "stamping preserves the payload");
    assertEquals("Label refused: wrong tape.", stamped.message());
    assertNull(e.id(), "stamping is a copy — the original is untouched");
  }

  @Test
  public void subjectIsImmutableAndSkipsNulls() {
    StatusEvent e = StatusEvent.warning("bus.forbidden", "Not permitted.").subject("action", "items.delete")
        .subject("requiredRole", null).build();
    assertEquals(1, e.subject().size(), "null values are dropped, not stored");
    assertThrows(UnsupportedOperationException.class, () -> e.subject().put("x", "y"));
  }

  @Test
  public void wireRoundTripsEverySeverityAndField() {
    for (Severity severity : Severity.values()) {
      StatusEvent original = StatusEvent.of(severity, "printer.no-scannable-qr", "No code fits the tape.")
          .source("printer.brother").subject("itemId", "01ARZ").subject("tapeMm", "9").detail("Use wider tape.")
          .correlationId("req-1").actor("01USER").build()
          .stamped("01M0EVENT", Instant.parse("2026-08-21T14:03:22.123456Z"));

      StatusEvent back = StatusEvents.fromWire(StatusEvents.toWire(original));
      assertEquals(original, back, severity + " must survive the wire unchanged");
    }
  }

  @Test
  public void wireCarriesTheSchemaVersionAndSeverityAddress() {
    StatusEvent e = StatusEvent.info("printer.printed", "Label printed.").build().stamped("01M0", Instant.EPOCH);
    assertEquals(StatusEvents.VERSION, StatusEvents.toWire(e).getInteger("v"));
    assertEquals("status.events.error", StatusEvents.severityAddress(Severity.ERROR));
    assertEquals("status.events.warning", StatusEvents.severityAddress(Severity.WARNING));
    assertTrue(StatusEvents.severityAddress(Severity.INFO).startsWith(StatusEvents.ADDRESS));
  }

  @Test
  public void unstampedEventsStillSerialize() {
    // publishing paths stamp first, but a dropped/logged event must not NPE
    StatusEvent e = StatusEvent.error("bus.bad-fabric-token", "Rejected.").source("bus.guard").build();
    StatusEvent back = StatusEvents.fromWire(StatusEvents.toWire(e));
    assertNull(back.id());
    assertNull(back.ts());
    assertEquals("bus.bad-fabric-token", back.code());
  }
}
