package com.kaiki.minechatcorrect.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcceptedWordsStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void writesAndReloadsNormalizedAcceptedWords() throws Exception {
        Path file = tempDir.resolve("additional_words.txt");

        AcceptedWordsStore.write(file, List.of("  Kaiki  ", "ChatCorrect", "kaiki", "", "Minecraft"));

        assertEquals(Set.of("chatcorrect", "kaiki", "minecraft"), AcceptedWordsStore.read(file));
    }

    @Test
    void addRemoveAndReplaceKeepSetStable() {
        Set<String> words = new LinkedHashSet<>();

        assertFalse(AcceptedWordsStore.add(words, "   "));
        assertTrue(AcceptedWordsStore.add(words, "Kaiki"));
        assertFalse(AcceptedWordsStore.add(words, "kaiki"));
        assertTrue(AcceptedWordsStore.add(words, "ChatCorrect"));

        assertTrue(AcceptedWordsStore.replace(words, "chatcorrect", "MineChatCorrect"));
        assertFalse(AcceptedWordsStore.replace(words, "minechatcorrect", "  "));

        assertTrue(AcceptedWordsStore.remove(words, "KAIKI"));
        assertFalse(AcceptedWordsStore.remove(words, "missing"));

        assertEquals(Set.of("minechatcorrect"), words);
    }

    @Test
    void missingAcceptedWordsFileReadsAsEmptySet() throws Exception {
        assertTrue(AcceptedWordsStore.read(tempDir.resolve("missing.txt")).isEmpty());
    }
}
