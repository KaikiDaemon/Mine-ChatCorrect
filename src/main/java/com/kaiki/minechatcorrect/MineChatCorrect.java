package com.kaiki.minechatcorrect;

import com.kaiki.minechatcorrect.config.ClientSettings;
import com.kaiki.minechatcorrect.spell.SpellChecker;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Main NeoForge mod entry point.
 *
 * <p>Mine-ChatCorrect is currently a client-side chat utility. The server does not
 * need this mod because all spell checking, suggestions, dictionary management,
 * and chat overlay rendering happen before the message is sent.</p>
 */
@Mod(MineChatCorrect.MOD_ID)
public final class MineChatCorrect {
    public static final String MOD_ID = "mine_chatcorrect";

    /**
     * Shared client settings instance. This is initialized only on the physical client.
     */
    private static ClientSettings clientSettings;

    /**
     * Shared spell checker instance. UI code uses this to find misspellings,
     * generate suggestions, import dictionaries, and manage additional words.
     */
    private static SpellChecker spellChecker;

    public MineChatCorrect(IEventBus modEventBus) {
        // Keep initialization client-only so dedicated servers never load client UI classes.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            clientSettings = new ClientSettings();
            spellChecker = new SpellChecker();

            // Register keybinds and client tick handling for opening the settings screen.
            com.kaiki.minechatcorrect.client.ClientKeyMappings.register(modEventBus);
        }
    }

    public static SpellChecker spellChecker() {
        return spellChecker;
    }

    public static ClientSettings clientSettings() {
        return clientSettings;
    }
}