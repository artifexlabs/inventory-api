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

import java.util.Map;
import java.util.Optional;

/**
 * The complete vocabulary of work the bus carries, each action bound to the service address that answers it and the
 * {@link Roles role} it demands. Requests are {@code request/reply} (point-to-point) — distinct from the after-commit
 * fact events of {@code inventory.events.*}, which remain publish-only announcements.
 *
 * Failure replies use HTTP-aligned failure codes (400, 401, 403, 404, 409, 503) so the HTTP gateway maps them
 * one-to-one.
 */
public final class BusActions {

  /** Prefix of every request/reply service address. */
  public final static String SERVICE_PREFIX = "inventory.svc.";

  // ---- items (CRUD + containment) ----------------------------------------
  public final static String ITEMS_LIST = "items.list";
  public final static String ITEMS_LIST_OF_TYPE = "items.list-of-type";
  public final static String ITEMS_GET = "items.get";
  public final static String ITEMS_CREATE = "items.create";
  public final static String ITEMS_UPDATE = "items.update";
  public final static String ITEMS_DELETE = "items.delete";
  /** The item's single container, or empty when it is a root. */
  public final static String ITEMS_CONTAINER_OF = "items.container-of";
  public final static String ITEMS_CONTAIN = "items.contain";
  public final static String ITEMS_UNCONTAIN = "items.uncontain";
  public final static String ITEMS_MOVE = "items.move";
  /** Own coordinates, else the nearest pinned ancestor's. */
  public final static String ITEMS_COORDINATES = "items.coordinates";
  public final static String ITEMS_TAG = "items.tag";
  public final static String ITEMS_UNTAG = "items.untag";
  public final static String ITEMS_FIND_BY_TAG = "items.find-by-tag";
  /** Physical markers (NFC UID, UPC, foreign QR) resolving to items. */
  public final static String ITEMS_IDENTITY_ADD = "items.identity-add";
  public final static String ITEMS_IDENTITY_REMOVE = "items.identity-remove";
  public final static String ITEMS_FIND_BY_IDENTITY = "items.find-by-identity";
  public final static String ITEMS_IDENTITIES_OF = "items.identities-of";

  // ---- assets (attachments) ----------------------------------------------
  public final static String ASSETS_STORE = "assets.store";
  /** A picture that IS a thing: create the item + attach the photo, one tx. */
  public final static String ASSETS_CREATE_ITEM = "assets.create-item";
  /** Swap an asset's content in place; the old bytes are archived in audit. */
  public final static String ASSETS_REPLACE = "assets.replace";
  public final static String ASSETS_GET = "assets.get";
  public final static String ASSETS_LIST_FOR = "assets.list-for";
  public final static String ASSETS_DELETE = "assets.delete";

  // ---- regions (spatial annotation) --------------------------------------
  public final static String REGIONS_LIST = "regions.list";
  public final static String REGIONS_CREATE = "regions.create";
  public final static String REGIONS_DELETE = "regions.delete";
  public final static String REGIONS_CREATE_ITEM = "regions.create-item";
  public final static String REGIONS_MAKE_ITEM = "regions.make-item";

  // ---- data media (manifests: what files are on a disc or a share) -------
  /** Replace a medium's whole manifest; archives become contained items. */
  public final static String DATA_REPLACE_MANIFEST = "data.replace-manifest";
  /** Correct a path without re-hashing the medium; renames descendants too. */
  public final static String DATA_RENAME_PATH = "data.rename-path";
  public final static String DATA_ENTRIES = "data.entries";
  public final static String DATA_SUMMARY = "data.summary";
  /** "Which disc has this file?" — the inventory question, asked of data. */
  public final static String DATA_BY_HASH = "data.by-hash";
  /** How much each other medium has in common with this one — four numbers and two flags, not a wall of rows. */
  public final static String DATA_OVERLAP = "data.overlap";
  /** What this medium could not read, and which sibling media can supply it. */
  public final static String DATA_REPAIRS = "data.repairs";
  /** Recompute this medium's directory rollup. A write: it rewrites every directory row the medium owns. */
  public final static String DATA_ROLLUP = "data.rollup";
  /** How far along hashing this medium is — the progress bar for a job measured in weeks. */
  public final static String DATA_PROGRESS = "data.progress";
  /**
   * Subtrees that occur in more than one place. Unlike every other data action, the target id is OPTIONAL: with one the
   * answer is "where else is this medium's content", without one it is the whole inventory's duplication.
   */
  public final static String DATA_SECTIONS = "data.sections";

