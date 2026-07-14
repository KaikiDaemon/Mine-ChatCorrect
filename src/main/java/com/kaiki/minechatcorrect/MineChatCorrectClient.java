package com.kaiki.minechatcorrect;

import com.kaiki.minechatcorrect.config.ClientSettings;
import com.kaiki.minechatcorrect.spell.SpellChecker;

import java.nio.file.Path;

/**
 * Loader-neutral client state shared by the NeoForge and Fabric entry points.
 */
public final class MineChatCorrectClient {
    public static final String MOD_ID = "mine_chatcorrect";

    private static ClientSettings clientSettings;
    private static SpellChecker spellChecker;

    private MineChatCorrectClient() {
    }

    /**
     * Initializes client state beneath the loader's base configuration directory.
     * The loader-specific entry point must pass its config root, not a mod-specific child.
     */
    public static void initialize(Path configRoot) {
        Path configDir = configRoot.resolve(MOD_ID);
        clientSettings = new ClientSettings(configDir);
        spellChecker = new SpellChecker(configDir);
    }

    public static SpellChecker spellChecker() {
        return spellChecker;
    }

    public static ClientSettings clientSettings() {
        return clientSettings;
    }
}
