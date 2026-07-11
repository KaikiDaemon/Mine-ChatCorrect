package com.kaiki.minechatcorrect.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DictionaryMetadataStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void identifiesMetadataFiles() {
        assertTrue(DictionaryMetadataStore.isMetadataFile(Path.of(".mine-chatcorrect.properties")));
        assertFalse(DictionaryMetadataStore.isMetadataFile(Path.of("words.txt")));
    }

    @Test
    void missingMetadataDefaultsToEnabled() throws Exception {
        assertTrue(DictionaryMetadataStore.readEnabled(tempDir.resolve("dict")));
    }

    @Test
    void persistsEnabledState() throws Exception {
        Path dictionaryPath = tempDir.resolve("dict");

        DictionaryMetadataStore.writeEnabled(dictionaryPath, false);
        assertFalse(DictionaryMetadataStore.readEnabled(dictionaryPath));

        DictionaryMetadataStore.writeEnabled(dictionaryPath, true);
        assertTrue(DictionaryMetadataStore.readEnabled(dictionaryPath));
    }
}
