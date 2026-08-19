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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class UserFactoryTest {

  @Test
  public void testRoundTrip() {
    InventoryUser original = new DefaultInventoryUser("u-1", "admin@example.com", "The Admin", true);
    InventoryUser read = UserFactory.deserialize(UserFactory.serialize(original));

    assertEquals("u-1", read.getId());
    assertEquals("admin@example.com", read.getEmail());
    assertEquals("The Admin", read.getDisplayName().get());
    assertTrue(read.isAdmin());
    assertEquals(original, read);
  }

  @Test
  public void testDisplayNameOptionalAndNonAdminDefault() {
    InventoryUser read = UserFactory
        .deserialize(UserFactory.serialize(new DefaultInventoryUser("u-2", "user@example.com", null, false)));
    assertTrue(read.getDisplayName().isEmpty());
    assertFalse(read.isAdmin());
  }

  @Test
  public void testNullPassThrough() {
    assertNull(UserFactory.deserialize(null));
  }
}
