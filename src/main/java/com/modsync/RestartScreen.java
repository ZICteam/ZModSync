package com.modsync;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public class RestartScreen extends Screen {
    public RestartScreen() {
        super(LanguageManager.component("modsync.restart.title"));
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int centerY = height / 2;
        addRenderableWidget(Button.builder(LanguageManager.component("modsync.restart_now"), button -> restartNow())
                .bounds(centerX - 105, centerY + 20, 100, 20)
                .build());
        addRenderableWidget(Button.builder(LanguageManager.component("modsync.restart_later"), button -> onClose())
                .bounds(centerX + 5, centerY + 20, 100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(font, title, width / 2, 28, 0xFFFFFF);
        guiGraphics.drawCenteredString(font, LanguageManager.component("modsync.restart_required"), width / 2, 48, 0xDDDDDD);

        int boxLeft = 40;
        int boxTop = 70;
        int boxRight = width - 40;
        int boxBottom = height - 70;
        guiGraphics.fill(boxLeft, boxTop, boxRight, boxBottom, 0x99000000);
        guiGraphics.drawString(font, LanguageManager.component("modsync.restart.updated_header"), boxLeft + 8, boxTop + 8, 0xFFFFFF, false);

        List<String> lines = RestartDetailsFormatter.buildLines(font, boxRight - boxLeft - 16,
                Math.max(1, (boxBottom - boxTop - 26) / 10));
        int y = boxTop + 22;
        for (String line : lines) {
            guiGraphics.drawString(font, line, boxLeft + 8, y, 0xD7D7D7, false);
            y += 10;
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void restartNow() {
        Minecraft.getInstance().stop();
    }
}
