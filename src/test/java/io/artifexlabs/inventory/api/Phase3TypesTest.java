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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

public class Phase3TypesTest {
  private final static Instant TS = Instant.parse("2019-04-01T12:00:00Z");

  @Test
  public void testParValuesValidationAndRoundTrip() {
    ParValues p = new ParValues(2, 10);
    assertEquals(p, ParValues.fromJson(p.toJson()));
    assertNull(ParValues.fromJson(null));
    assertTrue(p.isBelowMin(1));
    assertFalse(p.isBelowMin(2));
    assertThrows(IllegalArgumentException.class, () -> new ParValues(-1, 5));
    assertThrows(IllegalArgumentException.class, () -> new ParValues(6, 5));
  }

  @Test
  public void testParValuesRideItemJson() {
    Item original = DefaultItem.builder().id("P").name("screws").type("hardware").timestamp(TS).quantity(1L)
        .parValues(new ParValues(5, 50)).build();
    Item read = ItemFactory.deserialize(ItemFactory.serialize(original));
    assertEquals(new ParValues(5, 50), read.getParValues().get());
    assertTrue(read.getParValues().get().isBelowMin(read.getQuantity().get()));
    // absent by default, and builder-copy preserves it
    assertTrue(ItemFactory.deserialize(ItemFactory.serialize(
        DefaultItem.builder().id("Q").name("q").timestamp(TS).build())).getParValues().isEmpty());
    assertEquals(ItemFactory.serialize(original),
        ItemFactory.serialize(DefaultItem.builder(original).build()));
  }

  @Test
  public void testAssetInfoRoundTripAndValidation() {
    AssetInfo a = new AssetInfo("as-1", "item-1", "photo.jpg", "image/jpeg", 1234, TS);
    assertEquals(a, AssetInfo.fromJson(a.toJson()));
    assertNull(AssetInfo.fromJson(null));
    assertThrows(NullPointerException.class, () -> new AssetInfo("as-1", "item-1", null, "image/jpeg", 1, TS));
    assertThrows(IllegalArgumentException.class,
        () -> new AssetInfo("as-1", "item-1", "f", "image/jpeg", -1, TS));
  }
}