  // ---- catalog (external UPC lookup — prefill, never a dependency) -------
  public final static String CATALOG_UPC = "catalog.upc";
  /** One-shot: catalog prefill + item + identity + tags + image asset. */
  public final static String CATALOG_CREATE_ITEM = "catalog.create-item";

  // ---- audit trail -------------------------------------------------------
  public final static String AUDIT_RECENT = "audit.recent";
  public final static String AUDIT_BY_TARGET = "audit.by-target";

  // ---- labels (QR render + physical print) -------------------------------
  public final static String LABELS_QR = "labels.qr";
  public final static String LABELS_PRINT = "labels.print";
  /** Feed blank tape and cut — reclaims the leader. */
  public final static String LABELS_FEED = "labels.feed";
  /** A whole run as ONE printer job so the labels share one leader. */
  public final static String LABELS_PRINT_BATCH = "labels.print-batch";

  // ---- users + tokens (admin) --------------------------------------------
  public final static String USERS_LIST = "users.list";
  public final static String USERS_CREATE = "users.create";
  public final static String USERS_DELETE = "users.delete";
  public final static String USERS_SET_ADMIN = "users.set-admin";
  public final static String TOKENS_FOR_USER = "tokens.for-user";
  public final static String TOKENS_REVOKE = "tokens.revoke";

  // ---- authentication (pre-auth: fabric token only, no user yet) ---------
  public final static String AUTH_LOGIN = "auth.login";
  public final static String AUTH_TOKEN = "auth.token";
  public final static String AUTH_REVOKE = "auth.revoke";
  public final static String AUTH_EXCHANGE = "auth.exchange";

