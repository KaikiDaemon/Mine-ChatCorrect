package com.kaiki.minechatcorrect.client;

import com.kaiki.minechatcorrect.MineChatCorrectClient;
import com.kaiki.minechatcorrect.spell.SpellChecker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Scrollable editor for the user's additional accepted words.
 *
 * <p>The main settings screen can add a single word quickly. This screen is for
 * reviewing, editing, and removing words that have already been stored.</p>
 */
public final class AdditionalWordsScreen extends Screen {
    private final Screen parent;

    private EditBox editBox;
    private int selectedIndex;
    private int scrollOffset;
    private String status = "";

    public AdditionalWordsScreen(@Nullable Screen parent) {
        super(Component.literal("Mine-ChatCorrect Additional Words"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuildWidgets();
    }

    /**
     * Rebuilds the visible word rows whenever the selected word or scroll offset changes.
     */
    @Override
    protected void rebuildWidgets() {
        clearWidgets();

        int center = this.width / 2;
        int y = 34;

        editBox = new EditBox(this.font, center - 155, y, 200, 20, Component.literal("Selected word"));
        editBox.setMaxLength(64);

        List<String> words = words();
        if (!words.isEmpty()) {
            selectedIndex = Math.max(0, Math.min(selectedIndex, words.size() - 1));
            editBox.setValue(words.get(selectedIndex));
        }
        addRenderableWidget(editBox);

        addRenderableWidget(Button.builder(Component.literal("Save edit"), button -> saveEdit())
                .bounds(center + 50, y, 105, 20)
                .build());

        y += 28;

        // Only visible rows are rendered; mouse wheel scrolling changes scrollOffset.
        int visibleRows = Math.max(4, Math.min(10, (this.height - 110) / 22));
        for (int row = 0; row < visibleRows; row++) {
            int wordIndex = scrollOffset + row;
            if (wordIndex >= words.size()) {
                break;
            }

            String word = words.get(wordIndex);
            int rowY = y + row * 22;
            Component label = Component.literal((wordIndex == selectedIndex ? "> " : "") + word);

            addRenderableWidget(Button.builder(label, button -> {
                        selectedIndex = wordIndex;
                        editBox.setValue(word);
                        rebuildWidgets();
                    })
                    .bounds(center - 155, rowY, 200, 20)
                    .build());

            addRenderableWidget(Button.builder(Component.literal("Remove"), button -> removeWord(word))
                    .bounds(center + 50, rowY, 105, 20)
                    .build());
        }

        addRenderableWidget(Button.builder(Component.literal("Back"), button -> this.minecraft.setScreen(parent))
                .bounds(center - 155, this.height - 28, 150, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Reload"), button -> {
                    SpellChecker checker = MineChatCorrectClient.spellChecker();
                    if (checker != null) {
                        checker.reloadDictionaries();
                    }
                    rebuildWidgets();
                })
                .bounds(center + 5, this.height - 28, 150, 20)
                .build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<String> words = words();
        int visibleRows = Math.max(4, Math.min(10, (this.height - 110) / 22));
        int maxOffset = Math.max(0, words.size() - visibleRows);

        if (scrollY < 0) {
            scrollOffset = Math.min(maxOffset, scrollOffset + 1);
        } else if (scrollY > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        }

        rebuildWidgets();
        return true;
    }

    /**
     * Draw widgets first, then draw title/status above them for readability.
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);

        if (!status.isBlank()) {
            guiGraphics.drawCenteredString(this.font, status, this.width / 2, this.height - 42, 0xFFFFFF55);
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    /**
     * Saves the edited value over the currently selected additional word.
     */
    private void saveEdit() {
        SpellChecker checker = MineChatCorrectClient.spellChecker();
        List<String> words = words();

        if (checker == null || words.isEmpty() || selectedIndex >= words.size()) {
            return;
        }

        String value = editBox.getValue().trim();
        if (value.isBlank()) {
            return;
        }

        checker.dictionaryManager().replaceExtraWord(words.get(selectedIndex), value);
        checker.reloadDictionaries();
        ChatSpellOverlay.clearCache();
        status = "Saved word: " + value;
        rebuildWidgets();
    }

    /**
     * Removes a word from the persistent additional-word file.
     */
    private void removeWord(String word) {
        SpellChecker checker = MineChatCorrectClient.spellChecker();
        if (checker != null) {
            checker.dictionaryManager().removeExtraWord(word);
            checker.reloadDictionaries();
            ChatSpellOverlay.clearCache();
            selectedIndex = 0;
            scrollOffset = 0;
            status = "Removed word: " + word;
        }

        rebuildWidgets();
    }

    private List<String> words() {
        SpellChecker checker = MineChatCorrectClient.spellChecker();
        return checker == null ? List.of() : checker.dictionaryManager().extraWords();
    }
}
