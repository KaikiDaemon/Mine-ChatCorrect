package com.kaiki.minechatcorrect.config;

import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ClientSettings {
    private static final Path CONFIG_FILE = FMLPaths.CONFIGDIR.get().resolve("mine_chatcorrect").resolve("settings.properties");

    private boolean autoCorrectButtonEnabled;

    public ClientSettings() {
        load();
    }

    public boolean autoCorrectButtonEnabled() {
        return autoCorrectButtonEnabled;
    }

    public void setAutoCorrectButtonEnabled(boolean enabled) {
        autoCorrectButtonEnabled = enabled;
        save();
    }

    public void load() {
        try {
            if (!Files.exists(CONFIG_FILE)) {
                save();
                return;
            }

            for (String line : Files.readAllLines(CONFIG_FILE, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.equalsIgnoreCase("autoCorrectButtonEnabled=true")) {
                    autoCorrectButtonEnabled = true;
                } else if (trimmed.equalsIgnoreCase("autoCorrectButtonEnabled=false")) {
                    autoCorrectButtonEnabled = false;
                }
            }
        } catch (IOException ignored) {
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            Files.writeString(CONFIG_FILE, "autoCorrectButtonEnabled=" + autoCorrectButtonEnabled + "\n", StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}