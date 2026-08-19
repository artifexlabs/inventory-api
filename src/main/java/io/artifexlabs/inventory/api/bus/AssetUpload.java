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

/**
 * An asset attach ({@link BusActions#ASSETS_STORE}): the owning item's
 * identifier plus the bytes and their metadata. Bytes cross the bus base64
 * inside the envelope — bus messages are fully buffered, which is accepted
 * for the photo-sized assets this system stores.
 */
public interface AssetUpload extends AssetContent {

  /** The item the new asset attaches to. */
  String itemId();
}
