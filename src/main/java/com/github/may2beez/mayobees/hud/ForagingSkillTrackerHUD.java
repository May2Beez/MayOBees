package com.github.may2beez.mayobees.hud;

import cc.polyfrost.oneconfig.config.annotations.Color;
import cc.polyfrost.oneconfig.config.annotations.Dropdown;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.BasicHud;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.TextRenderer;
import com.github.may2beez.mayobees.module.impl.skills.Foraging;
import net.minecraft.client.Minecraft;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class ForagingSkillTrackerHUD extends BasicHud {
    @Color(
            name = "Text Color"
    )
    protected OneColor color = new OneColor(255, 255, 255);
    @Dropdown(
            name = "Text Type",
            options = {"No Shadow", "Shadow", "Full Shadow"}
    )
    protected int textType = 0;
    private final List<String> lines = new ArrayList<>();
    public ForagingSkillTrackerHUD() {
        super(true, 10, 10, 1, true, true, 4, 5, 5, new OneColor(0, 0, 0, 150), false, 2, new OneColor(0, 0, 0, 127));
    }

    @Override
    protected void draw(UMatrixStack matrices, float x, float y, float scale, boolean example) {
        loadLines();
        float textY = position.getY() + scale;
        for (String line : lines) {
            drawLine(line, position.getX() + scale, textY += 15 * scale, scale);
        }
    }

    @Override
    protected float getWidth(float scale, boolean example) {
        if (lines == null || lines.isEmpty()) return 0;
        float width = 0;
        for (String line : lines) {
            if (line == null) break;
            width = Math.max(width, Platform.getGLPlatform().getStringWidth(line) * scale);
        }
        return width + 11 * scale;
    }

    @Override
    protected float getHeight(float scale, boolean example) {
        if (lines == null) return 0;
        return (lines.size() + 1) * 15 * scale;
    }

    private void drawLine(String text, float x, float y, float scale) {
        if (text == null) return;
        TextRenderer.drawScaledString(text,
                x + (3.5f * scale) + paddingX * scale,
                y + paddingY * scale - Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT * scale,
                color.getRGB(),
                TextRenderer.TextType.toType(textType),
                scale);
    }

    private void loadLines() {
        this.lines.clear();

        Foraging foraging = Foraging.getInstance();
        int upTime = (int) ((foraging.isRunning() ? System.currentTimeMillis() - foraging.startTime : foraging.stopTime - foraging.startTime) / 1e3);
        int remainingSeconds = upTime % 3600;
        String uptimeString = String.format("%02dh %02dm %02ds", (upTime / 3600), (remainingSeconds / 60), (remainingSeconds % 60));

        if (foraging.isRunning() && foraging.totalXpGained <= 0) {
            this.lines.add("Please Ensure You Have Turned On Foraging Skill in Tablist Widget.");
        }
        this.lines.add("UpTime: " + uptimeString + (upTime > 0 && !foraging.isRunning() ? " (Paused)" : ""));
        this.lines.add("Total Xp Gained: " + NumberFormat.getInstance().format(foraging.totalXpGained));
        this.lines.add("Xp / H: " + NumberFormat.getInstance().format((foraging.totalXpGained * 3600) / Math.max(1, upTime)));
    }
}
