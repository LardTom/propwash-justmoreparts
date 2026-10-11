package de.lardtom.justmoreparts.share.client;

import de.lardtom.justmoreparts.share.PartAliases;
import de.lardtom.justmoreparts.share.ShareContent;
import de.lardtom.justmoreparts.share.ShareDecoded;
import de.lardtom.justmoreparts.share.ShareFormat;
import de.lardtom.justmoreparts.share.ShareStatus;
import de.lardtom.justmoreparts.share.ShareTables;
import de.lardtom.propwash.api.build.BuildCheck;
import de.lardtom.propwash.api.build.BuildStats;
import de.lardtom.propwash.api.build.ImportResult;
import de.lardtom.propwash.api.build.ImportStatus;
import de.lardtom.propwash.ui.client.theme.Metrics;
import de.lardtom.propwash.ui.client.theme.Palette;
import de.lardtom.propwash.ui.client.theme.ThemeDraw;
import de.lardtom.propwash.ui.client.theme.ThemeText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ShareLines {
    public static final int INDENT = 8;
    public static final int SCROLLBAR_SPACE = Metrics.SCROLLBAR_WIDTH + 4;

    private ShareLines() {
    }

    public record Line(Component text, int color, boolean section, int indent) {
        public static Line of(Component text, int color) {
            return new Line(text, color, false, 0);
        }

        public static Line indented(Component text, int color) {
            return new Line(text, color, false, INDENT);
        }

        public static Line section(Component text) {
            return new Line(text, Palette.ACCENT, true, 0);
        }
    }

    public static Locale locale() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null ? Locale.ROOT : ShareFormat.locale(minecraft.getLanguageManager().getSelected());
    }

    public static Line status(ShareDecoded decoded) {
        if (decoded.status() == ShareStatus.EMPTY) {
            return Line.of(Component.translatable("justmoreparts.share.import.empty"), Palette.TEXT_FAINT);
        }
        if (!decoded.ok()) {
            return Line.of(Component.translatable(decoded.status().translationKey()), Palette.ERROR);
        }
        return Line.of(Component.translatable("justmoreparts.share.import.readable", ShareFormat.layerList(decoded.layers())), Palette.SUCCESS);
    }

    public static List<Line> describe(ShareImportState state, @Nullable ShoppingList shopping, boolean creative) {
        List<Line> lines = new ArrayList<>();
        ShareDecoded decoded = state.decoded();
        lines.add(status(decoded));
        if (!decoded.ok()) {
            return lines;
        }
        ShareContent content = decoded.require();
        content.name().ifPresent(name -> lines.add(Line.of(Component.translatable("justmoreparts.share.import.name", Component.literal(name)),
                Palette.TEXT)));
        lines.add(Line.section(Component.translatable("justmoreparts.share.section.parts")));
        for (ShareFormat.Slot slot : ShareFormat.slots(content.build())) {
            lines.add(Line.indented(partLine(slot, decoded), partColor(slot.id(), decoded)));
        }
        if (!decoded.unknownParts().isEmpty()) {
            lines.add(Line.of(Component.translatable("justmoreparts.share.import.unknown", decoded.unknownParts().size()), Palette.ERROR));
        }
        if (!decoded.wrongKind().isEmpty()) {
            lines.add(Line.of(Component.translatable("justmoreparts.share.import.wrong_kind", ShareFormat.idList(decoded.wrongKind())),
                    Palette.WARNING));
        }
        for (PartAliases.Rename rename : decoded.renamed()) {
            lines.add(Line.indented(Component.translatable("justmoreparts.share.import.renamed", Component.literal(rename.from().toString()),
                    Component.literal(rename.to().toString())), Palette.TEXT_MUTED));
        }
        if (state.check().isPresent()) {
            BuildCheck check = state.check().get();
            for (String problem : check.problems()) {
                lines.add(Line.of(Component.translatable(BuildCheck.translationKey(problem)), Palette.ERROR));
            }
            for (String warning : check.warnings()) {
                lines.add(Line.of(Component.translatable(BuildCheck.translationKey(warning)), Palette.WARNING));
            }
        }
        state.stats().ifPresent(stats -> addStats(lines, stats));
        List<Line> layers = new ArrayList<>();
        content.paint().ifPresent(paint -> layers.add(Line.indented(Component.translatable("justmoreparts.share.import.paint", paint.colors().size()),
                Palette.TEXT_MUTED)));
        content.tune().ifPresent(tune -> layers.add(Line.indented(Component.translatable("justmoreparts.share.import.tune", tune.values().size()),
                Palette.TEXT_MUTED)));
        content.osd().ifPresent(osd -> layers.add(Line.indented(Component.translatable("justmoreparts.share.import.osd", osd.version()),
                Palette.TEXT_MUTED)));
        if (!layers.isEmpty()) {
            lines.add(Line.section(Component.translatable("justmoreparts.share.section.extras")));
            lines.addAll(layers);
        }
        if (decoded.partsKnown()) {
            if (creative) {
                lines.add(Line.section(Component.translatable("justmoreparts.share.section.shopping")));
                lines.add(Line.indented(Component.translatable("justmoreparts.share.shopping.creative"), Palette.TEXT_MUTED));
            } else if (shopping != null) {
                addShopping(lines, shopping);
            }
        }
        if (!decoded.skipped().isEmpty()) {
            lines.add(Line.of(Component.translatable("justmoreparts.share.import.skipped", Component.literal(String.join(", ", decoded.skipped()))),
                    Palette.TEXT_FAINT));
        }
        return lines;
    }

    private static MutableComponent partLine(ShareFormat.Slot slot, ShareDecoded decoded) {
        MutableComponent name;
        if (decoded.unknownParts().contains(slot.id())) {
            name = Component.literal(ShareTables.display(slot.id())).append(" ").append(Component.translatable("justmoreparts.share.part.unknown"));
        } else if (decoded.wrongKind().contains(slot.id())) {
            name = ShareFormat.partName(slot.id()).append(" ").append(Component.translatable("justmoreparts.share.part.wrong_kind"));
        } else {
            name = ShareFormat.partName(slot.id());
        }
        return ShareFormat.partLine(slot, name, "justmoreparts.share.import.part");
    }

    private static int partColor(Identifier id, ShareDecoded decoded) {
        if (decoded.unknownParts().contains(id)) {
            return Palette.ERROR;
        }
        return decoded.wrongKind().contains(id) ? Palette.WARNING : Palette.TEXT;
    }

    public static void addStats(List<Line> lines, BuildStats stats) {
        Locale locale = locale();
        lines.add(Line.section(Component.translatable("justmoreparts.share.section.stats")));
        lines.add(Line.indented(Component.translatable("justmoreparts.share.stats.mass", ShareFormat.number(stats.massGrams(), 0, locale),
                ShareFormat.number(stats.thrustToWeight(), 1, locale)), Palette.TEXT));
        lines.add(Line.indented(Component.translatable("justmoreparts.share.stats.time", ShareFormat.number(stats.hoverFlightTimeMin(), 1, locale),
                ShareFormat.number(stats.cruiseFlightTimeMin(), 1, locale)), Palette.TEXT));
        lines.add(Line.indented(Component.translatable("justmoreparts.share.stats.time_style", ShareFormat.number(stats.mixedFlightTimeMin(), 1, locale),
                ShareFormat.number(stats.aggressiveFlightTimeMin(), 1, locale)), Palette.TEXT));
        lines.add(Line.indented(Component.translatable("justmoreparts.share.stats.speed", ShareFormat.number(stats.topSpeedKmh(), 0, locale),
                ShareFormat.number(stats.fullThrottleCurrentA(), 0, locale)), Palette.TEXT));
        for (String warning : stats.warnings()) {
            lines.add(Line.indented(Component.translatable(BuildStats.warningTranslationKey(warning)), Palette.WARNING));
        }
    }

    public static void addShopping(List<Line> lines, ShoppingList shopping) {
        lines.add(Line.section(Component.translatable("justmoreparts.share.section.shopping")));
        for (ShoppingList.Entry entry : shopping.entries()) {
            MutableComponent name = ShareFormat.partName(entry.id());
            if (entry.missing() == 0) {
                lines.add(Line.indented(Component.translatable("justmoreparts.share.shopping.have", entry.needed(), name), Palette.SUCCESS));
            } else {
                lines.add(Line.indented(Component.translatable("justmoreparts.share.shopping.missing", entry.missing(), entry.needed(), name),
                        Palette.ERROR));
            }
        }
        int missing = shopping.missingPieces();
        lines.add(Line.indented(missing == 0 ? Component.translatable("justmoreparts.share.shopping.complete")
                : Component.translatable("justmoreparts.share.shopping.total", missing), missing == 0 ? Palette.SUCCESS : Palette.WARNING));
    }

    public static void addPaintCost(List<Line> lines, Map<String, Integer> cost) {
        if (cost.isEmpty()) {
            lines.add(Line.indented(Component.translatable("justmoreparts.share.paint_cost.free"), Palette.TEXT_MUTED));
            return;
        }
        for (Map.Entry<String, Integer> entry : cost.entrySet()) {
            lines.add(Line.indented(Component.translatable("justmoreparts.share.paint_cost.dye", entry.getValue(), ShareFormat.dyeName(entry.getKey())),
                    Palette.TEXT));
        }
    }

    public static List<Line> result(ImportResult result) {
        List<Line> lines = new ArrayList<>();
        int color = result.status().success() ? Palette.SUCCESS : result.status() == ImportStatus.BUSY ? Palette.WARNING : Palette.ERROR;
        lines.add(Line.of(Component.translatable(result.status().translationKey()), color));
        for (Map.Entry<Identifier, Integer> entry : ShoppingList.count(result.missingParts()).entrySet()) {
            lines.add(Line.indented(Component.translatable("justmoreparts.share.result.missing_part", entry.getValue(),
                    ShareFormat.partName(entry.getKey())), Palette.ERROR));
        }
        for (Map.Entry<String, Integer> entry : result.missingDyes().entrySet()) {
            lines.add(Line.indented(Component.translatable("justmoreparts.share.result.missing_dye", entry.getValue(), ShareFormat.dyeName(entry.getKey())),
                    Palette.ERROR));
        }
        return lines;
    }

    public static int height(Font font, List<Line> lines, int width) {
        int total = 0;
        for (Line line : lines) {
            total += rowsOf(font, line, width).size() * Metrics.LINE_HEIGHT + (line.section() ? 3 : 0);
        }
        return total;
    }

    public static int textWidth(Font font, List<Line> lines, int width, int height) {
        return height(font, lines, width) > height ? width - SCROLLBAR_SPACE : width;
    }

    public static int scrollHeight(Font font, List<Line> lines, int width, int height) {
        return height(font, lines, textWidth(font, lines, width, height));
    }

    private static List<FormattedCharSequence> rowsOf(Font font, Line line, int width) {
        return font.split(line.text(), Math.max(8, width - line.indent()));
    }

    public static boolean fullyVisible(int row, int top, int bottom) {
        return row >= top && row + Metrics.TEXT_HEIGHT + 1 <= bottom;
    }

    public static int render(GuiGraphicsExtractor g, Font font, List<Line> lines, int x, int y, int width, int height, int scroll) {
        int textWidth = textWidth(font, lines, width, height);
        int total = height(font, lines, textWidth);
        g.enableScissor(x, y, x + width, y + height);
        int row = y - scroll;
        for (Line line : lines) {
            if (line.section()) {
                row += 3;
            }
            boolean titleShown = false;
            for (FormattedCharSequence text : rowsOf(font, line, textWidth)) {
                if (fullyVisible(row, y, y + height)) {
                    ThemeText.text(g, font, text, x + line.indent(), row, line.color());
                    titleShown = true;
                }
                row += Metrics.LINE_HEIGHT;
            }
            if (line.section() && titleShown && row - 1 < y + height) {
                ThemeDraw.separator(g, x, x + textWidth, row - 1);
            }
        }
        g.disableScissor();
        if (total > height) {
            ThemeDraw.scrollbar(g, x + width - Metrics.SCROLLBAR_WIDTH, y, height, total, scroll, false);
        }
        return total;
    }

    public static List<String> wrapCode(Font font, String code, int width) {
        return ShareFormat.wrapCode(code, width, font::width);
    }

    public static int clampScroll(int scroll, int total, int height) {
        return Math.max(0, Math.min(scroll, Math.max(0, total - height)));
    }
}
