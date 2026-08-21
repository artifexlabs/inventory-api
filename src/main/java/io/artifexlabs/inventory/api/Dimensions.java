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

import io.vertx.core.json.JsonObject;

/**
 * The physical size of an item as length x width x height. Canonically stored in centimeters; conversions are derived,
 * never stored.
 *
 * @author mykel
 *
 */
public record Dimensions(double lengthCm, double widthCm, double heightCm) {

  public final static double CM_PER_INCH = 2.54;

  public Dimensions {
    validate("length", lengthCm);
    validate("width", widthCm);
    validate("height", heightCm);
  }

  private static void validate(String axis, double v) {
    if (v <= 0.0 || !Double.isFinite(v))
      throw new IllegalArgumentException(axis + " must be a positive finite number of cm: " + v);
  }

  public static Dimensions ofInches(double lengthIn, double widthIn, double heightIn) {
    return new Dimensions(lengthIn * CM_PER_INCH, widthIn * CM_PER_INCH, heightIn * CM_PER_INCH);
  }

  public double lengthInches() {
    return lengthCm / CM_PER_INCH;
  }

  public double widthInches() {
    return widthCm / CM_PER_INCH;
  }

  public double heightInches() {
    return heightCm / CM_PER_INCH;
  }

  public double volumeCubicCm() {
    return lengthCm * widthCm * heightCm;
  }

  public JsonObject toJson() {
    return new JsonObject().put("lengthCm", lengthCm).put("widthCm", widthCm).put("heightCm", heightCm);
  }

  public static Dimensions fromJson(JsonObject j) {
    return j == null ? null : new Dimensions(j.getDouble("lengthCm"), j.getDouble("widthCm"), j.getDouble("heightCm"));
  }
}
