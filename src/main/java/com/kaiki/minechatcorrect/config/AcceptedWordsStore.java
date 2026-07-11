package com.kaiki.minechatcorrect.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * File-backed accepted/custom word helpers.
 *
 * <p>The methods are deliberately independent from Minecraft runtime classes so
 * persistence behavior can be validated with fast unit tests.</p>
 */
public final class AcceptedWordsStore {
    private AcceptedWordsStore() {
    }

    public static Set<String> read(Path path) throws IOException {
        LinkedHashSet<String> words = new LinkedHashSet<>();
        if (!Files.exists(path)) {
            return words;
        }

        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String normalized = DictionaryWordParser.normalize(line);
            if (!normalized.isBlank()) {
                words.add(normalized);
            }
        }

        return words;
    }

    public static void write(Path path, Collection<String> words) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path, normalizeAll(words).stream().sorted().toList(), StandardCharsets.UTF_8);
    }

    public static boolean add(Set<String> words, String word) {
        String normalized = DictionaryWordParser.normalize(word);
        return !normalized.isBlank() && words.add(normalized);
    }

    public static boolean remove(Set<String> words, String word) {
        return words.remove(DictionaryWordParser.normalize(word));
    }

    public static boolean replace(Set<String> words, String oldWord, String newWord) {
        String oldNormalized = DictionaryWordParser.normalize(oldWord);
        String newNormalized = DictionaryWordParser.normalize(newWord);
        if (newNormalized.isBlank()) {
            return false;
        }

        boolean changed = words.remove(oldNormalized);
        changed = words.add(newNormalized) || changed;
        return changed;
    }

    private static Set<String> normalizeAll(Collection<String> words) {
        LinkedHashSet<String> normalizedWords = new LinkedHashSet<>();
        for (String word : words) {
            String normalized = DictionaryWordParser.normalize(word);
            if (!normalized.isBlank()) {
                normalizedWords.add(normalized);
            }
        }
        return normalizedWords;
    }
}
