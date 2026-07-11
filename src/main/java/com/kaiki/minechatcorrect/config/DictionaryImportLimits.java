package com.kaiki.minechatcorrect.config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shared import-size limits for dictionary files and extracted archive content.
 */
public final class DictionaryImportLimits {
    public static final long MAX_SOURCE_BYTES = 25L * 1024L * 1024L;
    public static final long MAX_EXTRACTED_ENTRY_BYTES = 10L * 1024L * 1024L;
    public static final long MAX_TOTAL_EXTRACTED_BYTES = 50L * 1024L * 1024L;

    private static final String MAX_ENTRY_OVERRIDE_PROPERTY = "minechatcorrect.test.maxEntryBytes";
    private static final String MAX_TOTAL_OVERRIDE_PROPERTY = "minechatcorrect.test.maxTotalExtractedBytes";

    private DictionaryImportLimits() {
    }

    public static byte[] readFile(Path path) throws IOException {
        long size = Files.size(path);
        if (size > MAX_SOURCE_BYTES) {
            throw new IOException("Dictionary source is too large. Maximum supported size is " + MAX_SOURCE_BYTES + " bytes.");
        }
        return Files.readAllBytes(path);
    }

    public static byte[] readStream(InputStream input, long maxBytes, String description) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;

        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new IOException(description + " is too large. Maximum supported size is " + maxBytes + " bytes.");
            }
            output.write(buffer, 0, read);
        }

        return output.toByteArray();
    }

    public static void validateEntrySize(long size, String name) throws IOException {
        if (size > maxExtractedEntryBytes()) {
            throw new IOException("Archive entry is too large: " + name);
        }
    }

    public static long addExtractedBytes(long currentTotal, long addedBytes) throws IOException {
        long total = currentTotal + addedBytes;
        long maxTotal = maxTotalExtractedBytes();
        if (total > maxTotal) {
            throw new IOException("Archive expands to too much data. Maximum supported extracted size is " + maxTotal + " bytes.");
        }
        return total;
    }

    static long maxExtractedEntryBytes() {
        return longProperty(MAX_ENTRY_OVERRIDE_PROPERTY, MAX_EXTRACTED_ENTRY_BYTES);
    }

    static long maxTotalExtractedBytes() {
        return longProperty(MAX_TOTAL_OVERRIDE_PROPERTY, MAX_TOTAL_EXTRACTED_BYTES);
    }

    private static long longProperty(String key, long fallback) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
