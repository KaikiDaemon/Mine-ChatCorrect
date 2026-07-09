package com.kaiki.minechatcorrect.client;

import com.kaiki.minechatcorrect.MineChatCorrect;
import com.kaiki.minechatcorrect.mixin.ScreenInvoker;
import com.kaiki.minechatcorrect.spell.MisspelledWord;
import com.kaiki.minechatcorrect.spell.SpellChecker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Adds the small correction controls above the vanilla chat box.
 *
 * <p>The action bar is intentionally stateful and static because only one chat
 * screen exists at a time. It tracks the current misspelled word, the current
 * suggestion, and an optional manually typed replacement.</p>
 */
public final class ChatActionBar {
    private static Button wordButton;
    private static Button suggestionButton;
    private static EditBox replacementInput;
    private static Button applyButton;
    private static Button addWordButton;
    private static Button autoCorrectButton;

    private static int selectedWordIndex;
    private static int selectedSuggestionIndex;
    private static String previousInputText = "";
    private static List<MisspelledWord> words = List.of();
    private static List<String> suggestions = List.of();

    private ChatActionBar() {
    }

    /**
     * Creates the action widgets and attaches them to the current chat screen.
     *
     * <p>After the widgets are added, focus is returned to the vanilla chat input.
     * This prevents the action widgets from stealing keyboard input when chat opens.</p>
     */
    public static void addButtons(Screen screen, EditBox input) {
        int x = 4;
        int y = screen.height - 42;
        int gap = 3;

        wordButton = Button.builder(Component.literal("Word: -"), button -> cycleWord(1))
                .bounds(x, y, 82, 20)
                .build();

        x += 82 + gap;

        suggestionButton = Button.builder(Component.literal("Sug: -"), button -> cycleSuggestion())
                .bounds(x, y, 105, 20)
                .build();

        x += 105 + gap;

        replacementInput = new EditBox(
                Minecraft.getInstance().font,
                x,
                y,
                78,
                20,
                Component.literal("Replacement")
        );
        replacementInput.setMaxLength(64);

        x += 78 + gap;

        applyButton = Button.builder(Component.literal("Apply"), button -> applySuggestion(input))
                .bounds(x, y, 46, 20)
                .build();

        x += 46 + gap;

        addWordButton = Button.builder(Component.literal("Add"), button -> addSelectedWord(input))
                .bounds(x, y, 42, 20)
                .build();

        x += 42 + gap;

        autoCorrectButton = Button.builder(Component.literal("Auto"), button -> autoCorrect(input))
                .bounds(x, y, 48, 20)
                .build();

        ScreenInvoker invoker = (ScreenInvoker) screen;
        invoker.mineChatCorrect$addRenderableWidget(wordButton);
        invoker.mineChatCorrect$addRenderableWidget(suggestionButton);
        invoker.mineChatCorrect$addRenderableWidget(replacementInput);
        invoker.mineChatCorrect$addRenderableWidget(applyButton);
        invoker.mineChatCorrect$addRenderableWidget(addWordButton);
        invoker.mineChatCorrect$addRenderableWidget(autoCorrectButton);

        update(input);
        forceChatInputFocus(input);
    }

    /**
     * Recalculates misspellings and suggestions from the current chat text.
     *
     * <p>This method is called every chat render tick. It keeps the cached selected
     * indices valid so applying a correction cannot crash after the list changes.</p>
     */
    public static void update(EditBox input) {
        SpellChecker checker = MineChatCorrect.spellChecker();

        // If the user typed after clicking an action widget, return focus to chat.
        if (input != null && !input.getValue().equals(previousInputText)) {
            previousInputText = input.getValue();
            input.setFocused(true);
            clearActionBarFocus();
        }

        if (checker == null || input == null) {
            words = List.of();
            suggestions = List.of();
            sanitizeSelection();
            setVisible(false);
            return;
        }

        words = checker.findMisspellings(input.getValue());
        sanitizeSelection();

        if (words.isEmpty()) {
            suggestions = List.of();
            sanitizeSelection();
            setVisible(false);
            return;
        }

        suggestions = checker.suggestionsFor(selectedWord().word());
        sanitizeSelection();

        refreshMessages();
        setVisible(true);
    }

