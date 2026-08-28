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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The manifest entry's contract: paths are normalized to one canonical spelling so the same file described twice
 * compares equal, hashes are validated against the algorithm that claims them, and an archive is a file that is also a
 * container.
 */
public class DataEntryTest {

  private final static String SHA = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

  private static DataEntry entry(String path) {
    return DataEntry.of(path, 10L, HashAlgorithm.SHA256, SHA);
  }

  @Test
  public void modifiedTimeIsTruncatedToMicrosBecauseTimestamptzHoldsMicros() {
    java.time.Instant nanos = java.time.Instant.parse("2026-08-01T00:00:00.123456789Z");
    DataEntry e = new DataEntry("a.txt", 1L, null, null, null, nanos, java.util.List.of());
    org.junit.jupiter.api.Assertions.assertEquals(java.time.Instant.parse("2026-08-01T00:00:00.123456Z"),
        e.modifiedAt(),
        "a filesystem reports nanoseconds and Postgres stores micros; comparing the two raw refuses every file "
            + "as changed");
  }

  @Test
  public void pathsNormalizeToOneSpelling() {
    // the same file, described four ways by four different producers
    assertEquals("docs/readme.md", entry("docs/readme.md").path());
    assertEquals("docs/readme.md", entry("./docs/readme.md").path());
    assertEquals("docs/readme.md", entry("docs//readme.md").path());
    assertEquals("docs/readme.md", entry("docs\\readme.md").path(), "windows separators are still paths");
  }

  @Test
  public void aPathMayNotLeaveItsMedium() {
    // refusing beats rewriting: this describes something other than the disc
    assertThrows(IllegalArgumentException.class, () -> entry("../etc/passwd"));
    assertThrows(IllegalArgumentException.class, () -> entry("docs/../../escape"));
    assertThrows(IllegalArgumentException.class, () -> entry("   "));
  }

  @Test
  public void aHashMustLookLikeItsAlgorithm() {
    assertThrows(IllegalArgumentException.class, () -> DataEntry.of("f", 1L, HashAlgorithm.SHA256, "abc"));
    assertThrows(IllegalArgumentException.class,
        () -> DataEntry.of("f", 1L, HashAlgorithm.SHA256, SHA.replace('e', 'z')));
    // case is not significance: digests normalize to lowercase
    assertEquals(SHA, DataEntry.of("f", 1L, HashAlgorithm.SHA256, SHA.toUpperCase(java.util.Locale.ROOT)).hash());
  }

  @Test
  public void anArchiveIsAFileThatIsAlsoAContainer() {
    DataEntry inner = entry("notes.txt");
    DataEntry zip = new DataEntry("backup.zip", 4096L, HashAlgorithm.SHA256, SHA, "application/zip", null,
        List.of(inner));

    assertTrue(zip.isArchive());
    assertFalse(inner.isArchive());
    // it is still one file on the medium: its own size and hash stand
    assertEquals(4096L, zip.sizeBytes());
    assertEquals(2, zip.flatten().size(), "the archive and what is inside it");
  }

  @Test
  public void archivesNestToAnyDepth() {
    DataEntry leaf = entry("deep.txt");
    DataEntry innerZip = new DataEntry("inner.zip", 100L, HashAlgorithm.SHA256, SHA, null, null, List.of(leaf));
    DataEntry outerZip = new DataEntry("outer.zip", 200L, HashAlgorithm.SHA256, SHA, null, null, List.of(innerZip));

    assertEquals(3, outerZip.flatten().size());
  }

  @Test
  public void archiveContentsAreImmutable() {
    java.util.List<DataEntry> mutable = new java.util.ArrayList<>(List.of(entry("a.txt")));
    DataEntry zip = new DataEntry("z.zip", 1L, HashAlgorithm.SHA256, SHA, null, null, mutable);
    mutable.add(entry("b.txt"));
    assertEquals(1, zip.archiveContents().size(), "the entry kept its own copy");
  }

  @Test
  public void wireRoundTripKeepsEverythingIncludingNesting() {
    Instant when = Instant.parse("2026-08-22T10:15:30Z");
    DataEntry zip = new DataEntry("backup.zip", 4096L, HashAlgorithm.SHA256, SHA, "application/zip", when,
        List.of(entry("notes.txt")));

    DataEntry read = DataEntry.fromJson(zip.toJson());

    assertEquals(zip, read);
    assertEquals(when, read.modified().orElse(null));
    assertEquals("application/zip", read.mime().orElse(null));
    assertEquals("notes.txt", read.archiveContents().get(0).path());
  }

  @Test
  public void pathHelpersSplitTheWayStorageWillStoreThem() {
    DataEntry deep = entry("home/mykel/git/somerepo/.git/config");
    assertEquals(List.of("home", "mykel", "git", "somerepo", ".git", "config"), deep.pathElements());
    assertEquals("config", deep.fileName());
  }
}
