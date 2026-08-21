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
 * A bare box drawn on a picture asset ({@link BusActions#REGIONS_CREATE}): the asset it belongs to plus normalized
 * coordinates and an optional label.
 */
public interface RegionBox {

  String assetId();

  double x();

  double y();

  double w();

  double h();

  /** Optional display label; null when the box is unlabeled. */
  String label();

  JsonObject toJson();
}
