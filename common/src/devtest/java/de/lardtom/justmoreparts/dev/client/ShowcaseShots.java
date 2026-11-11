package de.lardtom.justmoreparts.dev.client;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.dev.SelfTest;
import de.lardtom.justmoreparts.dev.SelfTest.StepResult;
import de.lardtom.justmoreparts.frame.client.FrameShapesScene;
import de.lardtom.justmoreparts.preset.JmpPresets;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.DroneData;
import de.lardtom.propwash.api.build.ImportRequest;
import de.lardtom.propwash.api.build.ImportResult;
import de.lardtom.propwash.api.client.PropwashClientApi;
import de.lardtom.propwash.api.client.WorkbenchContext;
import de.lardtom.propwash.api.client.WorkbenchExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class ShowcaseShots {
    private static final String FUTURE = "future";
    private static final String OPENED = "opened";
    private static final int FRAMES_OVERVIEW_TICKS = Math.round(FrameShapesScene.TAGS_AT) + 70;
    private static final int FRAMES_STRETCHED_TICKS = Math.round(FrameShapesScene.LINEUP_TICKS + FrameShapesScene.STRETCHED_REST_AT);
    private static final AtomicReference<ImportResult> IMPORTED = new AtomicReference<>();
    private static final AtomicReference<WorkbenchContext> CONTEXT = new AtomicReference<>();
    private static boolean probing;

    private ShowcaseShots() {
    }

    public static void register() {
        if (!Screenshots.enabled()) {
            return;
        }
        if (!probing) {
            probing = true;
            PropwashClientApi.registerWorkbenchExtension(new ContextProbe());
        }
        SelfTest.clientStep("bildschirmfotos: Fenster auf 1920×1080", 120, Screenshots::resizeWindow);
        Screenshots.step("bildschirmfotos: Presets von 75 mm bis X-Class 13″", "jmp_lineup", 40, new ScreenScene(LineupScreen::new));
        Screenshots.step("bildschirmfotos: Größenklassen im gleichen Maßstab", "jmp_scale", 40, new ScreenScene(ScaleScreen::new));
        Screenshots.step("bildschirmfotos: Werkbank mit X-Class 13″", "jmp_workbench_xclass13", 40, new WorkbenchScene());
        Screenshots.step("bildschirmfotos: Rahmenformen von oben mit Namen", "jmp_frames_scene", FRAMES_OVERVIEW_TICKS, new SceneShot());
        Screenshots.step("bildschirmfotos: Rahmenformen, Schritt Stretched-X", "jmp_frames_scene_stretched", FRAMES_STRETCHED_TICKS, new SceneShot());
        CompareShots.register();
        GalleryShots.register();
        SelfTest.clientStep("bildschirmfotos: Sprache Deutsch", 1200, ctx -> Screenshots.language(ctx, "de_de"));
        Screenshots.step("bildschirmfotos: Werkbank deutsch", "jmp_workbench_xclass13_de", 40, new WorkbenchScene());
        SelfTest.clientStep("bildschirmfotos: Sprache zurücksetzen", 1200, Screenshots::restoreLanguage);
        SelfTest.clientStep("bildschirmfotos: Fenster zurücksetzen", 20, Screenshots::restoreWindow);
    }

    private static final class ContextProbe implements WorkbenchExtension {
        @Override
        public Identifier id() {
            return JustMoreParts.id("showcase_probe");
        }

        @Override
        public void init(WorkbenchContext context) {
            CONTEXT.set(context);
        }
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

    private static final class WorkbenchScene implements Screenshots.Scene {
        @Override
        public StepResult prepare(SelfTest.StepContext ctx) {
            Minecraft mc = Minecraft.getInstance();
            Map<String, Object> state = ctx.state();
            if (!state.containsKey(FUTURE)) {
                mc.gui.setScreen(null);
                IMPORTED.set(null);
                CONTEXT.set(null);
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
            WorkbenchContext context = CONTEXT.get();
            if (context == null || context.screen() != mc.gui.screen() || context.result().isEmpty()) {
                return ctx.tick() > 400 ? StepResult.fail("Werkbank mit X-Class erscheint nicht: " + mc.gui.screen()) : StepResult.pending();
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
            ctx.onServer(server -> {
                ServerPlayer player = player(server);
                player.closeContainer();
                player.getInventory().clearContent();
                player.level().setBlockAndUpdate(benchPos(player), Blocks.AIR.defaultBlockState());
            });
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