    /**
     * Clears focus from every action-bar widget and returns focus to the chat input.
     */
    public static void forceChatInputFocus(EditBox input) {
        clearActionBarFocus();

        if (input != null) {
            input.setFocused(true);
        }
    }

    /**
     * Prevents action widgets from trapping keyboard input after a click or screen reopen.
     */
    private static void clearActionBarFocus() {
        if (wordButton != null) {
            wordButton.setFocused(false);
            suggestionButton.setFocused(false);
            replacementInput.setFocused(false);
            applyButton.setFocused(false);
            addWordButton.setFocused(false);
            autoCorrectButton.setFocused(false);
        }
    }

    /**
     * Ensures cached selection indices remain valid after text replacement,
     * auto-correction, word additions, or dictionary reloads.
     */
    private static void sanitizeSelection() {
        if (words == null || words.isEmpty()) {
            selectedWordIndex = 0;
            selectedSuggestionIndex = 0;
            words = List.of();
            suggestions = List.of();
            return;
        }

        if (selectedWordIndex < 0 || selectedWordIndex >= words.size()) {
            selectedWordIndex = 0;
        }

        if (suggestions == null || suggestions.isEmpty()) {
            selectedSuggestionIndex = 0;
            suggestions = List.of();
            return;
        }

        if (selectedSuggestionIndex < 0 || selectedSuggestionIndex >= suggestions.size()) {
            selectedSuggestionIndex = 0;
        }
    }

    /**
     * Moves to the next misspelled word in the chat input.
     */
    private static void cycleWord(int amount) {
        if (words.isEmpty()) {
            return;
        }

        selectedWordIndex = Math.floorMod(selectedWordIndex + amount, words.size());
        selectedSuggestionIndex = 0;

        SpellChecker checker = MineChatCorrect.spellChecker();
        suggestions = checker == null ? List.of() : checker.suggestionsFor(selectedWord().word());

        if (replacementInput != null) {
            replacementInput.setValue("");
        }

        sanitizeSelection();
        refreshMessages();
    }

    /**
     * Moves to the next suggested replacement for the selected word.
     */
    private static void cycleSuggestion() {
        if (suggestions.isEmpty()) {
            return;
        }

        selectedSuggestionIndex = Math.floorMod(selectedSuggestionIndex + 1, suggestions.size());

        if (replacementInput != null) {
            replacementInput.setValue("");
        }

        sanitizeSelection();
        refreshMessages();
    }

    /**
     * Applies either the manually typed replacement or the selected suggestion.
     */
    private static void applySuggestion(EditBox input) {
        sanitizeSelection();

        if (input == null || words.isEmpty()) {
            return;
        }

        String replacement = "";
        if (replacementInput != null) {
            replacement = replacementInput.getValue().trim();
        }

        if (replacement.isBlank()) {
            if (suggestions.isEmpty()) {
                return;
            }
            replacement = selectedSuggestion();
        }

        replaceSelectedWord(input, replacement);

        if (replacementInput != null) {
            replacementInput.setValue("");
            replacementInput.setFocused(false);
        }

        clearChatSelection(input);
        input.setFocused(true);

        previousInputText = input.getValue();
        update(input);
    }

    /**
     * Adds the selected misspelled word to the user's additional-word list.
     */
    private static void addSelectedWord(EditBox input) {
        sanitizeSelection();

        SpellChecker checker = MineChatCorrect.spellChecker();
        if (checker == null || words.isEmpty()) {
            return;
        }

        checker.addWord(selectedWord().word());

        if (input != null) {
            clearChatSelection(input);
            input.setFocused(true);
            update(input);
        } else {
            words = List.of();
            suggestions = List.of();
            sanitizeSelection();
            setVisible(false);
        }
    }

