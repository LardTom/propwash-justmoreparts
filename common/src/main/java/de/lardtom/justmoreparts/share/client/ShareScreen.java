package de.lardtom.justmoreparts.share.client;

import de.lardtom.justmoreparts.share.ShareCodec;
import de.lardtom.justmoreparts.share.ShareContent;
import de.lardtom.justmoreparts.share.ShareException;
import de.lardtom.justmoreparts.share.ShareFormat;
import de.lardtom.justmoreparts.share.ShareLayer;
import de.lardtom.justmoreparts.share.ShareOsd;
import de.lardtom.justmoreparts.share.ShareTables;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.DroneData;
import de.lardtom.propwash.api.build.ImportLayer;
import de.lardtom.propwash.api.build.ImportResult;
import de.lardtom.propwash.api.build.ImportStatus;
import de.lardtom.propwash.api.client.OsdLayoutSpec;
import de.lardtom.propwash.api.client.PropwashClientApi;
import de.lardtom.propwash.api.client.WorkbenchContext;
import de.lardtom.propwash.ui.client.theme.Metrics;
import de.lardtom.propwash.ui.client.theme.Palette;
import de.lardtom.propwash.ui.client.theme.ThemeDraw;
import de.lardtom.propwash.ui.client.theme.ThemeText;
import de.lardtom.propwash.ui.client.theme.ThemedScreen;
import de.lardtom.propwash.ui.client.widget.FlatButton;
import de.lardtom.propwash.ui.client.widget.TextField;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class ShareScreen extends Screen implements ThemedScreen {
    public enum Mode {
        EXPORT,
        IMPORT
    }

    public static final List<ShareLayer> EXPORT_TOGGLES = List.of(ShareLayer.PAINT, ShareLayer.TUNE, ShareLayer.OSD, ShareLayer.NAME);
    public static final List<ShareLayer> APPLY_TOGGLES = List.of(ShareLayer.PARTS, ShareLayer.PAINT, ShareLayer.TUNE, ShareLayer.OSD);
    private static final int MAX_WIDTH = 460;
    private static final int MAX_HEIGHT = 272;
    private static final int PAD = 8;
    private static final int INPUT_HEIGHT = 16;
    private static final int BUSY_TICKS = 20;
    private static final int SHOPPING_REFRESH_TICKS = 10;
    private static final int MAX_INPUT_LENGTH = 8000;
    private static final int MIN_CODE_ROWS = 2;

    private static @Nullable ShareScreen active;

    private final Screen parent;
    private final WorkbenchContext context;
    private final ShareImportState state = new ShareImportState();
    private Mode mode;
    private boolean copyOnOpen;
    private @Nullable EditBox input;
    private @Nullable FlatButton copyButton;
    private @Nullable FlatButton applyButton;
    private final List<FlatButton> applyToggles = new ArrayList<>();
    private final List<FlatButton> exportToggles = new ArrayList<>();
    private Optional<DroneData> exportData = Optional.empty();
    private String exportCode = "";
    private List<String> exportRows = List.of();
    private List<ShareLines.Line> exportStats = List.of();
    private @Nullable Component exportStatus;
    private int exportStatusColor = Palette.TEXT_MUTED;
    private @Nullable CompletableFuture<ImportResult> pending;
    private @Nullable ImportResult result;
    private List<ShareLines.Line> resultLines = List.of();
    private @Nullable ShoppingList shopping;
    private int shoppingAge;
    private int busyTicks;
    private int scroll;
    private int infoTotal;
    private long frames;

    private int left;
    private int top;
    private int right;
    private int bottom;
    private int contentTop;
    private int footerTop;
    private int previewRight;
    private int columnX;
    private int columnWidth;
    private int infoTop;
    private int infoBottom;
    private int codeTop;
    private int codeBottom;
    private int codeMaxBottom;
    private int codeScroll;
    private int statsScroll;
    private int statsTotal;
    private int statsTop;
    private int statsHeight;
    private int titleRight;

    public ShareScreen(Screen parent, WorkbenchContext context, Mode mode) {
        super(Component.translatable("justmoreparts.share.title"));
        this.parent = parent;
        this.context = context;
        this.mode = mode;
        this.state.update(ShareSettings.lastInput());
    }

    public static void open(WorkbenchContext context, Mode mode, boolean copyNow) {
        context.openChild(parent -> {
            ShareScreen screen = new ShareScreen(parent, context, mode);
            screen.copyOnOpen = copyNow;
            return screen;
        });
    }

    public static @Nullable ShareScreen active() {
        return active;
    }

    public Mode mode() {
        return this.mode;
    }

    public ShareImportState state() {
        return this.state;
    }

    public String exportCode() {
        return this.exportCode;
    }

    public @Nullable ImportResult result() {
        return this.result;
    }

    public long frames() {
        return this.frames;
    }

    public WorkbenchContext context() {
        return this.context;
    }

    public void switchMode(Mode next) {
        if (this.mode != next) {
            this.mode = next;
            this.scroll = 0;
            this.rebuildWidgets();
        }
    }

    public void setInput(String text) {
        if (this.input != null) {
            this.input.setValue(text);
        }
        this.onInput(text);
    }

    @Override
    protected void init() {
        active = this;
        int w = Math.min(this.width - 16, MAX_WIDTH);
        int h = Math.min(this.height - 12, MAX_HEIGHT);
        this.left = (this.width - w) / 2;
        this.right = this.left + w;
        this.top = (this.height - h) / 2;
        this.bottom = this.top + h;
        this.contentTop = this.top + Metrics.HEADER_HEIGHT + PAD;
        this.footerTop = this.bottom - Metrics.FOOTER_HEIGHT;
        this.previewRight = this.left + Math.max(120, w * 2 / 5);
        this.columnX = this.previewRight + PAD;
        this.columnWidth = this.right - PAD - this.columnX;
        this.applyToggles.clear();
        this.exportToggles.clear();
        this.copyButton = null;
        this.applyButton = null;
        this.input = null;

        Component exportTab = Component.translatable("justmoreparts.share.tab.export");
        Component importTab = Component.translatable("justmoreparts.share.tab.import");
        int tabImportWidth = this.font.width(importTab) + 16;
        int tabExportWidth = this.font.width(exportTab) + 16;
        int tabY = this.top + (Metrics.HEADER_HEIGHT - 1 - Metrics.ROW_HEIGHT) / 2;
        int tabX = this.right - PAD - tabImportWidth;
        this.addRenderableWidget(new FlatButton(tabX, tabY, tabImportWidth, Metrics.ROW_HEIGHT, importTab, FlatButton.Style.TAB,
                () -> this.mode == Mode.IMPORT, () -> this.switchMode(Mode.IMPORT)));
        this.addRenderableWidget(new FlatButton(tabX - Metrics.GAP - tabExportWidth, tabY, tabExportWidth, Metrics.ROW_HEIGHT, exportTab,
                FlatButton.Style.TAB, () -> this.mode == Mode.EXPORT, () -> this.switchMode(Mode.EXPORT)));
        this.titleRight = tabX - 2 * Metrics.GAP - tabExportWidth;

        int buttonY = this.footerTop + (Metrics.FOOTER_HEIGHT - Metrics.CONTROL_HEIGHT) / 2;
        Component back = Component.translatable("justmoreparts.share.back");
        int backWidth = Math.max(60, this.font.width(back) + 16);
        this.addRenderableWidget(new FlatButton(this.right - PAD - backWidth, buttonY, backWidth, Metrics.CONTROL_HEIGHT, back,
                FlatButton.Style.NORMAL, this::onClose));

        if (this.mode == Mode.EXPORT) {
            this.initExport(buttonY);
        } else {
            this.initImport(buttonY);
        }
        this.updateStates();
    }

    private void initExport(int buttonY) {
        int y = this.contentTop + Metrics.SECTION_HEIGHT + Metrics.SECTION_GAP;
        int count = EXPORT_TOGGLES.size();
        int toggleWidth = (this.columnWidth - (count - 1) * Metrics.GAP) / count;
        for (int i = 0; i < count; i++) {
            ShareLayer layer = EXPORT_TOGGLES.get(i);
            FlatButton toggle = new FlatButton(this.columnX + i * (toggleWidth + Metrics.GAP), y, toggleWidth, Metrics.CONTROL_HEIGHT,
                    Component.translatable(layer.translationKey()), FlatButton.Style.NORMAL,
                    () -> ShareSettings.exports(layer) && this.exportable(layer), () -> {
                ShareSettings.toggleExport(layer);
                this.refreshExport();
            });
            toggle.setTooltip(Tooltip.create(Component.translatable("justmoreparts.share.export.toggle." + layer.key())));
            this.exportToggles.add(this.addRenderableWidget(toggle));
        }
        this.codeTop = y + Metrics.ROW_HEIGHT + Metrics.GAP + Metrics.SECTION_HEIGHT + Metrics.SECTION_GAP;
        int codeMax = this.footerTop - PAD - 2 * Metrics.LINE_HEIGHT - Metrics.GAP;
        int statsReserve = 3 * Metrics.LINE_HEIGHT + Metrics.GAP;
        boolean roomForStats = codeMax - statsReserve >= this.codeTop + MIN_CODE_ROWS * Metrics.LINE_HEIGHT + 7;
        this.codeMaxBottom = roomForStats ? codeMax - statsReserve : codeMax;
        this.codeBottom = this.codeMaxBottom;
        Component copy = Component.translatable("justmoreparts.share.export.copy");
        int copyWidth = this.font.width(copy) + 20;
        this.copyButton = this.addRenderableWidget(new FlatButton(this.left + PAD, buttonY, copyWidth, Metrics.CONTROL_HEIGHT, copy,
                FlatButton.Style.PRIMARY, this::copy));
        this.refreshExport();
        if (this.copyOnOpen) {
            this.copyOnOpen = false;
            this.copy();
        }
    }

    private void initImport(int buttonY) {
        Component paste = Component.translatable("justmoreparts.share.import.paste");
        int pasteWidth = this.font.width(paste) + 16;
        int inputWidth = this.right - PAD - pasteWidth - Metrics.GAP - (this.left + PAD);
        EditBox box = new TextField(this.font, this.left + PAD, this.contentTop, inputWidth, INPUT_HEIGHT, Component.translatable("justmoreparts.share.import.field"));
        box.setMaxLength(MAX_INPUT_LENGTH);
        box.setHint(ThemeText.colored(Component.translatable("justmoreparts.share.import.hint"), Palette.TEXT_DISABLED));
        box.setValue(this.state.text());
        box.moveCursorToStart(false);
        box.setResponder(this::onInput);
        this.input = this.addRenderableWidget(box);
        FlatButton pasteButton = new FlatButton(this.right - PAD - pasteWidth, this.contentTop + (INPUT_HEIGHT - Metrics.CONTROL_HEIGHT) / 2,
                pasteWidth, Metrics.CONTROL_HEIGHT, paste, FlatButton.Style.NORMAL, this::paste);
        pasteButton.setTooltip(Tooltip.create(Component.translatable("justmoreparts.share.import.paste.tooltip")));
        this.addRenderableWidget(pasteButton);

        int toggleY = this.footerTop - PAD - Metrics.CONTROL_HEIGHT;
        int labelWidth = this.font.width(Component.translatable("justmoreparts.share.import.apply_label")) + Metrics.GAP;
        int count = APPLY_TOGGLES.size();
        int toggleWidth = (this.columnWidth - labelWidth - (count - 1) * Metrics.GAP) / count;
        if (toggleWidth < 36) {
            labelWidth = 0;
            toggleWidth = (this.columnWidth - (count - 1) * Metrics.GAP) / count;
        }
        for (int i = 0; i < count; i++) {
            ShareLayer layer = APPLY_TOGGLES.get(i);
            FlatButton toggle = new FlatButton(this.columnX + labelWidth + i * (toggleWidth + Metrics.GAP), toggleY, toggleWidth,
                    Metrics.CONTROL_HEIGHT, Component.translatable(layer.translationKey()), FlatButton.Style.NORMAL,
                    () -> ShareSettings.applies(layer) && this.available(layer), () -> {
                ShareSettings.toggleApply(layer);
                this.updateStates();
            });
            toggle.setTooltip(Tooltip.create(Component.translatable("justmoreparts.share.import.toggle." + layer.key())));
            this.applyToggles.add(this.addRenderableWidget(toggle));
        }
        this.infoTop = this.contentTop + INPUT_HEIGHT + PAD;
        this.infoBottom = toggleY - Metrics.GAP;
        Component apply = Component.translatable("justmoreparts.share.import.apply");
        int applyWidth = this.font.width(apply) + 20;
        this.applyButton = this.addRenderableWidget(new FlatButton(this.left + PAD, buttonY, applyWidth, Metrics.CONTROL_HEIGHT, apply,
                FlatButton.Style.PRIMARY, this::apply));
        this.setInitialFocus(box);
    }

    public boolean exportable(ShareLayer layer) {
        return layer != ShareLayer.NAME || this.exportData.flatMap(DroneData::name).isPresent();
    }

    private boolean available(ShareLayer layer) {
        Optional<ShareContent> content = this.state.content();
        if (content.isEmpty()) {
            return false;
        }
        return switch (layer) {
            case PARTS -> this.state.decoded().partsKnown() && this.state.buildable();
            case PAINT -> content.get().paint().isPresent();
            case TUNE -> content.get().tune().isPresent();
            case OSD -> content.get().osd().isPresent();
            default -> false;
        };
    }

    public Set<ShareLayer> chosenLayers() {
        EnumSet<ShareLayer> out = EnumSet.noneOf(ShareLayer.class);
        for (ShareLayer layer : APPLY_TOGGLES) {
            if (ShareSettings.applies(layer) && this.available(layer)) {
                out.add(layer);
            }
        }
        return out;
    }

    private void onInput(String text) {
        ShareSettings.lastInput(text);
        if (this.state.update(text)) {
            this.scroll = 0;
            this.result = null;
            this.resultLines = List.of();
            this.pending = null;
            this.shopping = null;
            this.shoppingAge = SHOPPING_REFRESH_TICKS;
            this.updateStates();
        }
    }

    public void paste() {
        String text = ShareConfigTab.compact(this.minecraft.keyboardHandler.getClipboard());
        String clipped = text.length() > MAX_INPUT_LENGTH ? text.substring(0, MAX_INPUT_LENGTH) : text;
        this.setInput(clipped);
    }

    private void refreshExport() {
        this.exportData = this.context.result();
        this.exportCode = "";
        this.exportStatus = null;
        this.exportStats = List.of();
        this.layoutCode();
        this.updateExportTooltips();
        if (this.exportData.isEmpty()) {
            this.exportStatus = Component.translatable("justmoreparts.share.export.no_result");
            this.exportStatusColor = Palette.WARNING;
            this.updateStates();
            return;
        }
        Set<ShareLayer> layers = ShareSettings.exportLayers();
        try {
            Optional<ShareOsd> osd = Optional.empty();
            if (layers.contains(ShareLayer.OSD)) {
                OsdLayoutSpec current = PropwashClientApi.osd().current();
                osd = Optional.of(new ShareOsd(current.version(), current.json()));
            }
            this.exportCode = ShareCodec.encode(ShareContent.of(this.exportData.get(), layers, osd));
        } catch (IllegalArgumentException e) {
            this.exportStatus = Component.translatable("justmoreparts.share.export.not_shareable", ShareException.reason(e));
            this.exportStatusColor = Palette.ERROR;
        }
        if (!this.exportCode.isEmpty() && !ShareCodec.fits(this.exportCode)) {
            this.exportStatus = Component.translatable("justmoreparts.share.export.too_long", this.exportCode.length(), ShareTables.MAX_INPUT_CHARS);
            this.exportStatusColor = Palette.ERROR;
        }
        List<ShareLines.Line> stats = new ArrayList<>();
        PropwashApi.builds().analyze(this.exportData.get().build()).ifPresent(analysis -> ShareLines.addStats(stats, analysis));
        this.exportStats = List.copyOf(stats);
        this.layoutCode();
        this.updateStates();
    }

    private void layoutCode() {
        this.codeScroll = 0;
        this.exportRows = this.exportCode.isEmpty() ? List.of() : ShareLines.wrapCode(this.font, this.exportCode, this.columnWidth - 8);
        if (this.exportRows.size() > this.codeRowsShown(this.codeMaxBottom)) {
            this.exportRows = ShareLines.wrapCode(this.font, this.exportCode, this.columnWidth - 8 - ShareLines.SCROLLBAR_SPACE);
        }
        int rows = Math.max(MIN_CODE_ROWS, this.exportRows.size());
        this.codeBottom = Math.min(this.codeMaxBottom, this.codeTop + rows * Metrics.LINE_HEIGHT + 7);
    }

    private int codeRowsShown(int bottom) {
        return Math.max(1, (bottom - 2 - (this.codeTop + 4)) / Metrics.LINE_HEIGHT);
    }

    int codeScroll() {
        return this.codeScroll;
    }

    int codeRowsShown() {
        return this.codeRowsShown(this.codeBottom);
    }

    int[] codeCenter() {
        return new int[]{(this.columnX + this.right - PAD) / 2, (this.codeTop + this.codeBottom) / 2};
    }

    int statsHeight() {
        return this.footerTop - PAD - (this.codeBottom + Metrics.GAP + 2 * Metrics.LINE_HEIGHT + Metrics.GAP);
    }

    private boolean overCode(double mouseX, double mouseY) {
        return mouseX >= this.columnX && mouseX < this.right - PAD && mouseY >= this.codeTop && mouseY < this.codeBottom;
    }

    private void updateExportTooltips() {
        for (int i = 0; i < this.exportToggles.size() && i < EXPORT_TOGGLES.size(); i++) {
            ShareLayer layer = EXPORT_TOGGLES.get(i);
            String key = "justmoreparts.share.export.toggle." + layer.key() + (this.exportable(layer) ? "" : ".none");
            this.exportToggles.get(i).setTooltip(Tooltip.create(Component.translatable(key)));
        }
    }

    public List<String> exportRows() {
        return this.exportRows;
    }

    public boolean copy() {
        if (this.exportCode.isEmpty() || !ShareCodec.fits(this.exportCode)) {
            return false;
        }
        this.minecraft.keyboardHandler.setClipboard(this.exportCode);
        this.exportStatus = Component.translatable("justmoreparts.share.export.copied", this.exportCode.length());
        this.exportStatusColor = Palette.SUCCESS;
        return true;
    }

    public static boolean applyOsd(ShareOsd osd) {
        OsdLayoutSpec layout;
        try {
            layout = new OsdLayoutSpec(osd.version(), osd.json());
        } catch (IllegalArgumentException e) {
            return false;
        }
        return PropwashClientApi.osd().validate(layout) && PropwashClientApi.osd().apply(layout);
    }

    public @Nullable CompletableFuture<ImportResult> apply() {
        if (this.state.content().isEmpty() || this.pending != null && !this.pending.isDone() || this.busyTicks > 0) {
            return null;
        }
        ShareContent content = this.state.content().get();
        Set<ShareLayer> chosen = this.chosenLayers();
        List<ShareLines.Line> lines = new ArrayList<>();
        if (chosen.contains(ShareLayer.OSD) && content.osd().isPresent()) {
            boolean applied = applyOsd(content.osd().get());
            lines.add(ShareLines.Line.of(Component.translatable(applied ? "justmoreparts.share.osd.applied" : "justmoreparts.share.osd.invalid"),
                    applied ? Palette.SUCCESS : Palette.ERROR));
        }
        EnumSet<ImportLayer> layers = EnumSet.noneOf(ImportLayer.class);
        if (chosen.contains(ShareLayer.PARTS)) {
            layers.add(ImportLayer.PARTS);
            if (content.name().isPresent()) {
                layers.add(ImportLayer.NAME);
            }
        }
        if (chosen.contains(ShareLayer.PAINT)) {
            layers.add(ImportLayer.PAINT);
        }
        if (chosen.contains(ShareLayer.TUNE)) {
            layers.add(ImportLayer.TUNE);
        }
        this.resultLines = lines;
        this.result = null;
        if (layers.isEmpty()) {
            this.pending = null;
            return null;
        }
        DroneData data = content.toDroneData();
        CompletableFuture<ImportResult> future = this.context.importIntoWorkbench(data, layers, ShareTables.TUNE_GROUPS);
        this.pending = future;
        List<ShareLines.Line> osdLines = List.copyOf(lines);
        future.thenAccept(done -> {
            if (this.pending != future) {
                return;
            }
            this.result = done;
            List<ShareLines.Line> all = new ArrayList<>(osdLines);
            all.addAll(ShareLines.result(done));
            this.resultLines = all;
            if (done.status() == ImportStatus.BUSY) {
                this.busyTicks = BUSY_TICKS;
            }
            this.shoppingAge = SHOPPING_REFRESH_TICKS;
            this.refreshExport();
        });
        this.updateStates();
        return future;
    }

    private void updateStates() {
        if (this.copyButton != null) {
            this.copyButton.active = !this.exportCode.isEmpty() && ShareCodec.fits(this.exportCode);
        }
        if (this.applyButton != null) {
            boolean waiting = this.pending != null && !this.pending.isDone();
            this.applyButton.active = !waiting && this.busyTicks <= 0 && !this.chosenLayers().isEmpty();
        }
        for (int i = 0; i < this.applyToggles.size() && i < APPLY_TOGGLES.size(); i++) {
            this.applyToggles.get(i).active = this.available(APPLY_TOGGLES.get(i));
        }
        for (int i = 0; i < this.exportToggles.size() && i < EXPORT_TOGGLES.size(); i++) {
            this.exportToggles.get(i).active = this.exportable(EXPORT_TOGGLES.get(i));
        }
    }

    @Override
    public void tick() {
        if (this.busyTicks > 0) {
            this.busyTicks--;
        }
        if (this.mode == Mode.EXPORT && !this.context.result().equals(this.exportData)) {
            this.refreshExport();
        }
        if (this.mode == Mode.IMPORT && ++this.shoppingAge >= SHOPPING_REFRESH_TICKS) {
            this.shoppingAge = 0;
            this.shopping = this.state.decoded().partsKnown() && this.state.content().isPresent()
                    ? ShoppingList.fromMissing(this.state.decoded().require().build(), this.context.missingParts(this.state.content().get().toDroneData()))
                    : null;
        }
        this.updateStates();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.mode == Mode.IMPORT && mouseX >= this.columnX && mouseX < this.right - PAD && mouseY >= this.infoTop && mouseY < this.infoBottom) {
            int height = this.infoBottom - this.infoTop;
            this.scroll = ShareLines.clampScroll(this.scroll - (int) Math.round(scrollY * Metrics.SCROLL_STEP), this.infoTotal, height);
            return true;
        }
        if (this.mode == Mode.EXPORT && this.overCode(mouseX, mouseY) && this.exportRows.size() > this.codeRowsShown(this.codeBottom)) {
            this.codeScroll = Math.max(0, Math.min(this.codeScroll - (int) Math.signum(scrollY), this.exportRows.size() - this.codeRowsShown(this.codeBottom)));
            return true;
        }
        if (this.mode == Mode.EXPORT && mouseX >= this.columnX && mouseX < this.right - PAD && mouseY >= this.statsTop
                && mouseY < this.statsTop + this.statsHeight && this.statsTotal > this.statsHeight) {
            this.statsScroll = ShareLines.clampScroll(this.statsScroll - (int) Math.round(scrollY * Metrics.SCROLL_STEP), this.statsTotal, this.statsHeight);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public void removed() {
        if (active == this) {
            active = null;
        }
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
        ThemeDraw.window(g, this.left, this.top, this.right, this.bottom);
        ThemeDraw.brandedHeader(g, this.font, this.left, this.top, this.right, Metrics.HEADER_HEIGHT, this.title, this.titleRight);
        int stageTop = this.mode == Mode.IMPORT ? this.contentTop + INPUT_HEIGHT + PAD : this.contentTop;
        int stageBottom = this.footerTop - PAD;
        ThemeDraw.previewStage(g, this.left + PAD, stageTop, this.previewRight, stageBottom);
        Optional<DroneData> preview = this.mode == Mode.EXPORT ? this.exportData : this.state.preview();
        if (preview.isPresent()) {
            float yaw = (float) ((System.currentTimeMillis() % 14000L) / 14000.0 * 360.0);
            PropwashClientApi.renderPreview(g, preview.get(), this.left + PAD, stageTop, this.previewRight, stageBottom, yaw);
        } else {
            Component hint = this.mode == Mode.EXPORT ? Component.translatable("justmoreparts.share.preview.no_result")
                    : this.state.decoded().ok() ? Component.translatable("justmoreparts.share.preview.unknown")
                    : Component.translatable("justmoreparts.share.preview.empty");
            int hintWidth = this.previewRight - this.left - 2 * PAD - 8;
            int hintHeight = ThemeText.wrappedHeight(this.font, hint, hintWidth);
            ThemeText.wrapped(g, this.font, hint, this.left + PAD + 4 + 4, (stageTop + stageBottom - hintHeight) / 2, hintWidth, Palette.TEXT_FAINT);
        }
        preview.flatMap(DroneData::name).ifPresent(name -> ThemeText.line(g, this.font, Component.literal(name), this.left + PAD + 4,
                this.previewRight - 4, stageTop + 4, Palette.TEXT, ThemeText.Align.LEFT, true));
        if (this.mode == Mode.EXPORT) {
            this.extractExport(g);
        } else {
            this.extractImport(g);
        }
        ThemeDraw.footerBar(g, this.left, this.footerTop, this.right, this.bottom);
        super.extractRenderState(g, mouseX, mouseY, a);
        ThemeDraw.windowBorder(g, this.left, this.top, this.right, this.bottom);
    }

    private void extractExport(GuiGraphicsExtractor g) {
        ThemeDraw.sectionTitle(g, this.font, Component.translatable("justmoreparts.share.export.layers"), this.columnX, this.contentTop,
                this.columnWidth);
        int titleY = this.codeTop - Metrics.SECTION_HEIGHT - Metrics.SECTION_GAP;
        Component codeTitle = this.exportCode.isEmpty() ? Component.translatable("justmoreparts.share.export.code")
                : Component.translatable("justmoreparts.share.export.code_length", this.exportCode.length());
        ThemeDraw.sectionTitle(g, this.font, codeTitle, this.columnX, titleY, this.columnWidth);
        ThemeDraw.framedBox(g, this.columnX, this.codeTop, this.right - PAD, this.codeBottom, Palette.FIELD, Palette.BORDER);
        int shownRows = this.codeRowsShown(this.codeBottom);
        this.codeScroll = Math.max(0, Math.min(this.codeScroll, this.exportRows.size() - shownRows));
        int rowY = this.codeTop + 4;
        for (int i = this.codeScroll; i < this.exportRows.size() && i < this.codeScroll + shownRows; i++) {
            ThemeText.text(g, this.font, this.exportRows.get(i), this.columnX + 4, rowY, Palette.TEXT);
            rowY += Metrics.LINE_HEIGHT;
        }
        if (this.exportRows.size() > shownRows) {
            ThemeDraw.scrollbar(g, this.right - PAD - 1 - Metrics.SCROLLBAR_WIDTH, this.codeTop + 1, this.codeBottom - this.codeTop - 2,
                    this.exportRows.size() * Metrics.LINE_HEIGHT, this.codeScroll * Metrics.LINE_HEIGHT, false);
        }
        int y = this.codeBottom + Metrics.GAP;
        if (this.exportData.isPresent() && !this.exportCode.isEmpty()) {
            Component inside = Component.translatable("justmoreparts.share.export.contains",
                    ShareFormat.layerList(ShareContent.of(this.exportData.get(), ShareSettings.exportLayers(), Optional.empty()).layers()));
            ThemeText.line(g, this.font, inside, this.columnX, this.right - PAD, y, Palette.TEXT_MUTED, ThemeText.Align.LEFT, true);
        }
        if (this.exportStatus != null) {
            ThemeText.line(g, this.font, this.exportStatus, this.columnX, this.right - PAD, y + Metrics.LINE_HEIGHT, this.exportStatusColor,
                    ThemeText.Align.LEFT, true);
        }
        this.statsTop = y + 2 * Metrics.LINE_HEIGHT + Metrics.GAP;
        this.statsHeight = this.footerTop - PAD - this.statsTop;
        this.statsTotal = 0;
        if (!this.exportCode.isEmpty() && this.statsHeight > Metrics.LINE_HEIGHT) {
            this.statsTotal = ShareLines.scrollHeight(this.font, this.exportStats, this.columnWidth, this.statsHeight);
            this.statsScroll = ShareLines.clampScroll(this.statsScroll, this.statsTotal, this.statsHeight);
            ShareLines.render(g, this.font, this.exportStats, this.columnX, this.statsTop, this.columnWidth, this.statsHeight, this.statsScroll);
        }
    }

    private void extractImport(GuiGraphicsExtractor g) {
        boolean creative = this.context.creative();
        List<ShareLines.Line> lines = new ArrayList<>(this.resultLines);
        if (this.pending != null && !this.pending.isDone()) {
            lines.add(0, ShareLines.Line.of(Component.translatable("justmoreparts.share.import.sending"), Palette.ACCENT));
        }
        lines.addAll(ShareLines.describe(this.state, this.shopping, creative));
        int height = this.infoBottom - this.infoTop;
        this.infoTotal = ShareLines.scrollHeight(this.font, lines, this.columnWidth, height);
        this.scroll = ShareLines.clampScroll(this.scroll, this.infoTotal, height);
        ShareLines.render(g, this.font, lines, this.columnX, this.infoTop, this.columnWidth, height, this.scroll);
        if (!this.applyToggles.isEmpty() && this.applyToggles.getFirst().getX() > this.columnX) {
            ThemeText.text(g, this.font, Component.translatable("justmoreparts.share.import.apply_label"), this.columnX,
                    Metrics.textY(this.applyToggles.getFirst().getY(), Metrics.CONTROL_HEIGHT), Palette.TEXT_MUTED);
        }
    }
}
