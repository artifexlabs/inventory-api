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

import io.vertx.core.json.JsonObject;

/**
 * One-shot region-to-item creation ({@link BusActions#REGIONS_CREATE_ITEM}): a box on an asset plus the item data it
 * becomes, in one transaction. {@link #containerId()} is nullable — null skips containment.
 */
public interface RegionItemCreation {

  RegionBox box();

  String name();

  String type();

  String containerId();

  JsonObject toJson();
}
