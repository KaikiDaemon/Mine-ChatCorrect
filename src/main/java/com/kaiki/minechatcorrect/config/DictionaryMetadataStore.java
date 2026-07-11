package com.kaiki.minechatcorrect.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Small file-backed metadata store for imported dictionaries.
 */
public final class DictionaryMetadataStore {
    static final String METADATA_FILE = ".mine-chatcorrect.properties";
    private static final String ENABLED_KEY = "enabled";

    private DictionaryMetadataStore() {
    }

    public static boolean isMetadataFile(Path path) {
        return path != null && METADATA_FILE.equals(path.getFileName().toString());
    }

    public static boolean readEnabled(Path dictionaryPath) throws IOException {
        Path metadataPath = metadataPath(dictionaryPath);
        if (!Files.exists(metadataPath)) {
            return true;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(metadataPath, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        return Boolean.parseBoolean(properties.getProperty(ENABLED_KEY, "true"));
    }

    public static void writeEnabled(Path dictionaryPath, boolean enabled) throws IOException {
        Files.createDirectories(dictionaryPath);

        Properties properties = new Properties();
        properties.setProperty(ENABLED_KEY, Boolean.toString(enabled));

        try (Writer writer = Files.newBufferedWriter(metadataPath(dictionaryPath), StandardCharsets.UTF_8)) {
            properties.store(writer, "Mine-ChatCorrect dictionary metadata");
        }
    }

    private static Path metadataPath(Path dictionaryPath) {
        return dictionaryPath.resolve(METADATA_FILE);
    }
}
