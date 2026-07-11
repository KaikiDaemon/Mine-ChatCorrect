package com.kaiki.minechatcorrect.client;

import com.kaiki.minechatcorrect.MineChatCorrect;
import com.kaiki.minechatcorrect.mixin.EditBoxAccessor;
import com.kaiki.minechatcorrect.spell.MisspelledWord;
import com.kaiki.minechatcorrect.spell.SpellChecker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;

import java.util.List;

/**
 * Draws misspelling underlines inside the vanilla chat input.
 *
 * <p>The overlay does not modify chat text. It only reads the current input value,
 * asks the spell checker for misspelled word ranges, and draws a red underline
 * below the visible portion of each misspelled word.</p>
 */
public final class ChatSpellOverlay {
    private static final int UNDERLINE_COLOR = 0xFFFF5555;

    private static String lastText = "";
    private static List<MisspelledWord> cachedMisspellings = List.of();

    private ChatSpellOverlay() {
    }

    /**
     * Renders underlines for the currently visible chat text.
     *
     * <p>Minecraft's EditBox scrolls horizontally for long input. The mixin accessor
     * exposes that scroll offset so underline positions remain aligned with the
     * visible text rather than the full unscrolled string.</p>
     */
    public static void render(GuiGraphics guiGraphics, EditBox input) {
        if (input == null || !input.isVisible()) {
            return;
        }

        SpellChecker checker = MineChatCorrect.spellChecker();
        if (checker == null) {
            return;
        }

        String text = input.getValue();
        if (!text.equals(lastText)) {
            lastText = text;
            cachedMisspellings = checker.findMisspellings(text);
        }

        if (cachedMisspellings.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int displayPos = getDisplayPos(input);

        // Keep these offsets close to vanilla EditBox text rendering.
        int textLeft = input.getX();
        int textRight = input.getX() + input.getWidth() - 4;
        int underlineY = input.getY() + input.getHeight() - 5;

        for (MisspelledWord word : cachedMisspellings) {
            if (word.start() < 0 || word.end() > text.length() || word.start() >= word.end()) {
                continue;
            }

            // Skip words that are completely scrolled off the left side.
            if (word.end() <= displayPos) {
                continue;
            }

            int visibleStart = Math.max(word.start(), displayPos);
            int visibleEnd = word.end();

            String beforeWord = text.substring(displayPos, visibleStart);
            String visibleWord = text.substring(visibleStart, visibleEnd);

            int startX = textLeft + minecraft.font.width(beforeWord);
            int endX = startX + minecraft.font.width(visibleWord);

            // Clamp underlines to the input box so they do not draw outside chat.
            startX = Math.max(startX, textLeft);
            endX = Math.min(endX, textRight);

            if (endX > startX) {
                guiGraphics.fill(startX, underlineY, Math.max(startX + 1, endX), underlineY + 1, UNDERLINE_COLOR);
            }
        }
    }

    /**
     * Forces a full spell-check recalculation after dictionaries or settings change.
     */
    public static void clearCache() {
        lastText = "";
        cachedMisspellings = List.of();
    }

    private static int getDisplayPos(EditBox input) {
        if (input instanceof EditBoxAccessor accessor) {
            int displayPos = accessor.mineChatCorrect$getDisplayPos();
            return Math.max(0, Math.min(displayPos, input.getValue().length()));
        }

        return 0;
    }
}