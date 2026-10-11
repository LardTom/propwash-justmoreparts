package de.lardtom.justmoreparts.dev.client;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.preset.JmpPresets;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.client.PropwashClientApi;
import de.lardtom.propwash.api.part.FrameDefinition;
import de.lardtom.propwash.ui.client.theme.Metrics;
import de.lardtom.propwash.ui.client.theme.Palette;
import de.lardtom.propwash.ui.client.theme.ThemeDraw;
import de.lardtom.propwash.ui.client.theme.ThemeText;
import de.lardtom.propwash.ui.client.theme.ThemedScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class ScaleScreen extends Screen implements ThemedScreen {
    public static final List<String> PRESETS = List.of("whoop75_1s", "toothpick35_6s", "deadcat5_6s", "lr7_deadcat_lion", "xclass13_8s");
    public static final float FIT = 0.9F;
    public static final int BAR_MM = 200;
    private static final int PAD = 8;
    private static final int GAP = 10;
    private static final int TEXT_ROWS = 2;
    private static final float YAW = 205.0F;

    private final List<JmpPresets.Entry> entries;
    private final int[] wheelbase;
    private final int[] span;
    private long frames;

    public ScaleScreen() {
        super(Component.literal(JustMoreParts.MOD_NAME));
        this.entries = PRESETS.stream().map(JmpPresets::byPath).toList();
        this.wheelbase = new int[this.entries.size()];
        this.span = new int[this.entries.size()];
        for (int i = 0; i < this.entries.size(); i++) {
            JmpPresets.Entry entry = this.entries.get(i);
            this.wheelbase[i] = PropwashApi.parts().get(FrameDefinition.class, JmpPresets.part(entry.frame())).map(FrameDefinition::wheelbaseMm)
                    .orElse(0);
            this.span[i] = Math.max(1, (int) Math.round(PropwashApi.builds().footprintMm(entry.data().build())));
        }
    }

    public long frames() {
        return this.frames;
    }

    public static float pixelsPerMillimetre(int[] spans, int width) {
        int total = 0;
        for (int s : spans) {
            total += s;
        }
        return total <= 0 ? 0.0F : Math.max(0.0F, width - GAP * (spans.length - 1)) / (float) total;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        ThemeDraw.backdrop(g, this.width, this.height);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        this.frames++;
        int left = PAD;
        int top = PAD;
        int right = this.width - PAD;
        int bottom = this.height - PAD;
        ThemeDraw.window(g, left, top, right, bottom);
        int titleEnd = ThemeDraw.brandedHeader(g, this.font, left, top, right, Metrics.HEADER_HEIGHT, this.title, right - PAD);
        ThemeDraw.headerSubtitle(g, this.font, titleEnd, top, Metrics.HEADER_HEIGHT, Component.literal("1 : 1"), right - PAD);
        int stageLeft = left + PAD;
        int stageRight = right - PAD;
        int stageTop = top + Metrics.HEADER_HEIGHT + PAD;
        int stageBottom = bottom - PAD - TEXT_ROWS * Metrics.LINE_HEIGHT - 6;
        ThemeDraw.previewStage(g, stageLeft, stageTop, stageRight, stageBottom);
        int inner = stageRight - stageLeft - 2 * PAD;
        float scale = pixelsPerMillimetre(this.span, inner);
        int x = stageLeft + PAD;
        for (int i = 0; i < this.entries.size(); i++) {
            JmpPresets.Entry entry = this.entries.get(i);
            int w = Math.max(8, Math.round(this.span[i] * scale));
            int center = x + w / 2;
            ThemeText.textCentered(g, this.font, Component.literal(this.wheelbase[i] + " mm"), center, stageTop + 5, Palette.TEXT_MUTED);
            PropwashClientApi.renderPreview(g, entry.data(), x, stageTop + 16, x + w, stageBottom - 16, YAW, scale * FIT);
            int labelY = stageBottom + 4 + (i % 2) * Metrics.LINE_HEIGHT;
            Component label = Component.translatable(entry.translationKey());
            int labelWidth = this.font.width(label);
            int labelX = Math.max(stageLeft, Math.min(stageRight - labelWidth, center - labelWidth / 2));
            ThemeText.text(g, this.font, label, labelX, labelY, Palette.TEXT);
            x += w + GAP;
        }
        int bar = Math.round(BAR_MM * scale * FIT);
        int barX = stageLeft + PAD;
        int barY = stageBottom - 10;
        g.fill(barX, barY, barX + bar, barY + 2, Palette.ACCENT);
        g.fill(barX, barY - 3, barX + 1, barY + 5, Palette.ACCENT);
        g.fill(barX + bar - 1, barY - 3, barX + bar, barY + 5, Palette.ACCENT);
        ThemeText.text(g, this.font, BAR_MM / 10 + " cm", barX + bar + 4, barY - 3, Palette.TEXT_MUTED);
        super.extractRenderState(g, mouseX, mouseY, a);
        ThemeDraw.windowBorder(g, left, top, right, bottom);
    }
}
