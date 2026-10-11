package de.lardtom.justmoreparts.share.client;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.command.JmpCommands;
import de.lardtom.justmoreparts.dev.SelfTest;
import de.lardtom.justmoreparts.dev.SelfTest.StepResult;
import de.lardtom.justmoreparts.share.ShareCodec;
import de.lardtom.justmoreparts.share.ShareContent;
import de.lardtom.justmoreparts.share.ShareDecoded;
import de.lardtom.justmoreparts.share.ShareLayer;
import de.lardtom.justmoreparts.share.ShareOsd;
import de.lardtom.justmoreparts.share.ShareStatus;
import de.lardtom.justmoreparts.share.ShareVectors;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.DroneData;
import de.lardtom.propwash.api.build.ImportRequest;
import de.lardtom.propwash.api.build.ImportResult;
import de.lardtom.propwash.api.build.ImportStatus;
import de.lardtom.propwash.api.build.PaintSpec;
import de.lardtom.propwash.api.build.TuneSpec;
import de.lardtom.propwash.api.client.OsdLayoutSpec;
import de.lardtom.propwash.api.client.PropwashClientApi;
import de.lardtom.propwash.api.client.WorkbenchContext;
import de.lardtom.propwash.guide.scene.Scene;
import de.lardtom.propwash.ui.client.theme.Metrics;
import de.lardtom.propwash.ui.client.theme.ThemedScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class ShareSelfTests {
    private static final String PHASE = "phase";
    private static final String SINCE = "since";
    private static final String FUTURE = "future";
    private static final String SERVER = "server";
    private static final String CODE = "code";
    private static final String SAVED = "saved";
    private static final int REQUEST_GAP_TICKS = 14;
    private static final AtomicReference<String> SERVER_ERROR = new AtomicReference<>();
    private static final AtomicInteger COMMAND_RESULT = new AtomicInteger();

    private ShareSelfTests() {
    }

    public static void register() {
        SelfTest.clientStep("share: Codec liest alle PW1- und PW2-Testvektoren", 20, ShareSelfTests::vectorsStep);
        SelfTest.clientStep("share: Werkbank-Knöpfe, Konfigurator-Tab und Flugschul-Szene angemeldet", 200, ShareSelfTests::registrationStep);
        SelfTest.clientStep("share: Werkbank → Code → Import in die Werkbank (Kreativ)", 1400, ShareSelfTests::workbenchStep);
        SelfTest.clientStep("share: Tuning und Lack auf die gehaltene Drohne", 900, ShareSelfTests::heldStep);
        SelfTest.clientStep("share: OSD-Layout-Rundreise", 40, ShareSelfTests::osdStep);
        SelfTest.clientStep("share: Konfigurator-Tab im Theme-Fenster", 300, ShareSelfTests::tabStep);
        SelfTest.clientStep("share: Befehle info und give", 200, ShareSelfTests::commandStep);
        SelfTest.clientStep("share: aufräumen", 40, ShareSelfTests::cleanupStep);
    }

    private static StepResult vectorsStep(SelfTest.StepContext ctx) {
        List<String> errors = new ArrayList<>();
        for (ShareVectors.Vector vector : ShareVectors.all()) {
            ShareDecoded decoded = ShareCodec.decode(vector.code());
            for (String error : ShareVectors.check(vector, decoded)) {
                errors.add(vector.name() + ": " + error);
            }
        }
        ShareDecoded a = ShareCodec.decode(ShareVectors.A);
        if (!a.partsKnown()) {
            errors.add("Vektor A: eingebaute Teile unbekannt " + a.unknownParts());
        }
        ShareDecoded shortA = ShareCodec.decode(ShareVectors.P2_A);
        if (!shortA.partsKnown() || !shortA.require().equals(a.require())) {
            errors.add("Vektor PW2 A: " + shortA.status() + " " + shortA.unknownParts());
        }
        return StepResult.check(errors.isEmpty(), String.join("; ", errors));
    }

    private static StepResult registrationStep(SelfTest.StepContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        Map<String, Object> state = ctx.state();
        int phase = (int) state.getOrDefault(PHASE, 0);
        if (phase == 0) {
            List<String> errors = new ArrayList<>();
            if (PropwashClientApi.workbenchExtensions().stream().noneMatch(e -> e.id().equals(ShareWorkbenchExtension.ID))) {
                errors.add("Werkbank-Erweiterung fehlt");
            }
            ShareConfigTab tab = new ShareConfigTab();
            if (!ShareConfigTab.ID.equals(tab.id()) || tab.order() != ShareConfigTab.ORDER || tab.readOnly()) {
                errors.add("Konfigurator-Tab falsch: " + tab.id());
            }
            Scene scene = ShareScene.create();
            for (String key : scene.translationKeys()) {
                if (!Language.getInstance().has(key)) {
                    errors.add("Szenentext fehlt: " + key);
                }
            }
            List<String> keys = new ArrayList<>(ShareWorkbenchExtension.LABEL_KEYS);
            keys.addAll(List.of(ShareWorkbenchExtension.TOOLTIP_KEY, "justmoreparts.share.title", "justmoreparts.share.export.toggle.name.none",
                    ShareStatus.CHECKSUM.translationKey(), ShareLayer.OSD.translationKey()));
            for (String key : keys) {
                if (!Language.getInstance().has(key)) {
                    errors.add("Text fehlt: " + key);
                }
            }
            if (!errors.isEmpty()) {
                return StepResult.fail(String.join("; ", errors));
            }
            if (!PropwashClientApi.openFlightSchoolScene(ShareScene.ID, null)) {
                return StepResult.fail("Flugschul-Szene " + ShareScene.ID + " lässt sich nicht öffnen");
            }
            state.put(PHASE, 1);
            state.put(SINCE, ctx.tick());
            return StepResult.pending();
        }
        if (ctx.tick() - (int) state.get(SINCE) < 40) {
            return StepResult.pending();
        }
        boolean themed = ThemedScreen.active();
        String screen = String.valueOf(mc.gui.screen());
        mc.gui.setScreen(null);
        return StepResult.check(themed, "Flugschul-Szene nicht im Theme: " + screen);
    }

    private static DroneData freestyle() {
        return new DroneData(ShareScene.freestyle(), PaintSpec.DEFAULT.with("frame", 0x1B1D22).with("motors", 0xE8B622),
                new TuneSpec(Map.of("rates.roll.rcRate", 1.25F, "pid.roll.p", 47.0F)), Optional.of("JMP Selbsttest"));
    }

    private static StepResult workbenchStep(SelfTest.StepContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        Map<String, Object> state = ctx.state();
        int phase = (int) state.getOrDefault(PHASE, 0);
        switch (phase) {
            case 0 -> {
                SERVER_ERROR.set(null);
                state.put(SERVER, ctx.onServer(ShareSelfTests::openBench));
                state.put(PHASE, 1);
                return StepResult.pending();
            }
            case 1 -> {
                StepResult server = awaitServer(ctx);
                if (server.status() != StepResult.Status.PASS) {
                    return server;
                }
                WorkbenchContext context = ShareWorkbenchExtension.lastContext();
                if (context == null || context.screen() != mc.gui.screen()) {
                    return timeout(ctx, 400, "Werkbank ruft die Sharecode-Erweiterung nicht auf: " + mc.gui.screen());
                }
                if (!context.creative()) {
                    return StepResult.fail("Selbsttest-Welt nicht im Kreativmodus");
                }
                if (context.headerSpace() < WorkbenchContext.ICON_BUTTON_SIZE + WorkbenchContext.BUTTON_GAP) {
                    return StepResult.fail("Werkbank meldet zu wenig Platz für den Sharecode-Knopf: " + context.headerSpace() + " px");
                }
                ctx.log("Werkbank-Kopf: " + context.headerSpace() + " px frei, Sharecode-Knopf " + (ShareWorkbenchExtension.asText(context)
                        ? "mit Text" : "als Symbol"));
                if (context.result().isEmpty()) {
                    ShareWorkbenchExtension.open(context);
                } else {
                    ShareScreen.open(context, ShareScreen.Mode.IMPORT, false);
                }
                state.put(PHASE, 2);
                return StepResult.pending();
            }
            case 2 -> {
                ShareScreen screen = ShareScreen.active();
                if (screen == null || mc.gui.screen() != screen || screen.frames() < 3) {
                    return timeout(ctx, 600, "Sharecode-Fenster erscheint nicht: " + mc.gui.screen());
                }
                if (!ThemedScreen.active()) {
                    return StepResult.fail("Sharecode-Fenster nicht im Theme");
                }
                if (screen.mode() != ShareScreen.Mode.IMPORT) {
                    return StepResult.fail("Sharecode-Knopf öffnet bei leerer Werkbank nicht „Importieren“: " + screen.mode());
                }
                String code = ShareCodec.encode(ShareContent.of(freestyle(), ShareLayer.DEFAULT_EXPORT, Optional.empty()));
                state.put(CODE, code);
                screen.setInput(code);
                if (!screen.state().decoded().partsKnown() || !screen.state().buildable()) {
                    return StepResult.fail("eingefügter Code nicht übernehmbar: " + screen.state().decoded());
                }
                ShareSettings.setApply(EnumSet.of(ShareLayer.PARTS, ShareLayer.PAINT, ShareLayer.TUNE));
                CompletableFuture<ImportResult> future = screen.apply();
                if (future == null) {
                    return StepResult.fail("Übernehmen schickt keine Anfrage: " + screen.chosenLayers());
                }
                state.put(FUTURE, future);
                state.put(PHASE, 3);
                return StepResult.pending();
            }
            case 3 -> {
                ImportResult result = result(state);
                if (result == null) {
                    return timeout(ctx, 900, "keine Antwort auf den Import in die Werkbank");
                }
                if (result.status() != ImportStatus.APPLIED) {
                    return StepResult.fail("Import in die Werkbank: " + result);
                }
                ShareScreen screen = ShareScreen.active();
                if (screen == null) {
                    return StepResult.fail("Sharecode-Fenster nach dem Import geschlossen");
                }
                screen.switchMode(ShareScreen.Mode.EXPORT);
                state.put(PHASE, 4);
                return StepResult.pending();
            }
            case 4 -> {
                ShareScreen screen = ShareScreen.active();
                if (screen == null || screen.mode() != ShareScreen.Mode.EXPORT || screen.exportCode().isEmpty()) {
                    return timeout(ctx, 1200, "Werkbank liefert nach dem Import keine fertige Drohne");
                }
                ShareDecoded decoded = ShareCodec.decode(screen.exportCode());
                DroneData expected = freestyle();
                if (!decoded.ok() || !decoded.require().build().equals(expected.build())
                        || !decoded.require().paint().equals(Optional.of(expected.paint()))) {
                    return StepResult.fail("Export der Werkbank passt nicht zum Import: " + decoded.content());
                }
                Float roll = decoded.require().tune().map(t -> t.values().get("rates.roll.rcRate")).orElse(null);
                if (roll == null || Math.abs(roll - 1.25F) > 1.0E-4F) {
                    return StepResult.fail("Tuning nicht in der Werkbank-Drohne: " + decoded.require().tune());
                }
                if (!String.join("", screen.exportRows()).equals(screen.exportCode()) || screen.exportRows().stream().anyMatch(r -> r.contains(" "))) {
                    return StepResult.fail("Code-Kasten zeigt den Code nicht unverändert in Zeilen: " + screen.exportRows());
                }
                String small = smallGui(ctx, mc, screen);
                if (small != null) {
                    return StepResult.fail(small);
                }
                if (!screen.copy() || !mc.keyboardHandler.getClipboard().equals(screen.exportCode())) {
                    return StepResult.fail("Kopieren in die Zwischenablage klappt nicht");
                }
                screen.onClose();
                state.put(PHASE, 5);
                return StepResult.pending();
            }
            default -> {
                WorkbenchContext context = ShareWorkbenchExtension.lastContext();
                if (context == null || mc.gui.screen() != context.screen()) {
                    return timeout(ctx, 1300, "„Zurück“ führt nicht zur Werkbank: " + mc.gui.screen());
                }
                closeBench(ctx);
                return StepResult.pass();
            }
        }
    }

    private static StepResult heldStep(SelfTest.StepContext ctx) {
        Map<String, Object> state = ctx.state();
        int phase = (int) state.getOrDefault(PHASE, 0);
        switch (phase) {
            case 0 -> {
                Minecraft.getInstance().gui.setScreen(null);
                SERVER_ERROR.set(null);
                state.put(SERVER, ctx.onServer(server -> {
                    ServerPlayer player = player(server);
                    player.closeContainer();
                    ItemStack drone = PropwashApi.builds().createDrone(DroneData.of(ShareScene.freestyle()));
                    player.getInventory().setItem(player.getInventory().getSelectedSlot(), drone);
                    player.containerMenu.broadcastChanges();
                }));
                state.put(PHASE, 1);
                state.put(SINCE, ctx.tick());
                return StepResult.pending();
            }
            case 1 -> {
                StepResult server = awaitServer(ctx);
                if (server.status() != StepResult.Status.PASS) {
                    return server;
                }
                if (PropwashClientApi.heldDrone().isEmpty() || ctx.tick() - (int) state.get(SINCE) < REQUEST_GAP_TICKS) {
                    return timeout(ctx, 200, "Drohne kommt nicht in der Hand an");
                }
                DroneData data = freestyle().withTune(new TuneSpec(Map.of("rates.roll.rcRate", 1.37F)));
                state.put(FUTURE, PropwashClientApi.sendImport(ImportRequest.tuneOnHeldDrone(data)));
                state.put(PHASE, 2);
                return StepResult.pending();
            }
            case 2 -> {
                ImportResult result = result(state);
                if (result == null) {
                    return timeout(ctx, 500, "keine Antwort auf den Tuning-Import");
                }
                if (result.status() != ImportStatus.APPLIED) {
                    return StepResult.fail("Tuning-Import: " + result);
                }
                state.put(SINCE, ctx.tick());
                state.put(PHASE, 3);
                return StepResult.pending();
            }
            case 3 -> {
                Float roll = PropwashClientApi.heldDrone().map(d -> d.tune().values().get("rates.roll.rcRate")).orElse(null);
                if (roll == null || Math.abs(roll - 1.37F) > 1.0E-4F || ctx.tick() - (int) state.get(SINCE) < REQUEST_GAP_TICKS) {
                    return timeout(ctx, 650, "Tuning nicht auf der gehaltenen Drohne: " + PropwashClientApi.heldDrone().map(DroneData::tune));
                }
                DroneData data = freestyle().withPaint(PaintSpec.DEFAULT.with("frame", 0x123456));
                state.put(FUTURE, PropwashClientApi.sendImport(ImportRequest.paintOnHeldDrone(data)));
                state.put(PHASE, 4);
                return StepResult.pending();
            }
            case 4 -> {
                ImportResult result = result(state);
                if (result == null) {
                    return timeout(ctx, 800, "keine Antwort auf den Lack-Import");
                }
                if (result.status() != ImportStatus.APPLIED) {
                    return StepResult.fail("Lack-Import: " + result);
                }
                state.put(PHASE, 5);
                return StepResult.pending();
            }
            default -> {
                Optional<Integer> frame = PropwashClientApi.heldDrone().flatMap(d -> d.paint().color("frame"));
                if (!frame.equals(Optional.of(0x123456))) {
                    return timeout(ctx, 880, "Lack nicht auf der gehaltenen Drohne: " + frame);
                }
                return StepResult.pass();
            }
        }
    }

    private static StepResult osdStep(SelfTest.StepContext ctx) {
        OsdLayoutSpec before = PropwashClientApi.osd().current();
        ShareContent content = ShareContent.of(DroneData.of(ShareScene.freestyle()), EnumSet.of(ShareLayer.PARTS, ShareLayer.OSD),
                Optional.of(new ShareOsd(before.version(), before.json())));
        ShareDecoded decoded = ShareCodec.decode(ShareCodec.encode(content));
        if (!decoded.ok() || decoded.require().osd().isEmpty()) {
            return StepResult.fail("OSD-Ebene fehlt nach der Rundreise: " + decoded.status());
        }
        ShareOsd osd = decoded.require().osd().get();
        if (osd.version() != before.version() || !osd.json().equals(before.json())) {
            return StepResult.fail("OSD-Layout verändert sich im Code");
        }
        boolean applied = PropwashClientApi.osd().apply(new OsdLayoutSpec(osd.version(), osd.json()));
        OsdLayoutSpec after = PropwashClientApi.osd().current();
        PropwashClientApi.osd().apply(before);
        return StepResult.check(applied && after.json().equals(before.json()), "OSD-Layout nach dem Übernehmen anders: " + applied);
    }

    private static StepResult tabStep(SelfTest.StepContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        Map<String, Object> state = ctx.state();
        int phase = (int) state.getOrDefault(PHASE, 0);
        if (phase == 0) {
            state.put(SAVED, ShareSettings.lastInput());
            mc.gui.setScreen(new TabHarnessScreen(new ShareConfigTab()));
            state.put(PHASE, 1);
            return StepResult.pending();
        }
        if (!(mc.gui.screen() instanceof TabHarnessScreen screen) || screen.frames() < 3 || !(screen.tab() instanceof ShareConfigTab tab)) {
            return timeout(ctx, 200, "Tab-Fenster erscheint nicht: " + mc.gui.screen());
        }
        if (!ThemedScreen.active()) {
            return StepResult.fail("Tab-Fenster nicht im Theme");
        }
        List<String> errors = new ArrayList<>();
        tab.setInput(ShareVectors.A);
        if (!tab.state().decoded().partsKnown()) {
            errors.add("Vektor A im Tab nicht lesbar: " + tab.state().decoded().status());
        }
        tab.setInput(ShareVectors.F);
        if (tab.state().decoded().status() != ShareStatus.CHECKSUM) {
            errors.add("Vektor F im Tab: " + tab.state().decoded().status());
        }
        if (!tab.copyHeld() || !mc.keyboardHandler.getClipboard().startsWith("PW2")) {
            errors.add("Tab kopiert die gehaltene Drohne nicht: " + tab.exportStatus());
        } else {
            tab.setInput(mc.keyboardHandler.getClipboard());
            Optional<Integer> frame = tab.state().content().flatMap(c -> c.paint()).flatMap(p -> p.color("frame"));
            if (ShareSettings.exports(ShareLayer.PAINT) && !frame.equals(Optional.of(0x123456))) {
                errors.add("Tab-Export trägt den Lack der gehaltenen Drohne nicht: " + frame);
            }
        }
        tab.setInput((String) state.get(SAVED));
        mc.gui.setScreen(null);
        return StepResult.check(errors.isEmpty(), String.join("; ", errors));
    }

    private static StepResult commandStep(SelfTest.StepContext ctx) {
        Map<String, Object> state = ctx.state();
        int phase = (int) state.getOrDefault(PHASE, 0);
        if (phase == 0) {
            SERVER_ERROR.set(null);
            COMMAND_RESULT.set(0);
            state.put(SERVER, ctx.onServer(server -> {
                ServerPlayer player = player(server);
                player.getInventory().clearContent();
                var dispatcher = server.getCommands().getDispatcher();
                try {
                    int info = dispatcher.execute(JmpCommands.ROOT + " code info " + ShareVectors.A, server.createCommandSourceStack());
                    int alias = dispatcher.execute(JmpCommands.ALIAS + " code info " + ShareVectors.B, server.createCommandSourceStack());
                    int broken = dispatcher.execute(JmpCommands.ROOT + " code info " + ShareVectors.F, server.createCommandSourceStack());
                    String code = ShareCodec.encode(ShareContent.of(freestyle(), ShareLayer.DEFAULT_EXPORT, Optional.empty()));
                    int give = dispatcher.execute(JmpCommands.ROOT + " code give " + code + " " + player.getScoreboardName(),
                            server.createCommandSourceStack());
                    boolean hasDrone = false;
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        Optional<DroneData> data = PropwashApi.builds().read(player.getInventory().getItem(i));
                        if (data.isPresent() && data.get().build().equals(freestyle().build())) {
                            hasDrone = true;
                        }
                    }
                    if (info != JmpCommands.SUCCESS || alias != JmpCommands.SUCCESS || broken != JmpCommands.FAILURE || give != 1 || !hasDrone) {
                        SERVER_ERROR.set("info " + info + ", Alias " + alias + ", F " + broken + ", give " + give + ", Drohne " + hasDrone);
                    }
                    COMMAND_RESULT.set(1);
                } catch (Exception e) {
                    SERVER_ERROR.set("Befehl warf: " + e);
                }
            }));
            state.put(PHASE, 1);
            return StepResult.pending();
        }
        StepResult server = awaitServer(ctx);
        if (server.status() != StepResult.Status.PASS) {
            return server;
        }
        return StepResult.check(COMMAND_RESULT.get() == 1, "Befehle liefen nicht");
    }

    private static StepResult cleanupStep(SelfTest.StepContext ctx) {
        if (ctx.tick() == 0) {
            Minecraft.getInstance().gui.setScreen(null);
            closeBench(ctx);
            ShareSettings.setExport(ShareLayer.DEFAULT_EXPORT);
            ShareSettings.setApply(EnumSet.of(ShareLayer.PARTS, ShareLayer.PAINT, ShareLayer.TUNE));
            return StepResult.pending();
        }
        return ctx.tick() < 5 ? StepResult.pending() : StepResult.pass();
    }

    private static ServerPlayer player(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            throw new IllegalStateException("kein Spieler auf dem Server");
        }
        return players.getFirst();
    }

    private static BlockPos benchPos(ServerPlayer player) {
        return player.blockPosition().offset(2, 0, 2);
    }

    private static void openBench(MinecraftServer server) {
        try {
            ServerPlayer player = player(server);
            player.closeContainer();
            Block block = BuiltInRegistries.BLOCK.getValue(JustMoreParts.propwash("drone_workbench"));
            BlockState bench = block.defaultBlockState();
            BlockPos pos = benchPos(player);
            player.level().setBlockAndUpdate(pos, bench);
            MenuProvider provider = player.level().getBlockState(pos).getMenuProvider(player.level(), pos);
            if (provider == null || player.openMenu(provider).isEmpty()) {
                SERVER_ERROR.set("Werkbank-Menü nicht geöffnet");
            }
        } catch (RuntimeException e) {
            SERVER_ERROR.set("Werkbank nicht aufgestellt: " + e);
        }
    }

    private static void closeBench(SelfTest.StepContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof ShareScreen) {
            mc.gui.setScreen(null);
        }
        ctx.onServer(server -> {
            ServerPlayer player = player(server);
            player.closeContainer();
            player.getInventory().clearContent();
            player.level().removeBlock(benchPos(player), false);
        });
    }

    @SuppressWarnings("unchecked")
    private static StepResult awaitServer(SelfTest.StepContext ctx) {
        CompletableFuture<Void> future = (CompletableFuture<Void>) ctx.state().get(SERVER);
        if (future == null) {
            return StepResult.pass();
        }
        if (!future.isDone()) {
            return timeout(ctx, 200, "Server antwortet nicht");
        }
        if (future.isCompletedExceptionally()) {
            return StepResult.fail("Server-Aktion fehlgeschlagen");
        }
        String error = SERVER_ERROR.get();
        if (error != null) {
            return StepResult.fail(error);
        }
        ctx.state().remove(SERVER);
        return StepResult.pass();
    }

    @SuppressWarnings("unchecked")
    private static ImportResult result(Map<String, Object> state) {
        CompletableFuture<ImportResult> future = (CompletableFuture<ImportResult>) state.get(FUTURE);
        return future != null && future.isDone() ? future.getNow(null) : null;
    }

    private static StepResult timeout(SelfTest.StepContext ctx, int limit, String reason) {
        return ctx.tick() >= limit ? StepResult.fail(reason) : StepResult.pending();
    }
    private static @Nullable String smallGui(SelfTest.StepContext ctx, Minecraft mc, ShareScreen screen) {
        int saved = mc.options.guiScale().get();
        List<ShareLayer> switched = new ArrayList<>();
        for (ShareLayer layer : ShareScreen.EXPORT_TOGGLES) {
            if (!ShareSettings.exports(layer)) {
                ShareSettings.toggleExport(layer);
                switched.add(layer);
            }
        }
        mc.options.guiScale().set(6);
        mc.resizeGui();
        try {
            String gui = mc.getWindow().getGuiScaledWidth() + "x" + mc.getWindow().getGuiScaledHeight();
            int shown = screen.codeRowsShown();
            ctx.log("Sharecode-Fenster bei GUI " + gui + ": " + screen.exportRows().size() + " Codezeilen, " + shown + " sichtbar, Werte "
                    + screen.statsHeight() + " px hoch");
            if (screen.exportRows().size() > shown) {
                int[] center = screen.codeCenter();
                screen.mouseScrolled(center[0], center[1], 0.0, -1.0);
                if (screen.codeScroll() != 1) {
                    return "Code-Kasten bei GUI " + gui + " scrollt nicht mit dem Mausrad (" + screen.exportRows().size() + " Zeilen, " + shown
                            + " sichtbar)";
                }
            }
            if (screen.statsHeight() <= Metrics.LINE_HEIGHT) {
                return "Werte der Drohne bei GUI " + gui + " ohne Platz: " + screen.statsHeight() + " px";
            }
            return null;
        } finally {
            switched.forEach(ShareSettings::toggleExport);
            mc.options.guiScale().set(saved);
            mc.resizeGui();
        }
    }
}
