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

/**
 * The weight of a physical item. Canonically stored in grams; conversions are
 * derived, never stored, so the canonical value is the single source of truth.
 *
 * @author mykel
 *
 */
public record Weight(double grams) {

  public final static double GRAMS_PER_OUNCE = 28.349523125;
  public final static double GRAMS_PER_POUND = 453.59237;

  public Weight {
    if (grams < 0.0 || !Double.isFinite(grams))
      throw new IllegalArgumentException("weight must be a non-negative finite number of grams: " + grams);
  }

  public static Weight ofKilograms(double kg) {
    return new Weight(kg * 1000.0);
  }

  public static Weight ofOunces(double oz) {
    return new Weight(oz * GRAMS_PER_OUNCE);
  }

  public static Weight ofPounds(double lb) {
    return new Weight(lb * GRAMS_PER_POUND);
  }

  public double toKilograms() {
    return grams / 1000.0;
  }

  public double toOunces() {
    return grams / GRAMS_PER_OUNCE;
  }

  public double toPounds() {
    return grams / GRAMS_PER_POUND;
  }
}
