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

import java.util.concurrent.CompletionStage;

/**
 * Connector for label printers: given an item and its rendered QR code, produce a physical label. Vendor-specific
 * implementations plug in behind this interface; the default implementation just logs.
 *
 * @author mykel
 *
 */
public interface LabelPrinter {

  // --- named label formats --------------------------------------------------
  // The format vocabulary a caller may pass to printLabel / LabelRequest.
  // The names are SEMANTIC, not physical: each printer maps them onto its
  // own media ("standard" is a Zebra 2.25×1.25 die-cut OR a Brother 12 mm
  // strip). Null always selects the printer's automatic layout. Each printer
  // honors its own subset and REFUSES the rest — and the Brother mappings
  // additionally bind to a tape width, refusing when the loaded tape
  // differs.

  /**
   * The everyday label. Zebra: 2.25×1.25 in die-cut (QR left, name/id/type/qty/location column). Brother: the 12 mm
   * compact strip — QR + name + printed date + weight when present, bold H when heavy.
   */
  String FORMAT_STANDARD = "standard";
  /**
   * The big label. Zebra: 2.25×4 in die-cut (room-scale QR, full field stack). Brother: the 24 mm QR + name/id wide
   * layout.
   */
  String FORMAT_LARGE = "large";
  /** Zebra die-cut 4×4 in. */
  String FORMAT_X_LARGE = "x-large";
  /** Zebra die-cut 4×6.5 in. */
  String FORMAT_2X_LARGE = "2x-large";

  /** Brother, any tape width: force the QR-only layout (the legacy flag). */
  String FORMAT_QR_ONLY = "qr-only";
  /**
   * The smallest scannable label: the bare-ULID code alone — always the id, never a URL (Brother 9 mm; replaces the
   * earlier id-only 9 mm format).
   */
  String FORMAT_TINY = "tiny";
  /** The standard-size code-only label: the full-URL QR alone (Brother 12 mm, v3 at 2 dots/module). */
  String FORMAT_STANDARD_QR_ONLY = "standard-qr";

  /** Print a label; false when no printer is available or printing failed. */
  CompletionStage<Boolean> printLabel(Item item, byte[] qrPng);

  /**
   * Print in a named label format ({@code standard}, {@code large}, ...) for printers with more than one media layout.
   * {@code format} may be null — use the printer's configured default. Printers without named formats ignore it.
   */
  default CompletionStage<Boolean> printLabel(Item item, byte[] qrPng, String format) {
    return printLabel(item, qrPng);
  }

  /**
   * Print with the scan URL the QR encodes, for printers that render their own module-exact code (small-media QRs need
   * integer dots per module — ongoing item 11). {@code scanUrl} may be null (such printers fall back to the item's bare
   * ULID); printers that just use the rendered {@code qrPng} ignore it.
   */
  default CompletionStage<Boolean> printLabel(Item item, String scanUrl, byte[] qrPng, String format) {
    return printLabel(item, qrPng, format);
  }

  /**
   * Feed blank media and cut — the "extend the tape" action that reclaims the leader on continuous-tape printers
   * (ongoing item 10). False when this printer has nothing to feed (die-cut media, no printer).
   */
  default CompletionStage<Boolean> feed() {
    return java.util.concurrent.CompletableFuture.completedStage(false);
  }

  /** One label of a batch; {@code scanUrl} and {@code format} may be null. */
  record LabelRequest(Item item, String scanUrl, byte[] qrPng, String format) {
  }

  /**
   * Print a whole run as ONE printer job (ongoing item 10). On continuous-tape printers the labels then share a single
   * ~25 mm leader instead of wasting one per cut, which is the entire point of chain printing.
   *
   * This exists because chaining CANNOT be a per-print flag: the Brother protocol treats a run as one continuous
   * conversation, so pages must stream down a single connection with only the last ending the job. Sending them as
   * independent jobs silently destroyed labels and wedged the printer (hardware-proven 2026-08-18).
   *
   * The default implementation prints sequentially — correct for printers with no batch concept (die-cut Zebra, the
   * logging printer), where every label is its own physical unit anyway.
   */
  default CompletionStage<Boolean> printBatch(java.util.List<LabelRequest> requests) {
    return printBatch(requests, true);
  }

  /**
   * As {@link #printBatch(java.util.List)}, but {@code halfCutBetween} chooses how the labels separate on continuous
   * tape: half cut perforates between them so the run stays one strip you tear by hand (the default, and what makes a
   * chained run practical to handle), while false leaves a full cut between every label — separate labels, but each cut
   * costs the feed to the cutter, giving back the leader saving chaining exists for.
   *
   * Meaningless on die-cut media, where every label is already its own physical unit; such printers ignore it.
   */
  default CompletionStage<Boolean> printBatch(java.util.List<LabelRequest> requests, boolean halfCutBetween) {
    CompletionStage<Boolean> all = java.util.concurrent.CompletableFuture.completedStage(true);
    for (LabelRequest r : requests)
      all = all.thenCompose(ok -> printLabel(r.item(), r.scanUrl(), r.qrPng(), r.format()).thenApply(one -> ok && one));
    return all;
  }
}
