package com.kaiki.minechatcorrect.spell;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellCheckerTest {
    @Test
    void findsMisspellingsWhileIgnoringCommandsAndUrls() {
        SpellChecker checker = new SpellChecker(Set.of("hello", "world", "minecraft", "server", "visit"));

        assertTrue(checker.findMisspellings("/msg hello wurld").isEmpty());
        assertTrue(checker.findMisspellings("visit example.com hello").isEmpty());

        List<MisspelledWord> misspellings = checker.findMisspellings("hello wurld server");
        assertEquals(1, misspellings.size());
        assertEquals("wurld", misspellings.getFirst().word());
    }

    @Test
    void returnsDeterministicCorrectionSuggestions() {
        SpellChecker checker = new SpellChecker(Set.of("world", "word", "would", "wild", "hello"));

        List<String> suggestions = checker.suggestionsFor("wurld");

        assertFalse(suggestions.isEmpty());
        assertEquals("world", suggestions.getFirst());
        assertTrue(suggestions.contains("would"));
    }

    @Test
    void acceptedCustomWordsSuppressFalsePositivesInMemory() {
        SpellChecker checker = new SpellChecker(Set.of("kaiki", "minecraft", "chatcorrect", "uses"));

        assertTrue(checker.findMisspellings("Kaiki uses ChatCorrect").isEmpty());
        assertEquals(List.of("unknownword"), checker.findMisspellings("unknownword").stream().map(MisspelledWord::word).toList());
    }
}