    /**
     * Replaces every currently misspelled word with its closest available suggestion.
     */
    private static void autoCorrect(EditBox input) {
        SpellChecker checker = MineChatCorrect.spellChecker();
        if (checker == null || input == null) {
            return;
        }

        List<MisspelledWord> misspelled = checker.findMisspellings(input.getValue());
        String text = input.getValue();

        // Replace from right to left so earlier index ranges remain valid.
        for (int i = misspelled.size() - 1; i >= 0; i--) {
            MisspelledWord word = misspelled.get(i);
            String suggestion = checker.bestSuggestionFor(word.word());
            if (!suggestion.isBlank()) {
                text = text.substring(0, word.start()) + suggestion + text.substring(word.end());
            }
        }

        input.setValue(text);
        input.setCursorPosition(text.length());
        clearChatSelection(input);
        input.setFocused(true);

        previousInputText = text;
        update(input);
    }

    /**
     * Replaces the selected word range in the vanilla chat input.
     */
    private static void replaceSelectedWord(EditBox input, String replacement) {
        sanitizeSelection();

        if (words.isEmpty()) {
            return;
        }

        MisspelledWord word = selectedWord();
        String text = input.getValue();

        if (word.start() < 0 || word.end() > text.length() || word.start() >= word.end()) {
            return;
        }

        String newText = text.substring(0, word.start()) + replacement + text.substring(word.end());
        int cursor = word.start() + replacement.length();

        input.setValue(newText);
        input.setCursorPosition(cursor);
        clearChatSelection(input);
    }

    /**
     * Clears Minecraft's highlighted selection so corrected text is not left selected.
     */
    private static void clearChatSelection(EditBox input) {
        int cursor = input.getCursorPosition();
        input.setHighlightPos(cursor);
    }

    private static MisspelledWord selectedWord() {
        sanitizeSelection();
        return words.get(selectedWordIndex);
    }

    private static String selectedSuggestion() {
        sanitizeSelection();
        return suggestions.get(selectedSuggestionIndex);
    }

    /**
     * Updates button labels, visibility, and enabled state from current spell-check state.
     */
    private static void refreshMessages() {
        if (wordButton == null) {
            return;
        }

        sanitizeSelection();

        String word = words.isEmpty() ? "-" : selectedWord().word();
        String suggestion = suggestions.isEmpty() ? "-" : selectedSuggestion();

        wordButton.setMessage(Component.literal("Word: " + truncate(word, 8)));
        suggestionButton.setMessage(Component.literal("Sug: " + truncate(suggestion, 10)));

        boolean hasWord = !words.isEmpty();
        boolean hasSuggestionOrTypedReplacement = hasWord
                && ((replacementInput != null && !replacementInput.getValue().trim().isBlank()) || !suggestions.isEmpty());

        boolean autoCorrectEnabled = MineChatCorrect.clientSettings() != null
                && MineChatCorrect.clientSettings().autoCorrectButtonEnabled();

        wordButton.active = hasWord;
        suggestionButton.active = !suggestions.isEmpty();
        replacementInput.active = hasWord;
        applyButton.active = hasSuggestionOrTypedReplacement;
        addWordButton.active = hasWord;
        autoCorrectButton.active = hasWord && autoCorrectEnabled;
    }

    /**
     * Shows the action bar only while there are misspellings to act on.
     */
    private static void setVisible(boolean visible) {
        if (wordButton == null) {
            return;
        }

        boolean autoCorrectEnabled = MineChatCorrect.clientSettings() != null
                && MineChatCorrect.clientSettings().autoCorrectButtonEnabled();

        wordButton.visible = visible;
        suggestionButton.visible = visible;
        replacementInput.visible = visible;
        applyButton.visible = visible;
        addWordButton.visible = visible;
        autoCorrectButton.visible = visible && autoCorrectEnabled;

        if (!visible) {
            clearActionBarFocus();
        }

        refreshMessages();
    }

    private static String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, Math.max(1, maxLength - 1)) + "…";
    }
}