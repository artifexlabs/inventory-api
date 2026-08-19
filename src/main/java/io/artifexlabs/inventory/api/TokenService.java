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

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Issues and validates the bearer tokens that guard the API.
 *
 * @author mykel
 *
 */
public interface TokenService {

  /** Resolve a presented bearer token to its user, empty if unknown or revoked. */
  CompletionStage<Optional<InventoryUser>> authenticate(String token);

  /** Issue a new token for the given user, returning the token string. */
  CompletionStage<String> issue(InventoryUser user);

  /** Revoke a token; true if it existed and is now revoked. */
  CompletionStage<Boolean> revoke(String token);

  /** Every token ever issued to a user, newest first, revoked included. */
  CompletionStage<java.util.List<TokenInfo>> tokensFor(String userId);
}
