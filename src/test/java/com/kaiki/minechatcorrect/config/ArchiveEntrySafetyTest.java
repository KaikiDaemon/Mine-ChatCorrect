package com.kaiki.minechatcorrect.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchiveEntrySafetyTest {
    @TempDir
    Path tempDir;

    @Test
    void resolvesNormalRelativeArchiveEntriesInsideRoot() throws Exception {
        Path resolved = ArchiveEntrySafety.safeResolve(tempDir, "dict/words.txt");

        assertTrue(resolved.startsWith(tempDir.toAbsolutePath().normalize()));
        assertEquals("words.txt", resolved.getFileName().toString());
    }

    @Test
    void rejectsPathTraversalAndAbsoluteArchiveEntries() throws Exception {
        assertThrows(IOException.class, () -> ArchiveEntrySafety.safeResolve(tempDir, "../evil.txt"));
        assertThrows(IOException.class, () -> ArchiveEntrySafety.safeResolve(tempDir, "/tmp/evil.txt"));
        assertEquals(tempDir.toAbsolutePath().normalize(), ArchiveEntrySafety.safeResolve(tempDir, ""));
    }
}
