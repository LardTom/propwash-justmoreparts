package de.lardtom.justmoreparts.dev.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.dev.SelfTest;
import de.lardtom.justmoreparts.dev.SelfTest.StepResult;
import de.lardtom.justmoreparts.frame.client.FrameShapesScene;
import de.lardtom.justmoreparts.preset.JmpPresets;
import de.lardtom.justmoreparts.share.ShareCodec;
import de.lardtom.justmoreparts.share.ShareContent;
import de.lardtom.justmoreparts.share.ShareLayer;
import de.lardtom.justmoreparts.share.client.ShareConfigTab;
import de.lardtom.justmoreparts.share.client.ShareScreen;
import de.lardtom.justmoreparts.share.client.ShareWorkbenchExtension;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.DroneData;
import de.lardtom.propwash.api.build.ImportRequest;
import de.lardtom.propwash.api.build.ImportResult;
import de.lardtom.propwash.api.client.PropwashClientApi;
import de.lardtom.propwash.api.client.WorkbenchContext;
import de.lardtom.propwash.ui.client.config.ConfigTabHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class ShowcaseShots {
    private static final String FUTURE = "future";
    private static final String OPENED = "opened";
    private static final String KEY = "key";
    private static final String END = "end";
    private static final String SELECTED = "selected";
    private static final int FRAMES_OVERVIEW_TICKS = Math.round(FrameShapesScene.TAGS_AT) + 70;
    private static final int FRAMES_STRETCHED_TICKS = Math.round(FrameShapesScene.LINEUP_TICKS + FrameShapesScene.STRETCHED_REST_AT);
    private static final AtomicReference<ImportResult> IMPORTED = new AtomicReference<>();

    private ShowcaseShots() {
    }

    public static void register() {
        if (!Screenshots.enabled()) {
            return;
        }
        SelfTest.clientStep("bildschirmfotos: Fenster auf 1920×1080", 120, Screenshots::resizeWindow);
        Screenshots.step("bildschirmfotos: Presets von 75 mm bis X-Class 13″", "jmp_lineup", 40, new ScreenScene(LineupScreen::new));
        Screenshots.step("bildschirmfotos: Größenklassen im gleichen Maßstab", "jmp_scale", 40, new ScreenScene(ScaleScreen::new));
        Screenshots.step("bildschirmfotos: Werkbank mit X-Class 13″", "jmp_workbench_xclass13", 40, new WorkbenchScene(Shot.BENCH));
        Screenshots.step("bildschirmfotos: Sharecode-Fenster Export", "jmp_share_export", 40, new WorkbenchScene(Shot.EXPORT));
        Screenshots.step("bildschirmfotos: Sharecode-Fenster Import", "jmp_share_import", 40, new WorkbenchScene(Shot.IMPORT));
        Screenshots.step("bildschirmfotos: Konfigurator-Tab Sharecode", "jmp_config_tab", 40, new TabScene());
        Screenshots.step("bildschirmfotos: Rahmenformen von oben mit Namen", "jmp_frames_scene", FRAMES_OVERVIEW_TICKS, new SceneShot());
        Screenshots.step("bildschirmfotos: Rahmenformen, Schritt Stretched-X", "jmp_frames_scene_stretched", FRAMES_STRETCHED_TICKS, new SceneShot());
        CompareShots.register();
        GalleryShots.register();
        SelfTest.clientStep("bildschirmfotos: Sprache Deutsch", 1200, ctx -> Screenshots.language(ctx, "de_de"));
        Screenshots.step("bildschirmfotos: Werkbank deutsch", "jmp_workbench_xclass13_de", 40, new WorkbenchScene(Shot.BENCH));
        Screenshots.step("bildschirmfotos: Sharecode-Import deutsch", "jmp_share_import_de", 40, new WorkbenchScene(Shot.IMPORT));
        Screenshots.step("bildschirmfotos: Konfigurator-Tab deutsch", "jmp_config_tab_de", 40, new TabScene());
        SelfTest.clientStep("bildschirmfotos: Fenster auf 640×480 (320×240 Punkte)", 120, Screenshots::smallWindow);
        Screenshots.step("bildschirmfotos: Konfigurator-Tab deutsch bei 320×240", "jmp_config_tab_small_de", 40, new TabScene(true));
        SelfTest.clientStep("bildschirmfotos: Sprache zurücksetzen", 1200, Screenshots::restoreLanguage);
        Screenshots.step("bildschirmfotos: Konfigurator-Tab bei 320×240", "jmp_config_tab_small", 40, new TabScene(true));
        SelfTest.clientStep("bildschirmfotos: Fenster zurücksetzen", 20, Screenshots::restoreWindow);
    }

    private static String code(String preset) {
        DroneData data = JmpPresets.byPath(preset).data();
        return ShareCodec.encode(ShareContent.of(data, ShareLayer.DEFAULT_EXPORT, Optional.empty()));
    }

    private static final class ScreenScene implements Screenshots.Scene {
        private final Supplier<Screen> factory;

        private ScreenScene(Supplier<Screen> factory) {
            this.factory = factory;
        }

        @Override
        public StepResult prepare(SelfTest.StepContext ctx) {
            Minecraft mc = Minecraft.getInstance();
            if (!ctx.state().containsKey(OPENED)) {
                ctx.state().put(OPENED, ctx.tick());
                mc.gui.setScreen(this.factory.get());
                return StepResult.pending();
            }
            return ctx.tick() - (int) ctx.state().get(OPENED) > 5 && mc.gui.screen() != null ? StepResult.pass() : StepResult.pending();
        }

        @Override
        public void hold(SelfTest.StepContext ctx) {
            Screenshots.parkMouse(Minecraft.getInstance());
        }

        @Override
        public void finish(SelfTest.StepContext ctx) {
            Minecraft.getInstance().gui.setScreen(null);
        }
    }

    private enum Shot { BENCH, EXPORT, IMPORT }

    private static final class WorkbenchScene implements Screenshots.Scene {
        private final Shot shot;

        private WorkbenchScene(Shot shot) {
            this.shot = shot;
        }

        @Override
        public StepResult prepare(SelfTest.StepContext ctx) {
            Minecraft mc = Minecraft.getInstance();
            Map<String, Object> state = ctx.state();
            if (!state.containsKey(FUTURE)) {
                mc.gui.setScreen(null);
                IMPORTED.set(null);
                DroneData xclass = JmpPresets.byPath("xclass13_8s").data();
                state.put(FUTURE, ctx.onServer(server -> {
                    ServerPlayer player = player(server);
                    player.closeContainer();
                    ServerLevel level = player.level();
                    BlockPos pos = benchPos(player);
                    BlockState bench = BuiltInRegistries.BLOCK.getValue(JustMoreParts.propwash("drone_workbench")).defaultBlockState();
                    level.setBlockAndUpdate(pos, bench);
                    MenuProvider provider = level.getBlockState(pos).getMenuProvider(level, pos);
                    if (provider == null || player.openMenu(provider).isEmpty()) {
                        throw new IllegalStateException("Werkbank-Menü nicht geöffnet");
                    }
                    IMPORTED.set(PropwashApi.builds().applyImport(player, ImportRequest.wholeBuild(xclass)));
                }));
                return StepResult.pending();
            }
            CompletableFuture<?> future = (CompletableFuture<?>) state.get(FUTURE);
            if (!future.isDone()) {
                return StepResult.pending();
            }
            if (future.isCompletedExceptionally()) {
                return StepResult.fail("Werkbank: " + future.exceptionNow().getMessage());
            }
            ImportResult imported = IMPORTED.get();
            if (imported == null || !imported.status().success()) {
                return StepResult.fail("X-Class ließ sich nicht in die Werkbank importieren: " + imported);
            }
            WorkbenchContext context = ShareWorkbenchExtension.lastContext();
            if (this.shot == Shot.BENCH || !state.containsKey(OPENED)) {
                if (context == null || context.screen() != mc.gui.screen() || context.result().isEmpty()) {
                    return ctx.tick() > 400 ? StepResult.fail("Werkbank mit X-Class erscheint nicht: " + mc.gui.screen()) : StepResult.pending();
                }
                if (this.shot == Shot.BENCH) {
                    return StepResult.pass();
                }
                state.put(OPENED, Boolean.TRUE);
                ShareScreen.open(context, this.shot == Shot.EXPORT ? ShareScreen.Mode.EXPORT : ShareScreen.Mode.IMPORT, false);
                return StepResult.pending();
            }
            ShareScreen screen = ShareScreen.active();
            if (screen == null || mc.gui.screen() != screen || screen.frames() < 3) {
                return ctx.tick() > 600 ? StepResult.fail("Sharecode-Fenster erscheint nicht: " + mc.gui.screen()) : StepResult.pending();
            }
            if (this.shot == Shot.EXPORT) {
                return screen.exportCode().isEmpty() ? StepResult.pending() : StepResult.pass();
            }
            if (!state.containsKey("input")) {
                state.put("input", Boolean.TRUE);
                screen.setInput(code("deadcat5_6s"));
                return StepResult.pending();
            }
            return screen.state().decoded().ok() ? StepResult.pass() : StepResult.fail("Sharecode nicht lesbar: " + screen.state().decoded());
        }

        @Override
        public void hold(SelfTest.StepContext ctx) {
            Screenshots.parkMouse(Minecraft.getInstance());
        }

        @Override
        public void finish(SelfTest.StepContext ctx) {
            Minecraft.getInstance().gui.setScreen(null);
            ctx.onServer(server -> {
                ServerPlayer player = player(server);
                player.closeContainer();
                player.getInventory().clearContent();
                player.level().setBlockAndUpdate(benchPos(player), Blocks.AIR.defaultBlockState());
            });
        }
    }

    private static final class TabScene implements Screenshots.Scene {
        private final boolean small;

        private TabScene() {
            this(false);
        }

        private TabScene(boolean small) {
            this.small = small;
        }

        @Override
        public StepResult prepare(SelfTest.StepContext ctx) {
            Minecraft mc = Minecraft.getInstance();
            Map<String, Object> state = ctx.state();
            if (!state.containsKey(FUTURE)) {
                mc.gui.setScreen(null);
                DroneData held = JmpPresets.byPath("lr7_deadcat_lion").data();
                state.put(FUTURE, ctx.onServer(server -> player(server).setItemInHand(InteractionHand.MAIN_HAND,
                        PropwashApi.builds().createDrone(held))));
                return StepResult.pending();
            }
            CompletableFuture<?> future = (CompletableFuture<?>) state.get(FUTURE);
            if (!future.isDone()) {
                return StepResult.pending();
            }
            if (future.isCompletedExceptionally()) {
                return StepResult.fail("Drohne in die Hand: " + future.exceptionNow().getMessage());
            }
            if (PropwashClientApi.heldDrone().isEmpty()) {
                return ctx.tick() > 200 ? StepResult.fail("gehaltene Drohne kommt nicht an") : StepResult.pending();
            }
            if (!state.containsKey(KEY)) {
                mc.gui.setScreen(null);
                state.put(SELECTED, Optional.ofNullable(ShareConfigTab.active()));
                if (!PropwashClientApi.openConfigTab(ShareConfigTab.ID, null)) {
                    return StepResult.fail("openConfigTab findet den Sharecode-Tab nicht");
                }
                state.put(KEY, ctx.tick());
                return StepResult.pending();
            }
            if (!(mc.gui.screen() instanceof ConfigTabHost)) {
                return ctx.tick() - (int) state.get(KEY) > 100 ? StepResult.fail("Konfigurator öffnet sich nicht: " + mc.gui.screen())
                        : StepResult.pending();
            }
            ShareConfigTab tab = ShareConfigTab.active();
            if (tab == null || Optional.of(tab).equals(state.get(SELECTED)) || tab.frames() < 1) {
                return ctx.tick() > 400 ? StepResult.fail("Sharecode-Tab wird im Konfigurator nicht aufgebaut") : StepResult.pending();
            }
            if (!state.containsKey("input")) {
                state.put("input", Boolean.TRUE);
                tab.setInput(code("xclass10_6s"));
                return StepResult.pending();
            }
            if (tab.frames() <= 5 || ctx.tick() - (int) state.get(KEY) < 12) {
                return StepResult.pending();
            }
            if (this.small) {
                String problem = tab.flowProblem();
                if (problem != null) {
                    return StepResult.fail(problem);
                }
                if (tab.summaryRows() <= 3) {
                    return StepResult.fail("Zusammenfassung bricht bei 320×240 nicht um: " + tab.summaryRows() + " Zeilen");
                }
                if (!state.containsKey(END)) {
                    state.put(END, ctx.tick());
                    Screen shown = mc.gui.screen();
                    shown.setFocused(null);
                    shown.keyPressed(new KeyEvent(InputConstants.KEY_END, 0, 0));
                    ctx.log("Zusammenfassung über " + tab.summaryRows() + " Zeilen, Knöpfe darunter ab y=" + tab.actionsTop());
                    return StepResult.pending();
                }
                if (ctx.tick() - (int) state.get(END) < 4) {
                    return StepResult.pending();
                }
            }
            return StepResult.pass();
        }

        @Override
        public void hold(SelfTest.StepContext ctx) {
            Screenshots.parkMouse(Minecraft.getInstance());
        }

        @Override
        public void finish(SelfTest.StepContext ctx) {
            Minecraft mc = Minecraft.getInstance();
            Screen screen = mc.gui.screen();
            if (screen != null) {
                screen.onClose();
            }
            mc.gui.setScreen(null);
            ctx.onServer(server -> player(server).setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
        }
    }

    private static final class SceneShot implements Screenshots.Scene {
        @Override
        public StepResult prepare(SelfTest.StepContext ctx) {
            Minecraft mc = Minecraft.getInstance();
            if (!ctx.state().containsKey(OPENED)) {
                mc.gui.setScreen(null);
                ctx.state().put(OPENED, Boolean.TRUE);
                if (!PropwashClientApi.openFlightSchoolScene(FrameShapesScene.ID, null)) {
                    return StepResult.fail("Szene " + FrameShapesScene.ID + " nicht angemeldet");
                }
                return StepResult.pending();
            }
            if (mc.gui.screen() == null) {
                return ctx.tick() > 200 ? StepResult.fail("Flugschul-Fenster erscheint nicht") : StepResult.pending();
            }
            return StepResult.pass();
        }

        @Override
        public void hold(SelfTest.StepContext ctx) {
            Screenshots.parkMouse(Minecraft.getInstance());
        }

        @Override
        public void finish(SelfTest.StepContext ctx) {
            Minecraft.getInstance().gui.setScreen(null);
        }
    }

    private static ServerPlayer player(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            throw new IllegalStateException("kein Spieler auf dem Server");
        }
        return players.getFirst();
    }

    private static BlockPos benchPos(ServerPlayer player) {
        return player.blockPosition().offset(2, 0, -2);
    }
}
