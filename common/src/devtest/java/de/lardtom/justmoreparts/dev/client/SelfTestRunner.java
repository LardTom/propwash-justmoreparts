package de.lardtom.justmoreparts.dev.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.core.client.JmpClientEvents;
import de.lardtom.justmoreparts.dev.DevResult;
import de.lardtom.justmoreparts.dev.SelfTest;
import de.lardtom.justmoreparts.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class SelfTestRunner {
    public static final String WORLD_NAME = "JMP Selftest";
    private static final int GLOBAL_TIMEOUT_TICKS = 20 * 480;
    private static final long WATCHDOG_MILLIS = 12L * 60L * 1000L;
    private static final long SMOKE_WATCHDOG_MILLIS = 5L * 60L * 1000L;
    private static final int SETTLE_TICKS = 40;
    private static final int TITLE_TIMEOUT_TICKS = 20 * 60;
    private static final int LEAVE_TIMEOUT_TICKS = 20 * 60;

    private enum Phase { WAIT_TITLE, WAIT_WORLD, SETTLE, RUN, WAIT_LEFT, FINISH, DONE }

    private static Phase phase = Phase.WAIT_TITLE;
    private static int totalTicks;
    private static int phaseTicks;
    private static List<SelfTest.Step> steps = List.of();
    private static int stepIndex;
    private static int stepTicks;
    private static Map<String, Object> stepState = SelfTest.newState();
    private static final List<String> FAILURES = new ArrayList<>();
    private static int passed;
    private static volatile boolean finished;
    private static boolean smoke;
    private static boolean installed;

    private SelfTestRunner() {
    }

    public static void install() {
        if (SelfTest.enabled()) {
            start(false);
        }
    }

    public static void installSmoke() {
        if (SelfTest.smokeEnabled()) {
            start(true);
        }
    }

    private static void start(boolean smokeMode) {
        if (installed) {
            return;
        }
        installed = true;
        smoke = smokeMode;
        JustMoreParts.LOG.info("[JMP-Selbsttest] {} aktiv ({})", smoke ? "Rauchtest" : "Selbsttest", Services.PLATFORM.name());
        JmpClientEvents.TICK_END.register(SelfTestRunner::tick);
        long timeout = smoke ? SMOKE_WATCHDOG_MILLIS : WATCHDOG_MILLIS;
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                return;
            }
            if (!finished) {
                JustMoreParts.LOG.error("{} FAIL Watchdog: Client hängt (Phase {})", marker(), phase);
                DevResult.write("FAIL Watchdog: Client hängt in Phase " + phase);
                Runtime.getRuntime().halt(3);
            }
        }, "JMP-Selftest-Watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    private static String marker() {
        return smoke ? "JMP SMOKE" : "JMP SELFTEST";
    }

    private static void tick(Minecraft mc) {
        totalTicks++;
        phaseTicks++;
        try {
            if (totalTicks > GLOBAL_TIMEOUT_TICKS && phase.ordinal() < Phase.WAIT_LEFT.ordinal()) {
                FAILURES.add("Zeitüberschreitung in Phase " + phase + " (Screen: " + screenName(mc) + ")");
                leaveWorld(mc);
            }
            switch (phase) {
                case WAIT_TITLE -> {
                    if (mc.gui.screen() instanceof TitleScreen && phaseTicks > 10) {
                        createWorld(mc);
                        setPhase(Phase.WAIT_WORLD);
                    } else if (phaseTicks > TITLE_TIMEOUT_TICKS) {
                        FAILURES.add("Titelbildschirm nicht erreicht, aktueller Screen: " + screenName(mc));
                        leaveWorld(mc);
                    }
                }
                case WAIT_WORLD -> {
                    Screen screen = mc.gui.screen();
                    if (screen instanceof ConfirmScreen confirm && phaseTicks % 20 == 0) {
                        JustMoreParts.LOG.info("[JMP-Selbsttest] bestätige {}", confirm.getTitle().getString());
                        pressButton(confirm, CommonComponents.GUI_YES.getString());
                    }
                    if (mc.player != null && mc.level != null && screen == null) {
                        JustMoreParts.LOG.info("[JMP-Selbsttest] Welt geladen nach {} Ticks", phaseTicks);
                        setPhase(Phase.SETTLE);
                    }
                }
                case SETTLE -> {
                    if (phaseTicks >= SETTLE_TICKS && smoke) {
                        JustMoreParts.LOG.info("[JMP-Selbsttest] Rauchtest: Welt betreten, keine Schritte");
                        leaveWorld(mc);
                    } else if (phaseTicks >= SETTLE_TICKS) {
                        steps = SelfTest.startClientSteps();
                        JustMoreParts.LOG.info("[JMP-Selbsttest] starte {} Schritte", steps.size());
                        startStep(0);
                        setPhase(Phase.RUN);
                    }
                }
                case RUN -> {
                    if (runStep(mc)) {
                        leaveWorld(mc);
                    }
                }
                case WAIT_LEFT -> {
                    if (mc.level == null && mc.player == null && mc.getSingleplayerServer() == null && phaseTicks > 5) {
                        JustMoreParts.LOG.info("[JMP-Selbsttest] Welt verlassen nach {} Ticks", phaseTicks);
                        setPhase(Phase.FINISH);
                    } else if (phaseTicks > LEAVE_TIMEOUT_TICKS) {
                        FAILURES.add("Welt nicht verlassen (Screen: " + screenName(mc) + ")");
                        setPhase(Phase.FINISH);
                    }
                }
                case FINISH -> finish(mc);
                case DONE -> {
                }
            }
        } catch (Throwable t) {
            JustMoreParts.LOG.error("[JMP-Selbsttest] Fehler im Runner (Phase {})", phase, t);
            if (phase.ordinal() < Phase.WAIT_LEFT.ordinal()) {
                FAILURES.add("Runner-Fehler in Phase " + phase + ": " + t);
                leaveWorld(mc);
            } else if (phase.ordinal() < Phase.FINISH.ordinal()) {
                FAILURES.add("Runner-Fehler in Phase " + phase + ": " + t);
                setPhase(Phase.FINISH);
            } else if (phase != Phase.DONE) {
                setPhase(Phase.DONE);
                mc.stop();
            }
        }
    }

    private static void leaveWorld(Minecraft mc) {
        setPhase(Phase.WAIT_LEFT);
        if (mc.level != null) {
            JustMoreParts.LOG.info("[JMP-Selbsttest] verlasse die Welt");
            mc.schedule(() -> mc.disconnectFromWorld(Component.literal("JMP-Selbsttest beendet")));
        }
    }

    private static void createWorld(Minecraft mc) {
        deleteOldWorlds(mc);
        CreateWorldScreen.openFresh(mc, () -> mc.gui.setScreen(new TitleScreen()));
        if (!(mc.gui.screen() instanceof CreateWorldScreen screen)) {
            throw new IllegalStateException("CreateWorldScreen wurde nicht geöffnet, sondern " + screenName(mc));
        }
        WorldCreationUiState ui = screen.getUiState();
        ui.setName(WORLD_NAME);
        Holder<WorldPreset> flat = ui.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT);
        ui.setWorldType(new WorldCreationUiState.WorldTypeEntry(flat));
        ui.setSeed("1");
        ui.setGenerateStructures(false);
        ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
        ui.getGameRules().set(GameRules.ADVANCE_TIME, false, null);
        ui.getGameRules().set(GameRules.ADVANCE_WEATHER, false, null);
        ui.getGameRules().set(GameRules.SPAWN_MOBS, false, null);
        JustMoreParts.LOG.info("[JMP-Selbsttest] lege Welt '{}' an", ui.getTargetFolder());
        if (!pressButton(screen, Component.translatable("selectWorld.create").getString())) {
            throw new IllegalStateException("Knopf 'selectWorld.create' nicht gefunden");
        }
    }

    private static void deleteOldWorlds(Minecraft mc) {
        Path saves = mc.getLevelSource().getBaseDir();
        if (!Files.isDirectory(saves)) {
            return;
        }
        try (Stream<Path> worlds = Files.list(saves)) {
            for (Path world : worlds.filter(p -> p.getFileName().toString().startsWith(WORLD_NAME)).toList()) {
                try (Stream<Path> files = Files.walk(world)) {
                    for (Path p : files.sorted(Comparator.reverseOrder()).toList()) {
                        Files.deleteIfExists(p);
                    }
                }
            }
        } catch (IOException e) {
            JustMoreParts.LOG.warn("[JMP-Selbsttest] alte Welten nicht gelöscht: {}", e.toString());
        }
    }

    private static boolean pressButton(Screen screen, String text) {
        for (GuiEventListener child : screen.children()) {
            if (child instanceof Button button && text.equals(button.getMessage().getString())) {
                button.onPress(new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
                return true;
            }
        }
        return false;
    }

    private static void startStep(int index) {
        stepIndex = index;
        stepTicks = 0;
        stepState = SelfTest.newState();
        if (index < steps.size()) {
            JustMoreParts.LOG.info("[JMP-Selbsttest] Schritt {}/{}: {}", index + 1, steps.size(), steps.get(index).name());
        }
    }

    private static boolean runStep(Minecraft mc) {
        if (stepIndex >= steps.size()) {
            return true;
        }
        SelfTest.Step step = steps.get(stepIndex);
        SelfTest.StepResult result;
        try {
            result = step.body().tick(new Context(stepTicks, mc.getSingleplayerServer(), stepState, step.name()));
        } catch (Throwable t) {
            JustMoreParts.LOG.error("[JMP-Selbsttest] Schritt '{}' warf", step.name(), t);
            result = SelfTest.StepResult.fail("Ausnahme: " + t);
        }
        stepTicks++;
        switch (result.status()) {
            case PASS -> {
                passed++;
                JustMoreParts.LOG.info("[JMP-Selbsttest] PASS {} ({} Ticks)", step.name(), stepTicks);
                startStep(stepIndex + 1);
            }
            case FAIL -> {
                FAILURES.add(step.name() + ": " + result.message());
                JustMoreParts.LOG.error("[JMP-Selbsttest] FAIL {}: {}", step.name(), result.message());
                startStep(stepIndex + 1);
            }
            case PENDING -> {
                if (stepTicks >= step.maxTicks()) {
                    FAILURES.add(step.name() + ": Zeitüberschreitung nach " + step.maxTicks() + " Ticks");
                    JustMoreParts.LOG.error("[JMP-Selbsttest] FAIL {}: Zeitüberschreitung", step.name());
                    startStep(stepIndex + 1);
                }
            }
        }
        return stepIndex >= steps.size();
    }

    private static void finish(Minecraft mc) {
        finished = true;
        String result;
        if (smoke && FAILURES.isEmpty()) {
            result = "PASS";
            JustMoreParts.LOG.info("JMP SMOKE PASS (Welt angelegt, betreten und verlassen, {})", Services.PLATFORM.name());
        } else if (!smoke && FAILURES.isEmpty() && passed > 0) {
            result = "PASS";
            JustMoreParts.LOG.info("JMP SELFTEST PASS ({} Schritte, {})", passed, Services.PLATFORM.name());
        } else {
            if (!smoke && passed == 0 && FAILURES.isEmpty()) {
                FAILURES.add("keine Schritte ausgeführt");
            }
            result = "FAIL " + String.join(" | ", FAILURES);
            JustMoreParts.LOG.error("{} {} ({})", marker(), result, Services.PLATFORM.name());
        }
        DevResult.write(result);
        setPhase(Phase.DONE);
        JustMoreParts.LOG.info("[JMP-Selbsttest] beende das Spiel");
        mc.stop();
    }

    private static void setPhase(Phase next) {
        phase = next;
        phaseTicks = 0;
    }

    private static String screenName(Minecraft mc) {
        Screen s = mc.gui.screen();
        return s == null ? "keiner" : s.getClass().getName();
    }

    private record Context(int tick, @Nullable MinecraftServer server, Map<String, Object> state, String name) implements SelfTest.StepContext {
        @Override
        public CompletableFuture<Void> onServer(Consumer<MinecraftServer> action) {
            MinecraftServer s = this.server;
            if (s == null) {
                return CompletableFuture.failedFuture(new IllegalStateException("kein integrierter Server"));
            }
            return s.submit(() -> action.accept(s)).whenComplete((v, t) -> {
                if (t != null) {
                    JustMoreParts.LOG.error("[JMP-Selbsttest] Server-Aktion in '{}' fehlgeschlagen", this.name, t);
                }
            });
        }

        @Override
        public void log(String message) {
            JustMoreParts.LOG.info("[JMP-Selbsttest] {}: {}", this.name, message);
        }
    }
}