  /**
   * action → required role. Auth actions map to no role: they run before a user exists and are gated by the fabric
   * token alone.
   */
  private final static Map<String, Optional<String>> REQUIRED_ROLE = Map.ofEntries(
      Map.entry(ITEMS_LIST, Optional.of(Roles.READ)), Map.entry(ITEMS_LIST_OF_TYPE, Optional.of(Roles.READ)),
      Map.entry(ITEMS_GET, Optional.of(Roles.READ)), Map.entry(ITEMS_CREATE, Optional.of(Roles.WRITE)),
      Map.entry(ITEMS_UPDATE, Optional.of(Roles.WRITE)), Map.entry(ITEMS_DELETE, Optional.of(Roles.WRITE)),
      Map.entry(ITEMS_CONTAINER_OF, Optional.of(Roles.READ)), Map.entry(ITEMS_CONTAIN, Optional.of(Roles.WRITE)),
      Map.entry(ITEMS_UNCONTAIN, Optional.of(Roles.WRITE)), Map.entry(ITEMS_MOVE, Optional.of(Roles.WRITE)),
      Map.entry(ITEMS_COORDINATES, Optional.of(Roles.READ)), Map.entry(ITEMS_TAG, Optional.of(Roles.WRITE)),
      Map.entry(ITEMS_UNTAG, Optional.of(Roles.WRITE)), Map.entry(ITEMS_FIND_BY_TAG, Optional.of(Roles.READ)),
      Map.entry(ITEMS_IDENTITY_ADD, Optional.of(Roles.WRITE)),
      Map.entry(ITEMS_IDENTITY_REMOVE, Optional.of(Roles.WRITE)),
      Map.entry(ITEMS_FIND_BY_IDENTITY, Optional.of(Roles.READ)),
      Map.entry(ITEMS_IDENTITIES_OF, Optional.of(Roles.READ)), Map.entry(ASSETS_REPLACE, Optional.of(Roles.WRITE)),
      Map.entry(ASSETS_CREATE_ITEM, Optional.of(Roles.WRITE)), Map.entry(ASSETS_STORE, Optional.of(Roles.WRITE)),
      Map.entry(ASSETS_GET, Optional.of(Roles.READ)), Map.entry(ASSETS_LIST_FOR, Optional.of(Roles.READ)),
      Map.entry(ASSETS_DELETE, Optional.of(Roles.WRITE)), Map.entry(REGIONS_LIST, Optional.of(Roles.READ)),
      Map.entry(REGIONS_CREATE, Optional.of(Roles.WRITE)), Map.entry(REGIONS_DELETE, Optional.of(Roles.WRITE)),
      Map.entry(REGIONS_CREATE_ITEM, Optional.of(Roles.WRITE)), Map.entry(REGIONS_MAKE_ITEM, Optional.of(Roles.WRITE)),
      Map.entry(DATA_REPLACE_MANIFEST, Optional.of(Roles.WRITE)), Map.entry(DATA_RENAME_PATH, Optional.of(Roles.WRITE)),
      Map.entry(DATA_ENTRIES, Optional.of(Roles.READ)), Map.entry(DATA_SUMMARY, Optional.of(Roles.READ)),
      Map.entry(DATA_BY_HASH, Optional.of(Roles.READ)), Map.entry(DATA_OVERLAP, Optional.of(Roles.READ)),
      Map.entry(DATA_REPAIRS, Optional.of(Roles.READ)), Map.entry(DATA_ROLLUP, Optional.of(Roles.WRITE)),
      Map.entry(DATA_PROGRESS, Optional.of(Roles.READ)), Map.entry(DATA_SECTIONS, Optional.of(Roles.READ)),
      Map.entry(CATALOG_UPC, Optional.of(Roles.READ)), Map.entry(CATALOG_CREATE_ITEM, Optional.of(Roles.WRITE)),
      Map.entry(AUDIT_RECENT, Optional.of(Roles.ADMIN)), Map.entry(AUDIT_BY_TARGET, Optional.of(Roles.READ)),
      Map.entry(LABELS_QR, Optional.of(Roles.READ)), Map.entry(LABELS_PRINT, Optional.of(Roles.WRITE)),
      Map.entry(LABELS_FEED, Optional.of(Roles.WRITE)), Map.entry(LABELS_PRINT_BATCH, Optional.of(Roles.WRITE)),
      Map.entry(USERS_LIST, Optional.of(Roles.ADMIN)), Map.entry(USERS_CREATE, Optional.of(Roles.ADMIN)),
      Map.entry(USERS_DELETE, Optional.of(Roles.ADMIN)), Map.entry(USERS_SET_ADMIN, Optional.of(Roles.ADMIN)),
      Map.entry(TOKENS_FOR_USER, Optional.of(Roles.ADMIN)), Map.entry(TOKENS_REVOKE, Optional.of(Roles.ADMIN)),
      Map.entry(AUTH_LOGIN, Optional.empty()), Map.entry(AUTH_TOKEN, Optional.empty()),
      Map.entry(AUTH_REVOKE, Optional.empty()), Map.entry(AUTH_EXCHANGE, Optional.empty()));

  private BusActions() {
  }

  /** Whether the action is part of the vocabulary at all. */
  public final static boolean known(String action) {
    return action != null && REQUIRED_ROLE.containsKey(action);
  }

  /**
   * The role the action demands; empty for the pre-auth {@code auth.*} actions. Unknown actions throw — validate with
   * {@link #known} first.
   */
  public final static Optional<String> requiredRole(String action) {
    Optional<String> role = REQUIRED_ROLE.get(action);
    if (role == null)
      throw new IllegalArgumentException("unknown bus action: " + action);
    return role;
  }

  /**
   * The service address answering an action: {@code inventory.svc.<prefix>} where prefix is the action's segment before
   * the first dot ({@code
   * items.create} → {@code inventory.svc.items}).
   */
  public final static String addressOf(String action) {
    if (!known(action))
      throw new IllegalArgumentException("unknown bus action: " + action);
    return SERVICE_PREFIX + action.substring(0, action.indexOf('.'));
  }
}
