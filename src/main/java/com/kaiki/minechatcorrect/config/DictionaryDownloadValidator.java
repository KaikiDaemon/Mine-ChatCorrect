package com.kaiki.minechatcorrect.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Small validation helpers for downloaded dictionary payloads.
 */
public final class DictionaryDownloadValidator {
    private DictionaryDownloadValidator() {
    }

    public static void validateDownloadedContent(byte[] content) throws IOException {
        if (content == null || content.length == 0) {
            throw new IOException("Download failed. Received empty file.");
        }

        String sample = new String(content, 0, Math.min(content.length, 128), StandardCharsets.UTF_8)
                .trim()
                .toLowerCase(Locale.ROOT);
        if (sample.startsWith("<!doctype html") || sample.startsWith("<html")) {
            throw new IOException("Download returned HTML instead of a dictionary archive.");
        }
    }

    public static void validateRedirectCount(int redirects, int maxRedirects) throws IOException {
        if (redirects > maxRedirects) {
            throw new IOException("Download failed. Too many redirects.");
        }
    }
}
