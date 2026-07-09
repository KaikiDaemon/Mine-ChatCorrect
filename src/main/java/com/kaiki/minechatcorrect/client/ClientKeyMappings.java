package com.kaiki.minechatcorrect.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/**
 * Registers and handles Mine-ChatCorrect client keybinds.
 *
 * <p>The settings key is unbound by default so users can choose a key that does
 * not conflict with their existing controls.</p>
 */
public final class ClientKeyMappings {
    private static final KeyMapping OPEN_SETTINGS = new KeyMapping(
            "key.mine_chatcorrect.open_settings",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.mine_chatcorrect"
    );

    private ClientKeyMappings() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ClientKeyMappings::registerKeys);
        NeoForge.EVENT_BUS.addListener(ClientKeyMappings::onClientTick);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SETTINGS);
    }

    /**
     * Opens the settings screen on the client tick after the key press is consumed.
     */
    private static void onClientTick(ClientTickEvent.Post event) {
        while (OPEN_SETTINGS.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen(new MineChatCorrectSettingsScreen(minecraft.screen));
        }
    }
}