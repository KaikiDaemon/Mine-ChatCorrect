package com.kaiki.minechatcorrect.mixin;

import com.kaiki.minechatcorrect.client.ChatActionBar;
import com.kaiki.minechatcorrect.client.ChatSpellOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Shadow
    protected EditBox input;

    @Inject(method = "init", at = @At("TAIL"))
    private void mineChatCorrect$init(CallbackInfo callbackInfo) {
        Screen screen = (Screen) (Object) this;
        ChatActionBar.addButtons(screen, this.input);
        screen.setFocused(this.input);
        this.input.setFocused(true);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void mineChatCorrect$updateButtons(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo callbackInfo) {
        ChatActionBar.update(this.input);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void mineChatCorrect$renderSpellOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo callbackInfo) {
        ChatSpellOverlay.render(guiGraphics, this.input);
    }
}