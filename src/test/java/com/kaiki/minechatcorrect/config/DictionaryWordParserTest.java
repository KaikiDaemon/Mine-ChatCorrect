package com.kaiki.minechatcorrect.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DictionaryWordParserTest {
    @Test
    void parsesPlainWordListsWithNormalizationAndDuplicatesRemoved() {
        Set<String> words = DictionaryWordParser.parseDictionaryWords("""
                Apple
                banana
                apple
                   Cherry
                minecraft/IGNORED_AFFIX
                # comment
                /slash-command-style-comment
                12345
                two words
                bad-word
                under_score
                a
                can't
                café
                """);

        assertEquals(List.of("apple", "banana", "cherry", "minecraft", "two", "can't", "café"), List.copyOf(words));
    }

    @Test
    void rejectsBlankMalformedAndTooShortDictionaryWords() {
        Set<String> words = DictionaryWordParser.parseDictionaryWords("""
                 
                #
                /
                99
                a
                i
                bad_word
                bad-word
                word123
                @mention
                ok
                """);

        assertEquals(Set.of("ok"), words);
    }

    @Test
    void handlesEmptyNullAndLargeLinesSafely() {
        assertTrue(DictionaryWordParser.parseDictionaryWords(null).isEmpty());
        assertTrue(DictionaryWordParser.parseDictionaryWords("   \n\t").isEmpty());

        String longWord = "a".repeat(10_000);
        Set<String> words = DictionaryWordParser.parseDictionaryWords(longWord + "\nvalid\n" + longWord);
        assertEquals(2, words.size());
        assertTrue(words.contains(longWord));
        assertTrue(words.contains("valid"));
    }
}
