package com.kaiki.minechatcorrect.spell;

import com.kaiki.minechatcorrect.config.DictionaryManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SpellChecker {
    private static final Pattern WORD_PATTERN = Pattern.compile("[A-Za-z][A-Za-z']{2,}");
    private static final Pattern URL_PATTERN = Pattern.compile("(?i)^(https?://|www\\.|[a-z0-9.-]+\\.[a-z]{2,})(.*)$");

    private final DictionaryManager dictionaryManager = new DictionaryManager();
    private Set<String> dictionary = dictionaryManager.allWords();

    public List<MisspelledWord> findMisspellings(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        if (text.startsWith("/")) {
            return List.of();
        }

        ArrayList<MisspelledWord> results = new ArrayList<>();
        Matcher matcher = WORD_PATTERN.matcher(text);

        while (matcher.find()) {
            String rawWord = matcher.group();

            if (shouldIgnore(rawWord)) {
                continue;
            }

            String normalized = normalize(rawWord);
            if (!dictionary.contains(normalized)) {
                results.add(new MisspelledWord(rawWord, matcher.start(), matcher.end()));
            }
        }

        return results;
    }

    public List<String> suggestionsFor(String word) {
        String normalized = normalize(word);
        if (normalized.isBlank()) {
            return List.of();
        }

        return dictionary.stream()
                .filter(candidate -> Math.abs(candidate.length() - normalized.length()) <= 2)
                .map(candidate -> new Suggestion(candidate, distance(normalized, candidate)))
                .filter(suggestion -> suggestion.distance() <= Math.max(2, normalized.length() / 3))
                .sorted(Comparator.comparingInt(Suggestion::distance).thenComparing(Suggestion::word))
                .limit(8)
                .map(Suggestion::word)
                .toList();
    }

    public String bestSuggestionFor(String word) {
        List<String> suggestions = suggestionsFor(word);
        return suggestions.isEmpty() ? "" : suggestions.getFirst();
    }

    public DictionaryManager dictionaryManager() {
        return dictionaryManager;
    }

    public void addWord(String word) {
        dictionaryManager.addExtraWord(word);
        reloadDictionaries();
    }

    public void reloadDictionaries() {
        dictionaryManager.reload();
        dictionary = dictionaryManager.allWords();
    }

    public String importDictionary(String source) throws IOException {
        String name = dictionaryManager.importDictionary(source);
        dictionary = dictionaryManager.allWords();
        return name;
    }

    public void setDictionaryEnabled(String name, boolean enabled) {
        dictionaryManager.setDictionaryEnabled(name, enabled);
        dictionary = dictionaryManager.allWords();
    }

    public void removeDictionary(String name) {
        dictionaryManager.removeDictionary(name);
        dictionary = dictionaryManager.allWords();
    }

    private boolean shouldIgnore(String word) {
        if (word.length() < 3) {
            return true;
        }

        if (word.indexOf('_') >= 0 || word.indexOf(':') >= 0 || word.indexOf('@') >= 0) {
            return true;
        }

        return URL_PATTERN.matcher(word).matches();
    }

    private String normalize(String word) {
        String normalized = word.toLowerCase(Locale.ROOT);

        if (normalized.endsWith("'s")) {
            normalized = normalized.substring(0, normalized.length() - 2);
        }

        return normalized;
    }

    private int distance(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];

        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost
                );
            }

            int[] swap = previous;
            previous = current;
            current = swap;
        }

        return previous[right.length()];
    }

    private record Suggestion(String word, int distance) {
    }
}