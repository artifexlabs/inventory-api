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
 * What kind of thing an {@link Item} is. Most inventory is plain physical objects; the remaining kinds describe the
 * medium data lives on.
 *
 * @author mykel
 *
 */
public enum MediaKind {
  /** A plain physical object that is not (and does not carry) data. */
  OBJECT,
  /** Data on physical media in inventory (CD, disk, tape). */
  PHYSICAL_MEDIA,
  /** Data on remote, network, or cloud storage. */
  REMOTE_STORAGE;
}
