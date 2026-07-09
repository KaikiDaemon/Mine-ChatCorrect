package com.kaiki.minechatcorrect.client;

import com.kaiki.minechatcorrect.MineChatCorrect;
import com.kaiki.minechatcorrect.config.DictionaryManager;
import com.kaiki.minechatcorrect.spell.SpellChecker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * Main in-game configuration screen.
 *
 * <p>This screen exposes options that matter during normal play: enabling the
 * optional auto-correct chat button, importing dictionaries, enabling/removing
 * imported dictionaries, and adding custom accepted words.</p>
 */
public final class MineChatCorrectSettingsScreen extends Screen {
    private final Screen parent;

    private EditBox dictionarySource;
    private EditBox addWordEditor;
    private Button autoCorrectToggle;
    private Button selectedDictionaryButton;
    private Button dictionaryEnableButton;
    private Button dictionaryRemoveButton;

    private int selectedDictionaryIndex;
    private String status = "";

    public MineChatCorrectSettingsScreen(@Nullable Screen parent) {
        super(Component.literal("Mine-ChatCorrect Settings"));
        this.parent = parent;
    }

    /**
     * Builds all visible controls. Minecraft recreates screen widgets on resize,
     * so every control is initialized from the current runtime state.
     */
    @Override
    protected void init() {
        int center = this.width / 2;
        int y = 34;

        // Toggle whether the chat overlay exposes the one-click auto-correct button.
        autoCorrectToggle = Button.builder(Component.literal(""), button -> {
            if (MineChatCorrect.clientSettings() != null) {
                MineChatCorrect.clientSettings().setAutoCorrectButtonEnabled(!MineChatCorrect.clientSettings().autoCorrectButtonEnabled());
            }
            refreshButtons();
        }).bounds(center - 155, y, 310, 20).build();
        addRenderableWidget(autoCorrectToggle);

        // Dictionary source accepts a URL or local file path.
        y += 34;
        dictionarySource = new EditBox(this.font, center - 155, y, 310, 20, Component.literal("Dictionary file URL or path"));
        dictionarySource.setMaxLength(1024);
        dictionarySource.setValue(DictionaryManager.DEFAULT_DICTIONARY_URL);
        addRenderableWidget(dictionarySource);

        y += 24;
        addRenderableWidget(Button.builder(Component.literal("Import dictionary file/archive from URL/path"), button -> importDictionary())
                .bounds(center - 155, y, 310, 20)
                .build());

        // Imported dictionary selection and management controls.
        y += 28;
        selectedDictionaryButton = Button.builder(Component.literal("Dictionary: -"), button -> cycleDictionary())
                .bounds(center - 155, y, 150, 20)
                .build();
        addRenderableWidget(selectedDictionaryButton);

        dictionaryEnableButton = Button.builder(Component.literal("Enable/disable"), button -> toggleDictionary())
                .bounds(center, y, 100, 20)
                .build();
        addRenderableWidget(dictionaryEnableButton);

        dictionaryRemoveButton = Button.builder(Component.literal("Remove"), button -> removeDictionary())
                .bounds(center + 105, y, 50, 20)
                .build();
        addRenderableWidget(dictionaryRemoveButton);

        // Quick add field for a new accepted custom word.
        y += 38;
        addWordEditor = new EditBox(this.font, center - 155, y, 190, 20, Component.literal("Additional word"));
        addWordEditor.setMaxLength(64);
        addRenderableWidget(addWordEditor);

        addRenderableWidget(Button.builder(Component.literal("Add word"), button -> addWord())
                .bounds(center + 40, y, 70, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Edit list"), button -> this.minecraft.setScreen(new AdditionalWordsScreen(this)))
                .bounds(center + 115, y, 40, 20)
                .build());

        // Utility buttons.
        y += 34;
        addRenderableWidget(Button.builder(Component.literal("Reload dictionaries"), button -> {
                    SpellChecker checker = MineChatCorrect.spellChecker();
                    if (checker != null) {
                        checker.reloadDictionaries();
                        ChatSpellOverlay.clearCache();
                        status = "Dictionaries reloaded.";
                    }
                    refreshButtons();
                })
                .bounds(center - 155, y, 150, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> this.minecraft.setScreen(parent))
                .bounds(center + 5, y, 150, 20)
                .build());

        refreshButtons();
    }

    /**
     * Draws widgets first, then title/status above them so text stays readable.
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);

        if (!status.isBlank()) {
            guiGraphics.drawCenteredString(this.font, status, this.width / 2, this.height - 28, 0xFFFFFF55);
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    /**
     * Imports a dictionary or StarDict archive from the source field.
     */
    private void importDictionary() {
        SpellChecker checker = MineChatCorrect.spellChecker();
        if (checker == null) {
            return;
        }

        try {
            String name = checker.importDictionary(dictionarySource.getValue());
            ChatSpellOverlay.clearCache();
            status = "Imported dictionary: " + name;
        } catch (IOException | IllegalArgumentException exception) {
            status = "Import failed: " + exception.getMessage();
        }

        refreshButtons();
    }

    /**
     * Cycles the selected imported dictionary shown on the dictionary button.
     */
    private void cycleDictionary() {
        List<DictionaryManager.ExternalDictionary> dictionaries = dictionaries();
        if (!dictionaries.isEmpty()) {
            selectedDictionaryIndex = Math.floorMod(selectedDictionaryIndex + 1, dictionaries.size());
        }
        refreshButtons();
    }

    /**
     * Enables or disables the currently selected imported dictionary.
     */
    private void toggleDictionary() {
        SpellChecker checker = MineChatCorrect.spellChecker();
        List<DictionaryManager.ExternalDictionary> dictionaries = dictionaries();

        if (checker != null && !dictionaries.isEmpty()) {
            DictionaryManager.ExternalDictionary dictionary = dictionaries.get(selectedDictionaryIndex);
            checker.setDictionaryEnabled(dictionary.name(), !dictionary.enabled());
            ChatSpellOverlay.clearCache();
            status = "Toggled dictionary: " + dictionary.name();
        }

        refreshButtons();
    }

    /**
     * Removes the selected imported dictionary from disk and from active use.
     */
    private void removeDictionary() {
        SpellChecker checker = MineChatCorrect.spellChecker();
        List<DictionaryManager.ExternalDictionary> dictionaries = dictionaries();

        if (checker != null && !dictionaries.isEmpty()) {
            DictionaryManager.ExternalDictionary dictionary = dictionaries.get(selectedDictionaryIndex);
            checker.removeDictionary(dictionary.name());
            selectedDictionaryIndex = 0;
            ChatSpellOverlay.clearCache();
            status = "Removed dictionary: " + dictionary.name();
        }

        refreshButtons();
    }

    /**
     * Adds a single custom accepted word from the main settings screen.
     */
    private void addWord() {
        SpellChecker checker = MineChatCorrect.spellChecker();
        if (checker == null) {
            return;
        }

        String value = addWordEditor.getValue().trim();
        if (value.isBlank()) {
            return;
        }

        checker.addWord(value);
        checker.reloadDictionaries();
        ChatSpellOverlay.clearCache();
        addWordEditor.setValue("");
        status = "Added word: " + value;
        refreshButtons();
    }

    /**
     * Refreshes button labels and enabled states from current settings/dictionaries.
     */
    private void refreshButtons() {
        if (autoCorrectToggle == null) {
            return;
        }

        boolean autoCorrect = MineChatCorrect.clientSettings() != null && MineChatCorrect.clientSettings().autoCorrectButtonEnabled();
        autoCorrectToggle.setMessage(Component.literal("Auto-correct chat button: " + (autoCorrect ? "enabled" : "disabled")));

        List<DictionaryManager.ExternalDictionary> dictionaries = dictionaries();
        if (selectedDictionaryIndex >= dictionaries.size()) {
            selectedDictionaryIndex = 0;
        }

        if (dictionaries.isEmpty()) {
            selectedDictionaryButton.setMessage(Component.literal("Dictionary: none"));
            dictionaryEnableButton.active = false;
            dictionaryRemoveButton.active = false;
        } else {
            DictionaryManager.ExternalDictionary dictionary = dictionaries.get(selectedDictionaryIndex);
            selectedDictionaryButton.setMessage(Component.literal("Dictionary: " + dictionary.name() + " [" + (dictionary.enabled() ? "on" : "off") + "]"));
            dictionaryEnableButton.active = true;
            dictionaryRemoveButton.active = true;
        }
    }

    private List<DictionaryManager.ExternalDictionary> dictionaries() {
        SpellChecker checker = MineChatCorrect.spellChecker();
        return checker == null ? List.of() : checker.dictionaryManager().dictionaries();
    }
}