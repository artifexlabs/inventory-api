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

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * The stock immutable {@link Item} implementation, and the type produced by
 * {@link ItemFactory#deserialize(JsonObject)}.
 *
 * Identity is the id alone, so that an Item within a containing Set remains the same Item across updates to its other
 * properties.
 *
 * @author mykel
 *
 */
public final class DefaultItem implements Item {
  private final String id;
  private final String name;
  private final String displayName;
  private final String type;
  private final Instant timestamp;
  private final String description;
  private final String containerId;
  private final LatLong coordinates;
  private final boolean heavy;
  private final Expiration expiration;
  private final Set<ItemTag> tags;
  private final DataInfo dataInfo;
  private final Long quantity;
  private final Weight weight;
  private final Dimensions dimensions;
  private final ParValues parValues;
  private final Set<Item> containedItems;

  public static Builder builder() {
    return new Builder();
  }

  public static Builder builder(Item source) {
    Builder b = new Builder().id(source.getId()).name(source.getName()).type(source.getType())
        .timestamp(source.getTimestamp()).heavy(source.isHeavy()).tags(source.getTags());
    source.getDisplayName().ifPresent(b::displayName);
    source.getDescription().ifPresent(b::description);
    source.getContainerId().ifPresent(b::containerId);
    source.getCoordinates().ifPresent(b::coordinates);
    source.getExpiration().ifPresent(b::expiration);
    source.getDataInfo().ifPresent(b::dataInfo);
    source.getQuantity().ifPresent(b::quantity);
    source.getWeight().ifPresent(b::weight);
    source.getDimensions().ifPresent(b::dimensions);
    source.getParValues().ifPresent(b::parValues);
    source.getContainedItems().ifPresent(b::containedItems);
    return b;
  }

  public DefaultItem(String id, String name, String displayName, String type, Instant timestamp,
      Set<Item> containedItems)
  {
    this(id, name, displayName, type, timestamp, null, null, null, false, null, null, null, null, null, null, null,
        containedItems);
  }

  private DefaultItem(String id, String name, String displayName, String type, Instant timestamp, String description,
      String containerId, LatLong coordinates, boolean heavy, Expiration expiration, Set<ItemTag> tags,
      DataInfo dataInfo, Long quantity, Weight weight, Dimensions dimensions, ParValues parValues,
      Set<Item> containedItems)
  {
    this.id = requireNonNull(id, "id");
    this.name = requireNonNull(name, "name");
    this.displayName = displayName;
    this.type = Optional.ofNullable(type).orElse(InventoryConstants.DEFAULT_TYPE);
    this.timestamp = requireNonNull(timestamp, "timestamp");
    this.description = description;
    if (containerId != null && containerId.equals(id))
      throw new IllegalArgumentException("an item cannot contain itself: " + id);
    this.containerId = containerId;
    this.coordinates = coordinates;
    this.heavy = heavy;
    this.expiration = expiration;
    // sorted + immutable: tag order must not depend on insertion accident
    this.tags = tags == null || tags.isEmpty() ? Set.of() : java.util.Collections.unmodifiableSet(new TreeSet<>(tags));
    this.dataInfo = dataInfo;
    if (quantity != null && quantity < 0)
      throw new IllegalArgumentException("quantity must be non-negative: " + quantity);
    this.quantity = quantity;
    this.weight = weight;
    this.dimensions = dimensions;
    this.parValues = parValues;
    this.containedItems = containedItems == null ? null : Set.copyOf(containedItems);
  }

  /**
   * Read an item back from the form written by {@link ItemFactory#serialize(Item)}.
   *
   * @param j
   */
  public DefaultItem(JsonObject j) {
    this(j.getString("id"), j.getString("name"), j.getString("displayName"), j.getString("type"),
        j.getInstant("timestamp"), j.getString("description"), j.getString("containerId"), coordinatesFrom(j),
        Boolean.TRUE.equals(j.getBoolean("heavy")), Expiration.fromJson(j.getJsonObject("expiration")),
        tagsFrom(j.getJsonArray("tags")), DataInfo.fromJson(j.getJsonObject("dataInfo")), j.getLong("quantity"),
        j.getDouble("weightGrams") == null ? null : new Weight(j.getDouble("weightGrams")),
        Dimensions.fromJson(j.getJsonObject("dimensionsCm")), ParValues.fromJson(j.getJsonObject("parValues")),
        containedItemsFrom(j.getJsonArray("containedItems")));
  }

  private final static LatLong coordinatesFrom(JsonObject j) {
    return j.getDouble("latitude") == null || j.getDouble("longitude") == null ? null
        : new LatLong(j.getDouble("latitude"), j.getDouble("longitude"));
  }

  private final static Set<ItemTag> tagsFrom(JsonArray a) {
    if (a == null)
      return Set.of();
    Set<ItemTag> s = new TreeSet<>();
    for (int i = 0; i < a.size(); ++i)
      s.add(ItemTag.fromJson(a.getJsonObject(i)));
    return s;
  }

  private final static Set<Item> containedItemsFrom(JsonArray a) {
    if (a == null)
      return null;
    Set<Item> s = new LinkedHashSet<>();
    for (int i = 0; i < a.size(); ++i)
      s.add(ItemFactory.deserialize(a.getJsonObject(i)));
    return s;
  }

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public Optional<String> getDisplayName() {
    return Optional.ofNullable(this.displayName == null ? this.name : this.displayName);
  }

  @Override
  public String getType() {
    return this.type;
  }

  @Override
  public Instant getTimestamp() {
    return this.timestamp;
  }

  @Override
  public Optional<String> getDescription() {
    return Optional.ofNullable(this.description);
  }

  @Override
  public Optional<String> getContainerId() {
    return Optional.ofNullable(this.containerId);
  }

  @Override
  public Optional<LatLong> getCoordinates() {
    return Optional.ofNullable(this.coordinates);
  }

  @Override
  public boolean isHeavy() {
    return this.heavy;
  }

  @Override
  public Optional<Expiration> getExpiration() {
    return Optional.ofNullable(this.expiration);
  }

  @Override
  public Set<ItemTag> getTags() {
    return this.tags;
  }

  @Override
  public Optional<DataInfo> getDataInfo() {
    return Optional.ofNullable(this.dataInfo);
  }

  @Override
  public Optional<Long> getQuantity() {
    return Optional.ofNullable(this.quantity);
  }

  @Override
  public Optional<Weight> getWeight() {
    return Optional.ofNullable(this.weight);
  }

  @Override
  public Optional<Dimensions> getDimensions() {
    return Optional.ofNullable(this.dimensions);
  }

  @Override
  public Optional<ParValues> getParValues() {
    return Optional.ofNullable(this.parValues);
  }

  @Override
  public Optional<Set<Item>> getContainedItems() {
    return Optional.ofNullable(this.containedItems);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.id);
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || (obj instanceof Item other && this.id.equals(other.getId()));
  }

  @Override
  public String toString() {
    return ItemFactory.serialize(this).encode();
  }

  public final static class Builder {
    private String id;
    private String name;
    private String displayName;
    private String type;
    private Instant timestamp;
    private String description;
    private String containerId;
    private LatLong coordinates;
    private boolean heavy;
    private Expiration expiration;
    private Set<ItemTag> tags;
    private DataInfo dataInfo;
    private Long quantity;
    private Weight weight;
    private Dimensions dimensions;
    private ParValues parValues;
    private Set<Item> containedItems;

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder displayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder type(String type) {
      this.type = type;
      return this;
    }

    public Builder timestamp(Instant timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder containerId(String containerId) {
      this.containerId = containerId;
      return this;
    }

    public Builder coordinates(LatLong coordinates) {
      this.coordinates = coordinates;
      return this;
    }

    public Builder heavy(boolean heavy) {
      this.heavy = heavy;
      return this;
    }

    public Builder expiration(Expiration expiration) {
      this.expiration = expiration;
      return this;
    }

    public Builder tags(Set<ItemTag> tags) {
      this.tags = tags;
      return this;
    }

    public Builder dataInfo(DataInfo dataInfo) {
      this.dataInfo = dataInfo;
      return this;
    }

    public Builder quantity(long quantity) {
      this.quantity = quantity;
      return this;
    }

    public Builder weight(Weight weight) {
      this.weight = weight;
      return this;
    }

    public Builder dimensions(Dimensions dimensions) {
      this.dimensions = dimensions;
      return this;
    }

    public Builder parValues(ParValues parValues) {
      this.parValues = parValues;
      return this;
    }

    public Builder containedItems(Set<Item> containedItems) {
      this.containedItems = containedItems;
      return this;
    }

    public DefaultItem build() {
      return new DefaultItem(id, name, displayName, type, timestamp, description, containerId, coordinates, heavy,
          expiration, tags, dataInfo, quantity, weight, dimensions, parValues, containedItems);
    }
  }
}
