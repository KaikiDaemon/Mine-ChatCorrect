package com.kaiki.minechatcorrect.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DictionaryManagerImportTest {
    @TempDir
    Path tempDir;

    @Test
    void importsLocalWordListAndCleansFailedImports() throws Exception {
        Path source = tempDir.resolve("words.txt");
        Files.writeString(source, "Kaiki\nChatCorrect\n# comment\nkaiki\n");

        Path configDir = tempDir.resolve("config");
        DictionaryManager manager = new DictionaryManager(configDir);

        String dictionaryName = manager.importDictionary(source.toString());

        assertFalse(dictionaryName.isBlank());
        assertTrue(manager.allWords().contains("kaiki"));
        assertTrue(manager.allWords().contains("chatcorrect"));

        long importsBeforeFailure = importDirectoryCount(configDir);

        Path badSource = tempDir.resolve("bad.txt");
        Files.writeString(badSource, "1\n# comment\n_\n");
        Exception exception = assertThrows(Exception.class, () -> manager.importDictionary(badSource.toString()));

        assertTrue(exception.getMessage().contains("No usable dictionary words"));
        assertEquals(importsBeforeFailure, importDirectoryCount(configDir));
    }

    @Test
    void importedDictionaryEnabledStatePersistsAcrossReloads() throws Exception {
        Path source = tempDir.resolve("words.txt");
        Files.writeString(source, "temporaryword\n");

        Path configDir = tempDir.resolve("config");
        DictionaryManager manager = new DictionaryManager(configDir);
        String dictionaryName = manager.importDictionary(source.toString());

        assertTrue(manager.allWords().contains("temporaryword"));

        manager.setDictionaryEnabled(dictionaryName, false);
        assertFalse(manager.allWords().contains("temporaryword"));

        DictionaryManager reloaded = new DictionaryManager(configDir);
        assertFalse(reloaded.allWords().contains("temporaryword"));

        reloaded.setDictionaryEnabled(dictionaryName, true);
        DictionaryManager enabledAgain = new DictionaryManager(configDir);
        assertTrue(enabledAgain.allWords().contains("temporaryword"));
    }

    @Test
    void extraWordStoragePersistsThroughDictionaryManagerReload() {
        Path configDir = tempDir.resolve("config");

        DictionaryManager manager = new DictionaryManager(configDir);
        manager.addExtraWord("KaikiTerm");

        DictionaryManager reloaded = new DictionaryManager(configDir);

        assertTrue(reloaded.allWords().contains("kaikiterm"));
    }

    @Test
    void importsLocalDictionaryDirectory() throws Exception {
        Path sourceDir = tempDir.resolve("source-dictionary");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("words.txt"), "directoryword\nfolderword\n");

        DictionaryManager manager = new DictionaryManager(tempDir.resolve("config"));
        manager.importDictionary(sourceDir.toString());

        assertTrue(manager.allWords().contains("directoryword"));
        assertTrue(manager.allWords().contains("folderword"));
    }

    @Test
    void importsStarDictIndexFromLocalDirectory() throws Exception {
        Path sourceDir = tempDir.resolve("stardict");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("dictionary.ifo"), "bookname=Test StarDict\n");
        Files.write(sourceDir.resolve("dictionary.idx"), starDictIndex("stardictword"));

        DictionaryManager manager = new DictionaryManager(tempDir.resolve("config"));
        String dictionaryName = manager.importDictionary(sourceDir.toString());

        assertEquals("Test StarDict", dictionaryName);
        assertTrue(manager.allWords().contains("stardictword"));
    }

    @Test
    void unsupportedImportWithNoUsableWordsFailsClearlyAndCleansUp() throws Exception {
        Path source = tempDir.resolve("unsupported.bin");
        Files.write(source, new byte[]{0, 1, 2, 3, 4});

        Path configDir = tempDir.resolve("config");
        DictionaryManager manager = new DictionaryManager(configDir);

        Exception exception = assertThrows(Exception.class, () -> manager.importDictionary(source.toString()));

        assertTrue(exception.getMessage().contains("No usable dictionary words"));
        assertEquals(0, importDirectoryCount(configDir));
    }

    @Test
    void importsDictIndexHeadwordsFromLocalDirectory() throws Exception {
        Path sourceDir = tempDir.resolve("dictd");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("freedict.index"), "hello\t0\t10\nworld\t10\t20\nnot-valid-word\t20\t30\n");
        Files.writeString(sourceDir.resolve("freedict.dict"), "definition data is ignored\n");

        DictionaryManager manager = new DictionaryManager(tempDir.resolve("config"));
        manager.importDictionary(sourceDir.toString());

        assertTrue(manager.allWords().contains("hello"));
        assertTrue(manager.allWords().contains("world"));
    }

    private byte[] starDictIndex(String word) {
        byte[] wordBytes = word.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] bytes = new byte[wordBytes.length + 1 + 8];
        System.arraycopy(wordBytes, 0, bytes, 0, wordBytes.length);
        return bytes;
    }

    private long importDirectoryCount(Path configDir) throws Exception {
        Path importsDir = configDir.resolve("dictionaries").resolve("imports");
        if (!Files.exists(importsDir)) {
            return 0;
        }

        try (var stream = Files.list(importsDir)) {
            return stream.filter(Files::isDirectory).count();
        }
    }
}
