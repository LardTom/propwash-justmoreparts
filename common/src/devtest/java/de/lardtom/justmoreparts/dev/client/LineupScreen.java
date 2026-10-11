package de.lardtom.justmoreparts.dev.client;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.preset.JmpPresets;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.BuildStats;
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
import java.util.Locale;
import java.util.Optional;

public final class LineupScreen extends Screen implements ThemedScreen {
    public static final List<String> PRESETS = List.of("whoop75_1s", "toothpick35_6s", "cinewhoop35_6s", "deadcat5_6s", "race5_6s",
            "hframe5_6s", "lr7_deadcat_lion", "xclass13_8s");
    private static final int COLUMNS = 4;
    private static final int PAD = 8;
    private static final int TEXT_ROWS = 3;

    private final List<JmpPresets.Entry> entries;
    private final List<Optional<BuildStats>> stats;
    private final List<Integer> wheelbase;
    private long frames;

    public LineupScreen() {
        super(Component.literal(JustMoreParts.MOD_NAME));
        this.entries = PRESETS.stream().map(JmpPresets::byPath).toList();
        this.stats = this.entries.stream().map(e -> PropwashApi.builds().analyze(e.build())).toList();
        this.wheelbase = this.entries.stream().map(e -> PropwashApi.parts().get(FrameDefinition.class, JmpPresets.part(e.frame()))
                .map(FrameDefinition::wheelbaseMm).orElse(0)).toList();
    }

    public long frames() {
        return this.frames;
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
        ThemeDraw.brandedHeader(g, this.font, left, top, right, Metrics.HEADER_HEIGHT, this.title, right - PAD);
        int rows = (this.entries.size() + COLUMNS - 1) / COLUMNS;
        int gridTop = top + Metrics.HEADER_HEIGHT + PAD;
        int cellWidth = (right - left - PAD) / COLUMNS;
        int cellHeight = (bottom - gridTop) / rows;
        int textHeight = TEXT_ROWS * Metrics.LINE_HEIGHT + 4;
        for (int i = 0; i < this.entries.size(); i++) {
            JmpPresets.Entry entry = this.entries.get(i);
            int x0 = left + PAD + (i % COLUMNS) * cellWidth;
            int y0 = gridTop + (i / COLUMNS) * cellHeight;
            int x1 = x0 + cellWidth - PAD;
            int y1 = y0 + cellHeight - PAD;
            int stageBottom = y1 - textHeight;
            ThemeDraw.previewStage(g, x0, y0, x1, stageBottom);
            float yaw = 205.0F + 12.0F * (i % 3);
            PropwashClientApi.renderPreview(g, entry.data(), x0, y0, x1, stageBottom, yaw);
            ThemeText.line(g, this.font, Component.translatable(entry.translationKey()), x0, x1, stageBottom + 3, Palette.TEXT,
                    ThemeText.Align.LEFT, true);
            Optional<BuildStats> s = this.stats.get(i);
            if (s.isPresent()) {
                String first = String.format(Locale.ROOT, "%d mm · %.0f g · T/W %.1f", this.wheelbase.get(i), s.get().massGrams(),
                        s.get().thrustToWeight());
                String second = String.format(Locale.ROOT, "%.0f km/h · %.1f min", s.get().topSpeedKmh(), s.get().hoverFlightTimeMin());
                ThemeText.line(g, this.font, Component.literal(first), x0, x1, stageBottom + 3 + Metrics.LINE_HEIGHT, Palette.TEXT_MUTED,
                        ThemeText.Align.LEFT, true);
                ThemeText.line(g, this.font, Component.literal(second), x0, x1, stageBottom + 3 + 2 * Metrics.LINE_HEIGHT, Palette.TEXT_MUTED,
                        ThemeText.Align.LEFT, true);
            }
        }
        super.extractRenderState(g, mouseX, mouseY, a);
        ThemeDraw.windowBorder(g, left, top, right, bottom);
    }
}
