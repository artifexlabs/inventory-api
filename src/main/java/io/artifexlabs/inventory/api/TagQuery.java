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

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import io.vertx.core.json.JsonObject;

/**
 * A tag search: match a key, and optionally its value, by exact text, glob, or regex. A null {@code valuePattern} means
 * "any value, or none" — i.e. existence of the key.
 *
 * Matching is compiled to a {@link Pattern} in every mode so implementations share one semantic; EXACT quotes the
 * input, GLOB translates {@code *} and {@code ?} and quotes everything else. Case-insensitive throughout, since humans
 * type tags by hand.
 */
public record TagQuery(String keyPattern, String valuePattern, Mode mode) {

  public enum Mode {
    EXACT, GLOB, REGEX
  }

  public TagQuery {
    if (keyPattern == null || keyPattern.isBlank())
      throw new IllegalArgumentException("a tag query needs a key pattern");
    mode = mode == null ? Mode.EXACT : mode;
  }

  /** Items carrying this exact key, whatever its value. */
  public static TagQuery key(String key) {
    return new TagQuery(key, null, Mode.EXACT);
  }

  public Pattern keyMatcher() {
    return compile(this.keyPattern);
  }

  /** Null when the query does not constrain the value. */
  public Pattern valueMatcher() {
    return this.valuePattern == null ? null : compile(this.valuePattern);
  }

  /** True when the tag satisfies both halves of this query. */
  public boolean matches(ItemTag tag) {
    if (!keyMatcher().matcher(tag.key()).matches())
      return false;
    Pattern v = valueMatcher();
    if (v == null)
      return true;
    return tag.value() != null && v.matcher(tag.value()).matches();
  }

  private Pattern compile(String pattern) {
    try {
      return Pattern.compile(switch (this.mode) {
      case REGEX -> pattern;
      case GLOB -> globToRegex(pattern);
      default -> Pattern.quote(pattern);
      }, Pattern.CASE_INSENSITIVE);
    } catch (PatternSyntaxException e) {
      throw new IllegalArgumentException("bad " + this.mode + " pattern: " + pattern + " (" + e.getMessage() + ")");
    }
  }

  /** {@code *} → any run, {@code ?} → one char; everything else is literal. */
  private static String globToRegex(String glob) {
    StringBuilder out = new StringBuilder(glob.length() + 8);
    StringBuilder literal = new StringBuilder();
    for (char c : glob.toCharArray()) {
      if (c == '*' || c == '?') {
        if (!literal.isEmpty()) {
          out.append(Pattern.quote(literal.toString()));
          literal.setLength(0);
        }
        out.append(c == '*' ? ".*" : ".");
      } else
        literal.append(c);
    }
    if (!literal.isEmpty())
      out.append(Pattern.quote(literal.toString()));
    return out.toString();
  }

  public JsonObject toJson() {
    JsonObject j = new JsonObject().put("keyPattern", this.keyPattern).put("mode", this.mode.name());
    if (this.valuePattern != null)
      j.put("valuePattern", this.valuePattern);
    return j;
  }

  public static TagQuery fromJson(JsonObject j) {
    String mode = j.getString("mode", Mode.EXACT.name());
    return new TagQuery(j.getString("keyPattern"), j.getString("valuePattern"),
        Mode.valueOf(mode.toUpperCase(java.util.Locale.ROOT)));
  }
}
