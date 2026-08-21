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
package io.artifexlabs.inventory.api.bus;

import java.util.Set;

import io.artifexlabs.inventory.api.InventoryUser;

/**
 * The role vocabulary for bus actions. Every action is assigned exactly one required role in {@link BusActions}; an
 * envelope whose asserted roles do not include it is refused with 403 before any work happens.
 */
public final class Roles {

  /** Read the inventory: queries, views, per-target history, downloads. */
  public final static String READ = "inventory.read";

  /** Mutate the inventory: CRUD, containment, uploads, label printing. */
  public final static String WRITE = "inventory.write";

  /** Administer users, tokens, and the global audit feed. */
  public final static String ADMIN = "inventory.admin";

  private Roles() {
  }

  /**
   * The roles an authenticated user holds. Every user reads and writes; admins administer. This is the single place the
   * user→role mapping lives.
   */
  public final static Set<String> rolesFor(InventoryUser user) {
    return user.isAdmin() ? Set.of(READ, WRITE, ADMIN) : Set.of(READ, WRITE);
  }
}
