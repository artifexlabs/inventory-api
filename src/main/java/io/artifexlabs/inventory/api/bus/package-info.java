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
/**
 * The request/reply contract of the Vert.x event-bus service fabric.
 *
 * Every unit of work crossing the bus travels as a {@link io.artifexlabs.inventory.api.bus.BusEnvelope}: the action,
 * its optional target identifier, a typed payload, the acting user (id + principal), the roles asserted for that user,
 * and the shared fabric token. Workers (hosted by inventory-server) verify the token, check the action's required role
 * ({@link io.artifexlabs.inventory.api.bus.BusActions}), do the work through the domain services, and reply. External
 * inputs are authenticated at the HTTP tier (inventory-web-api) before an envelope is ever built; services already on
 * the bus are considered authenticated and speak for themselves.
 *
 * This fabric is request/reply and distinct from {@code inventory.events.*} (see
 * {@link io.artifexlabs.inventory.api.events}), which stays a publish-only stream of after-commit facts.
 */
package io.artifexlabs.inventory.api.bus;
