package com.kaiki.minechatcorrect.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DictionaryImportLimitsTest {
    @TempDir
    Path tempDir;

    @Test
    void readsFilesWithinConfiguredLimit() throws Exception {
        Path file = tempDir.resolve("small.words");
        Files.writeString(file, "hello\nworld\n");

        assertArrayEquals("hello\nworld\n".getBytes(), DictionaryImportLimits.readFile(file));
    }

    @Test
    void rejectsStreamsAboveCallerLimit() {
        byte[] bytes = "abcdef".getBytes();

        IOException exception = assertThrows(IOException.class, () ->
                DictionaryImportLimits.readStream(new ByteArrayInputStream(bytes), 5, "Test stream"));

        assertTrue(exception.getMessage().contains("too large"));
    }

    @Test
    void validatesEntrySizeAtBoundary() throws Exception {
        DictionaryImportLimits.validateEntrySize(DictionaryImportLimits.MAX_EXTRACTED_ENTRY_BYTES, "max.txt");
    }

    @Test
    void rejectsOversizedArchiveEntriesAndExpandedTotals() {
        assertThrows(IOException.class, () ->
                DictionaryImportLimits.validateEntrySize(DictionaryImportLimits.MAX_EXTRACTED_ENTRY_BYTES + 1, "huge.txt"));

        assertThrows(IOException.class, () ->
                DictionaryImportLimits.addExtractedBytes(DictionaryImportLimits.MAX_TOTAL_EXTRACTED_BYTES, 1));
    }
}
