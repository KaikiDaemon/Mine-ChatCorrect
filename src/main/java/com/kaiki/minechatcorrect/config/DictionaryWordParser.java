package com.kaiki.minechatcorrect.config;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Pure dictionary word parsing and normalization helpers.
 *
 * <p>This class intentionally has no Minecraft or NeoForge dependencies so the
 * import behavior can be validated with fast unit tests.</p>
 */
public final class DictionaryWordParser {
    private DictionaryWordParser() {
    }

    public static Set<String> parseDictionaryWords(String content) {
        LinkedHashSet<String> words = new LinkedHashSet<>();
        if (content == null || content.isBlank()) {
            return words;
        }

        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isBlank() || trimmed.startsWith("#") || trimmed.startsWith("/")) {
                continue;
            }

            if (trimmed.matches("\\d+")) {
                continue;
            }

            String word = trimmed.split("[/\\s]", 2)[0];
            String normalized = normalizeDictionaryWord(word);
            if (!normalized.isBlank()) {
                words.add(normalized);
            }
        }

        return words;
    }

    public static String normalizeDictionaryWord(String word) {
        String normalized = normalize(word);
        if (normalized.indexOf(' ') >= 0 || normalized.indexOf('_') >= 0 || normalized.indexOf('-') >= 0) {
            return "";
        }

        if (!normalized.matches("[\\p{L}']{2,}")) {
            return "";
        }

        return normalized;
    }

    public static String normalize(String word) {
        return word == null ? "" : word.trim().toLowerCase(Locale.ROOT);
    }
}
