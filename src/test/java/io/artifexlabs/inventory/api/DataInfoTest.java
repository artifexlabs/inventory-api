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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class DataInfoTest {

  @Test
  public void testJsonRoundTrip() {
    DataInfo original = new DataInfo(MediaKind.REMOTE_STORAGE, true, true);
    assertEquals(original, DataInfo.fromJson(original.toJson()));
  }

  @Test
  public void testNullPassThrough() {
    assertNull(DataInfo.fromJson(null));
  }

  @Test
  public void testKindRequired() {
    assertThrows(NullPointerException.class, () -> new DataInfo(null, true, false));
  }

  @Test
  public void testPlainObjectKind() {
    assertEquals(MediaKind.OBJECT, DataInfo.OBJECT.kind());
    assertEquals(DataInfo.OBJECT, DataInfo.fromJson(DataInfo.OBJECT.toJson()));
  }

  @Test
  public void aLocatorSaysWhereAMediumIsReachedWhenItIsNotInYourHand() {
    DataInfo share = new DataInfo(MediaKind.REMOTE_STORAGE, true, false, "  smb://nas/backups  ");
    assertEquals("smb://nas/backups", share.where().orElse(null), "trimmed, and it round-trips");
    assertEquals(share, DataInfo.fromJson(share.toJson()));

    // a disc you hold needs no locator, and blank is the same as absent
    assertTrue(new DataInfo(MediaKind.PHYSICAL_MEDIA, false, false).where().isEmpty());
    assertTrue(new DataInfo(MediaKind.PHYSICAL_MEDIA, false, false, "   ").where().isEmpty());
    assertFalse(DataInfo.OBJECT.holdsData(), "a crate has no files");
    assertTrue(new DataInfo(MediaKind.PHYSICAL_MEDIA, false, false).holdsData());
  }
}
