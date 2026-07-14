package com.kaiki.minechatcorrect.fabric;

import com.kaiki.minechatcorrect.client.MineChatCorrectSettingsScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Registers and handles Fabric client keybindings. */
public final class FabricClientKeyMappings {
    private static final KeyMapping OPEN_SETTINGS = new KeyMapping(
            "key.mine_chatcorrect.open_settings",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.mine_chatcorrect"
    );

    private FabricClientKeyMappings() {
    }

    public static void register() {
        KeyBindingHelper.registerKeyBinding(OPEN_SETTINGS);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_SETTINGS.consumeClick()) {
                client.setScreen(new MineChatCorrectSettingsScreen(client.screen));
            }
        });
    }
}
