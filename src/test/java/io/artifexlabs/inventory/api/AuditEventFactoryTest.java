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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

public class AuditEventFactoryTest {
  private final static Instant TS = Instant.parse("2019-04-01T12:00:00Z");

  @Test
  public void testRoundTrip() {
    AuditEvent original = new DefaultAuditEvent("ae-1", TS, "admin@example.com", "item.create", "item-9",
        new JsonObject().put("name", "wrench"));
    AuditEvent read = AuditEventFactory.deserialize(AuditEventFactory.serialize(original));

    assertEquals("ae-1", read.getId());
    assertEquals(TS, read.getTimestamp());
    assertEquals("admin@example.com", read.getPrincipal());
    assertEquals("item.create", read.getAction());
    assertEquals("item-9", read.getTargetId());
    assertEquals("wrench", read.getDetails().get().getString("name"));
    assertEquals(original, read);
  }

  @Test
  public void testDetailsOptional() {
    AuditEvent read = AuditEventFactory
        .deserialize(AuditEventFactory.serialize(new DefaultAuditEvent("ae-2", TS, "system", "item.delete", "item-9", null)));
    assertTrue(read.getDetails().isEmpty());
  }

  @Test
  public void testNullPassThrough() {
    assertNull(AuditEventFactory.deserialize(null));
  }

  @Test
  public void testRequiredFieldsAreRejected() {
    assertThrows(NullPointerException.class, () -> new DefaultAuditEvent("ae-3", TS, null, "x", "y", null));
    assertThrows(NullPointerException.class, () -> new DefaultAuditEvent("ae-3", null, "p", "x", "y", null));
  }
}
