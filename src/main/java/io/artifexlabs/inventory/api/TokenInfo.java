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

import static java.util.Objects.requireNonNull;

import java.time.Instant;

import io.vertx.core.json.JsonObject;

/**
 * Metadata about an issued API token.
 *
 * @author mykel
 *
 */
public record TokenInfo(String token, String userId, Instant issuedAt, boolean revoked) {

  public TokenInfo {
    requireNonNull(token, "token");
    requireNonNull(userId, "userId");
    requireNonNull(issuedAt, "issuedAt");
  }

  public JsonObject toJson() {
    return new JsonObject().put("token", token).put("userId", userId).put("issuedAt", issuedAt).put("revoked",
        revoked);
  }

  public static TokenInfo fromJson(JsonObject j) {
    return j == null ? null
        : new TokenInfo(j.getString("token"), j.getString("userId"), j.getInstant("issuedAt"),
            Boolean.TRUE.equals(j.getBoolean("revoked")));
  }
}
