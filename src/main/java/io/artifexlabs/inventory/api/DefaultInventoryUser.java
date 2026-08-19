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

import java.util.Objects;
import java.util.Optional;

/**
 * Stock immutable {@link InventoryUser}. Identity is the id alone.
 *
 * @author mykel
 *
 */
public final class DefaultInventoryUser implements InventoryUser {
  private final String id;
  private final String email;
  private final String displayName;
  private final boolean admin;

  public DefaultInventoryUser(String id, String email, String displayName, boolean admin) {
    this.id = requireNonNull(id, "id");
    this.email = requireNonNull(email, "email");
    this.displayName = displayName;
    this.admin = admin;
  }

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public String getEmail() {
    return this.email;
  }

  @Override
  public Optional<String> getDisplayName() {
    return Optional.ofNullable(this.displayName);
  }

  @Override
  public boolean isAdmin() {
    return this.admin;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.id);
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || (obj instanceof InventoryUser other && this.id.equals(other.getId()));
  }

  @Override
  public String toString() {
    return UserFactory.serialize(this).encode();
  }
}
