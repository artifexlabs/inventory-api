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

import java.time.Instant;

import io.vertx.core.json.JsonObject;

/**
 * When an item expires, and how seriously to take it.
 *
 * {@code absolute} distinguishes a hard stop — a pharmaceutical's expiry, a certification lapse, a fire extinguisher's
 * service date — from a recommendation like a "best by" date. The flag exists because the two demand different human
 * responses, and only a person can say which one a date is.
 */
public record Expiration(Instant when, boolean absolute) {

  public Expiration {
    if (when == null)
      throw new IllegalArgumentException("an expiration needs an instant");
  }

  /** A soft "best by" style date. */
  public static Expiration recommended(Instant when) {
    return new Expiration(when, false);
  }

  /** A hard stop: past this instant the item must not be used. */
  public static Expiration hard(Instant when) {
    return new Expiration(when, true);
  }

  public boolean isExpiredAt(Instant now) {
    return now.isAfter(this.when);
  }

  public JsonObject toJson() {
    return new JsonObject().put("when", this.when.toString()).put("absolute", this.absolute);
  }

  public static Expiration fromJson(JsonObject j) {
    if (j == null)
      return null;
    return new Expiration(j.getInstant("when"), Boolean.TRUE.equals(j.getBoolean("absolute")));
  }
}
