package com.kaiki.minechatcorrect;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Main NeoForge mod entry point.
 *
 * <p>Mine-ChatCorrect is currently a client-side chat utility. The server does not
 * need this mod because all spell checking, suggestions, dictionary management,
 * and chat overlay rendering happen before the message is sent.</p>
 */
@Mod(MineChatCorrect.MOD_ID)
public final class MineChatCorrect {
    public static final String MOD_ID = MineChatCorrectClient.MOD_ID;

    public MineChatCorrect(IEventBus modEventBus) {
        // Keep initialization client-only so dedicated servers never load client UI classes.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            MineChatCorrectClient.initialize(FMLPaths.CONFIGDIR.get());

            // Register keybinds and client tick handling for opening the settings screen.
            com.kaiki.minechatcorrect.client.ClientKeyMappings.register(modEventBus);
        }
    }

}
