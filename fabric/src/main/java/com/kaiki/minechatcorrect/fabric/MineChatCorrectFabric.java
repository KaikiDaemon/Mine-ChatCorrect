package com.kaiki.minechatcorrect.fabric;

import com.kaiki.minechatcorrect.MineChatCorrectClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Fabric client entry point. */
public final class MineChatCorrectFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MineChatCorrectClient.initialize(FabricLoader.getInstance().getConfigDir());
        FabricClientKeyMappings.register();
    }
}
