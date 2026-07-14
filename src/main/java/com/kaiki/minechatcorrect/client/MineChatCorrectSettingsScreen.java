package com.kaiki.minechatcorrect.client;

import com.kaiki.minechatcorrect.MineChatCorrectClient;
import com.kaiki.minechatcorrect.config.DictionaryManager;
import com.kaiki.minechatcorrect.spell.SpellChecker;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * Main in-game configuration screen.
 *
 * <p>This screen exposes options that matter during normal play: enabling the
 * optional auto-correct chat button, importing dictionaries, enabling/removing
 * imported dictionaries, and adding custom accepted words.</p>
 */
public final class MineChatCorrectSettingsScreen extends Screen {
    private static final int DICTIONARY_LIST_ROW_HEIGHT = 10;
    private static final int DICTIONARY_LIST_VISIBLE_ROWS = 4;

    private final Screen parent;

    private EditBox dictionarySource;
    private EditBox addWordEditor;
    private Button autoCorrectToggle;
    private Button selectedDictionaryButton;
    private Button previousDictionaryButton;
    private Button nextDictionaryButton;
    private Button dictionaryEnableButton;
    private Button dictionaryRemoveButton;
    private Button installLocalFileButton;

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
            if (MineChatCorrectClient.clientSettings() != null) {
                MineChatCorrectClient.clientSettings().setAutoCorrectButtonEnabled(!MineChatCorrectClient.clientSettings().autoCorrectButtonEnabled());
            }
            refreshButtons();
        }).bounds(center - 155, y, 310, 20).build();
        addRenderableWidget(autoCorrectToggle);

        // Dictionary source accepts a URL. Local files are installed through the import drop folder.
        y += 34;
        dictionarySource = new EditBox(this.font, center - 155, y, 310, 20, Component.literal("Dictionary URL"));
        dictionarySource.setMaxLength(1024);
        dictionarySource.setValue(DictionaryManager.DEFAULT_DICTIONARY_URL);
        addRenderableWidget(dictionarySource);

        y += 24;
        addRenderableWidget(Button.builder(Component.literal("Import from URL"), button -> importDictionary())
                .bounds(center - 155, y, 150, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Import file folder"), button -> openImportFolder())
                .bounds(center + 5, y, 150, 20)
                .build());

        y += 24;
        installLocalFileButton = Button.builder(Component.literal("Install file"), button -> installDroppedFile())
                .bounds(center - 155, y, 310, 20)
                .build();
        addRenderableWidget(installLocalFileButton);

        // Imported dictionary selection and management controls.
        y += 28;
        previousDictionaryButton = Button.builder(Component.literal("<"), button -> cycleDictionary(-1))
                .bounds(center - 155, y, 22, 20)
                .build();
        addRenderableWidget(previousDictionaryButton);

        selectedDictionaryButton = Button.builder(Component.literal("Dictionary: -"), button -> cycleDictionary(1))
                .bounds(center - 130, y, 135, 20)
                .build();
        addRenderableWidget(selectedDictionaryButton);

        nextDictionaryButton = Button.builder(Component.literal(">"), button -> cycleDictionary(1))
                .bounds(center + 8, y, 22, 20)
                .build();
        addRenderableWidget(nextDictionaryButton);

        dictionaryEnableButton = Button.builder(Component.literal("Enable/disable"), button -> toggleDictionary())
                .bounds(center + 33, y, 72, 20)
                .build();
        addRenderableWidget(dictionaryEnableButton);

        dictionaryRemoveButton = Button.builder(Component.literal("Remove"), button -> removeDictionary())
                .bounds(center + 108, y, 47, 20)
                .build();
        addRenderableWidget(dictionaryRemoveButton);

        // Leave room to render a compact imported-dictionary list under the selected dictionary row.
        y += 86;

        // Quick add field for a new accepted custom word.
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
                    SpellChecker checker = MineChatCorrectClient.spellChecker();
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

        renderDictionaryList(guiGraphics);

        if (!status.isBlank()) {
            guiGraphics.drawCenteredString(this.font, truncate(status, 72), this.width / 2, 24, 0xFFFFFF55);
        }
    }

    private void renderDictionaryList(GuiGraphics guiGraphics) {
        List<DictionaryManager.ExternalDictionary> dictionaries = dictionaries();
        if (dictionaries.isEmpty()) {
            return;
        }

        int x = this.width / 2 - 155;
        int y = this.height - 50;
        int maxVisible = Math.min(DICTIONARY_LIST_VISIBLE_ROWS, dictionaries.size());
        int start = Math.max(0, Math.min(selectedDictionaryIndex - 1, dictionaries.size() - maxVisible));

        for (int row = 0; row < maxVisible; row++) {
            int index = start + row;
            DictionaryManager.ExternalDictionary dictionary = dictionaries.get(index);
            boolean selected = index == selectedDictionaryIndex;
            String prefix = selected ? "> " : "  ";
            String state = dictionary.enabled() ? "enabled" : "disabled";
            String text = prefix + (index + 1) + "/" + dictionaries.size() + " " + state + " - " + dictionary.name();
            guiGraphics.drawString(this.font, truncate(text, 56), x, y + row * DICTIONARY_LIST_ROW_HEIGHT, selected ? 0xFFFFFF55 : 0xFFAAAAAA);
        }

        if (dictionaries.size() > maxVisible) {
            guiGraphics.drawString(this.font, "Click a row or use < / > to select", x, y + maxVisible * DICTIONARY_LIST_ROW_HEIGHT, 0xFF888888);
        }
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (selectDictionaryRow(mouseX, mouseY)) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean selectDictionaryRow(double mouseX, double mouseY) {
        List<DictionaryManager.ExternalDictionary> dictionaries = dictionaries();
        if (dictionaries.isEmpty()) {
            return false;
        }

        int x = this.width / 2 - 155;
        int y = this.height - 50;
        int maxVisible = Math.min(DICTIONARY_LIST_VISIBLE_ROWS, dictionaries.size());
        int start = Math.max(0, Math.min(selectedDictionaryIndex - 1, dictionaries.size() - maxVisible));

        if (mouseX < x || mouseX > x + 310 || mouseY < y || mouseY >= y + maxVisible * DICTIONARY_LIST_ROW_HEIGHT) {
            return false;
        }

        int row = (int) ((mouseY - y) / DICTIONARY_LIST_ROW_HEIGHT);
        selectedDictionaryIndex = start + row;
        refreshButtons();
        return true;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    /**
     * Imports a dictionary or StarDict archive from the source field.
     */
    private void importDictionary() {
        SpellChecker checker = MineChatCorrectClient.spellChecker();
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

    private void openImportFolder() {
        SpellChecker checker = MineChatCorrectClient.spellChecker();
        if (checker == null) {
            return;
        }

        try {
            Path dropDir = checker.dictionaryManager().localImportDropDir();
            Util.getPlatform().openUri(dropDir.toUri());
            status = "Import folder opened. Use mine_chatcorrect/dictionaries/drop.";
        } catch (IOException exception) {
            status = "Could not open import folder. Use mine_chatcorrect/dictionaries/drop.";
        }

        refreshButtons();
    }

    private void installDroppedFile() {
        SpellChecker checker = MineChatCorrectClient.spellChecker();
        if (checker == null) {
            return;
        }

        try {
            Path file = firstDroppedFile(checker.dictionaryManager().localImportDropDir());
            if (file == null) {
                status = "No file found in import folder.";
                refreshButtons();
                return;
            }

            String name = checker.importDictionary(file.toString());
            Files.deleteIfExists(file);
            ChatSpellOverlay.clearCache();
            status = "Installed dictionary: " + name;
        } catch (IOException | IllegalArgumentException exception) {
            status = "Install failed: " + exception.getMessage();
        }

        refreshButtons();
    }

    private Path firstDroppedFile(Path dropDir) throws IOException {
        if (!Files.exists(dropDir)) {
            return null;
        }

        try (var stream = Files.list(dropDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * Cycles the selected imported dictionary shown on the dictionary button.
     */
    private void cycleDictionary(int amount) {
        List<DictionaryManager.ExternalDictionary> dictionaries = dictionaries();
        if (!dictionaries.isEmpty()) {
            selectedDictionaryIndex = Math.floorMod(selectedDictionaryIndex + amount, dictionaries.size());
        }
        refreshButtons();
    }

    /**
     * Enables or disables the currently selected imported dictionary.
     */
    private void toggleDictionary() {
        SpellChecker checker = MineChatCorrectClient.spellChecker();
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
        SpellChecker checker = MineChatCorrectClient.spellChecker();
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
        SpellChecker checker = MineChatCorrectClient.spellChecker();
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

        boolean autoCorrect = MineChatCorrectClient.clientSettings() != null && MineChatCorrectClient.clientSettings().autoCorrectButtonEnabled();
        autoCorrectToggle.setMessage(Component.literal("Auto-correct chat button: " + (autoCorrect ? "enabled" : "disabled")));

        List<DictionaryManager.ExternalDictionary> dictionaries = dictionaries();
        if (selectedDictionaryIndex >= dictionaries.size()) {
            selectedDictionaryIndex = 0;
        }

        if (dictionaries.isEmpty()) {
            selectedDictionaryButton.setMessage(Component.literal("Dictionary: none"));
            previousDictionaryButton.active = false;
            nextDictionaryButton.active = false;
            dictionaryEnableButton.setMessage(Component.literal("Enable/disable"));
            dictionaryEnableButton.active = false;
            dictionaryRemoveButton.active = false;
        } else {
            DictionaryManager.ExternalDictionary dictionary = dictionaries.get(selectedDictionaryIndex);
            selectedDictionaryButton.setMessage(Component.literal("Dictionary " + (selectedDictionaryIndex + 1) + "/" + dictionaries.size() + ": " + truncate(dictionary.name(), 15) + " [" + (dictionary.enabled() ? "enabled" : "disabled") + "]"));
            previousDictionaryButton.active = dictionaries.size() > 1;
            nextDictionaryButton.active = dictionaries.size() > 1;
            dictionaryEnableButton.setMessage(Component.literal(dictionary.enabled() ? "Disable" : "Enable"));
            dictionaryEnableButton.active = true;
            dictionaryRemoveButton.active = true;
        }

        refreshInstallLocalFileButton();
    }

    private void refreshInstallLocalFileButton() {
        if (installLocalFileButton == null) {
            return;
        }

        SpellChecker checker = MineChatCorrectClient.spellChecker();
        if (checker == null) {
            installLocalFileButton.active = false;
            installLocalFileButton.setMessage(Component.literal("Install file: unavailable"));
            return;
        }

        try {
            Path file = firstDroppedFile(checker.dictionaryManager().localImportDropDir());
            if (file == null) {
                installLocalFileButton.active = false;
                installLocalFileButton.setMessage(Component.literal("Install file: none detected"));
            } else {
                installLocalFileButton.active = true;
                installLocalFileButton.setMessage(Component.literal("Install: " + truncate(file.getFileName().toString(), 24)));
            }
        } catch (IOException exception) {
            installLocalFileButton.active = false;
            installLocalFileButton.setMessage(Component.literal("Install file: folder error"));
        }
    }

    private List<DictionaryManager.ExternalDictionary> dictionaries() {
        SpellChecker checker = MineChatCorrectClient.spellChecker();
        return checker == null ? List.of() : checker.dictionaryManager().dictionaries();
    }
    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

}
