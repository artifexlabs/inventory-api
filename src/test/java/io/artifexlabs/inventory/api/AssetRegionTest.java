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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

/** Region shape rules, dots included (ongoing item 8). */
public class AssetRegionTest {

  private final static Instant TS = Instant.parse("2026-08-15T00:00:00Z");

  private static AssetRegion region(double x, double y, double w, double h) {
    return new AssetRegion("r-1", "a-1", x, y, w, h, null, null, TS);
  }

  @Test
  public void boxesNeedPositiveSides() {
    assertFalse(region(0.1, 0.2, 0.3, 0.4).isDot());
    assertThrows(IllegalArgumentException.class, () -> region(0.1, 0.2, -0.1, 0.4));
    assertThrows(IllegalArgumentException.class, () -> region(0.1, 0.2, 0.3, -0.1));
    assertThrows(IllegalArgumentException.class, () -> region(0.9, 0.9, 0.5, 0.05), "exceeds the image");
  }

  @Test
  public void zeroByZeroIsADot() {
    AssetRegion dot = region(0.5, 0.25, 0, 0);
    assertTrue(dot.isDot());
    assertEquals(0.0, dot.toJson().getDouble("w"));
    // an edge dot is fine — a point at 1.0 is still inside the image
    assertTrue(region(1.0, 1.0, 0, 0).isDot());
  }

  @Test
  public void mixedZeroIsNeitherBoxNorDot() {
    assertThrows(IllegalArgumentException.class, () -> region(0.1, 0.2, 0, 0.4));
    assertThrows(IllegalArgumentException.class, () -> region(0.1, 0.2, 0.3, 0));
  }
}
