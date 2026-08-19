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

import org.junit.jupiter.api.Test;

public class UnitsTest {
  private final static double EPS = 1e-9;

  @Test
  public void testWeightConversions() {
    assertEquals(1000.0, Weight.ofKilograms(1.0).grams(), EPS);
    assertEquals(1.0, new Weight(1000.0).toKilograms(), EPS);
    assertEquals(453.59237, Weight.ofPounds(1.0).grams(), EPS);
    assertEquals(1.0, Weight.ofPounds(1.0).toPounds(), EPS);
    assertEquals(16.0, Weight.ofPounds(1.0).toOunces(), 1e-6);
    assertEquals(28.349523125, Weight.ofOunces(1.0).grams(), EPS);
  }

  @Test
  public void testWeightValidation() {
    assertThrows(IllegalArgumentException.class, () -> new Weight(-1.0));
    assertThrows(IllegalArgumentException.class, () -> new Weight(Double.NaN));
    assertThrows(IllegalArgumentException.class, () -> new Weight(Double.POSITIVE_INFINITY));
  }

  @Test
  public void testDimensionConversions() {
    Dimensions d = Dimensions.ofInches(1.0, 2.0, 3.0);
    assertEquals(2.54, d.lengthCm(), EPS);
    assertEquals(5.08, d.widthCm(), EPS);
    assertEquals(7.62, d.heightCm(), EPS);
    assertEquals(1.0, d.lengthInches(), EPS);
    assertEquals(2.0, d.widthInches(), EPS);
    assertEquals(3.0, d.heightInches(), EPS);
    assertEquals(2.54 * 5.08 * 7.62, d.volumeCubicCm(), EPS);
  }

  @Test
  public void testDimensionValidation() {
    assertThrows(IllegalArgumentException.class, () -> new Dimensions(0.0, 1.0, 1.0));
    assertThrows(IllegalArgumentException.class, () -> new Dimensions(1.0, -1.0, 1.0));
    assertThrows(IllegalArgumentException.class, () -> new Dimensions(1.0, 1.0, Double.NaN));
  }

  @Test
  public void testDimensionsJsonRoundTrip() {
    Dimensions d = new Dimensions(20.0, 10.0, 7.5);
    assertEquals(d, Dimensions.fromJson(d.toJson()));
    assertNull(Dimensions.fromJson(null));
  }
}
