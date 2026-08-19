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
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

public class ItemFactoryTest {
  private final static Instant TS = Instant.parse("2019-04-01T12:00:00Z");

  private static Item leaf(String id) {
    return new DefaultItem(id, "name-" + id, null, "sometype", TS, null);
  }

  @Test
  public void testRoundTripOfLeafItem() {
    Item original = leaf("A");
    Item read = ItemFactory.deserialize(ItemFactory.serialize(original));

    assertEquals("A", read.getId());
    assertEquals("name-A", read.getName());
    assertEquals("sometype", read.getType());
    assertEquals(TS, read.getTimestamp());
    assertFalse(read.isContainer());
    assertTrue(read.getContainedItems().isEmpty());
  }

  @Test
  public void testRoundTripOfContainer() {
    Item original = new DefaultItem("box", "a box", "The Box", "container", TS,
        Set.of(leaf("A"), leaf("B")));
    Item read = ItemFactory.deserialize(ItemFactory.serialize(original));

    assertTrue(read.isContainer());
    assertEquals("The Box", read.getDisplayName().get());
    assertEquals(Set.of(leaf("A"), leaf("B")), read.getContainedItems().get());
    assertEquals(ItemFactory.serialize(original), ItemFactory.serialize(read));
  }

  @Test
  public void testDisplayNameDefaultsToName() {
    assertEquals("name-A", ItemFactory.deserialize(ItemFactory.serialize(leaf("A"))).getDisplayName().get());
  }

  @Test
  public void testNullsPassThrough() {
    assertNull(ItemFactory.deserialize(null));
  }

  @Test
  public void testRequiredFieldsAreRejected() {
    assertThrows(NullPointerException.class, () -> ItemFactory.deserialize(new JsonObject().put("name", "x")));
  }

  @Test
  public void testRoundTripOfExtendedFields() {
    Item original = DefaultItem.builder().id("D").name("backup disk").type("data").timestamp(TS)
        .description("Offsite backup drive").containerId("loc-1")
        .dataInfo(new DataInfo(MediaKind.PHYSICAL_MEDIA, true, false)).build();
    Item read = ItemFactory.deserialize(ItemFactory.serialize(original));

    assertEquals("Offsite backup drive", read.getDescription().get());
    assertEquals("loc-1", read.getContainerId().get());
    assertEquals(new DataInfo(MediaKind.PHYSICAL_MEDIA, true, false), read.getDataInfo().get());
    assertEquals(ItemFactory.serialize(original), ItemFactory.serialize(read));
  }

  @Test
  public void testExtendedFieldsAbsentByDefault() {
    Item read = ItemFactory.deserialize(ItemFactory.serialize(leaf("A")));
    assertTrue(read.getDescription().isEmpty());
    assertTrue(read.getContainerId().isEmpty());
    assertTrue(read.getDataInfo().isEmpty());
  }

  @Test
  public void testRoundTripOfPhysicalAttributes() {
    Item original = DefaultItem.builder().id("W").name("brick").type("masonry").timestamp(TS).quantity(250L)
        .weight(Weight.ofKilograms(2.0)).dimensions(new Dimensions(20.0, 10.0, 7.5)).build();
    Item read = ItemFactory.deserialize(ItemFactory.serialize(original));

    assertEquals(250L, read.getQuantity().get());
    assertEquals(2000.0, read.getWeight().get().grams());
    assertEquals(new Dimensions(20.0, 10.0, 7.5), read.getDimensions().get());
    assertEquals(ItemFactory.serialize(original), ItemFactory.serialize(read));
  }

  @Test
  public void testPhysicalAttributesAbsentByDefault() {
    Item read = ItemFactory.deserialize(ItemFactory.serialize(leaf("A")));
    assertTrue(read.getQuantity().isEmpty());
    assertTrue(read.getWeight().isEmpty());
    assertTrue(read.getDimensions().isEmpty());
  }

  @Test
  public void testNegativeQuantityRejected() {
    assertThrows(IllegalArgumentException.class,
        () -> DefaultItem.builder().id("Q").name("q").timestamp(TS).quantity(-1L).build());
  }

  @Test
  public void testBuilderCopiesAllFields() {
    Item original = DefaultItem.builder().id("D").name("cdrom").type("data").timestamp(TS).displayName("CD-ROM")
        .description("write-once").dataInfo(new DataInfo(MediaKind.PHYSICAL_MEDIA, false, true)).quantity(3L)
        .weight(new Weight(15.5)).dimensions(Dimensions.ofInches(4.724, 4.724, 0.047))
        .containedItems(Set.of(leaf("A"))).build();
    Item copy = DefaultItem.builder(original).build();
    assertEquals(ItemFactory.serialize(original), ItemFactory.serialize(copy));
  }
}
