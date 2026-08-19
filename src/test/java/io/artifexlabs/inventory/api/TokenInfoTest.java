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

import java.time.Instant;

import org.junit.jupiter.api.Test;

public class TokenInfoTest {
  private final static Instant TS = Instant.parse("2019-04-01T12:00:00Z");

  @Test
  public void testJsonRoundTrip() {
    TokenInfo original = new TokenInfo("tok-1", "u-1", TS, true);
    assertEquals(original, TokenInfo.fromJson(original.toJson()));
    assertNull(TokenInfo.fromJson(null));
  }

  @Test
  public void testRequiredFields() {
    assertThrows(NullPointerException.class, () -> new TokenInfo(null, "u", TS, false));
    assertThrows(NullPointerException.class, () -> new TokenInfo("t", "u", null, false));
  }
}
