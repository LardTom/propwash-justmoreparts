package de.lardtom.justmoreparts.dev.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import de.lardtom.justmoreparts.dev.SelfTest;
import de.lardtom.justmoreparts.dev.SelfTest.StepResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class Screenshots {
    public static final String PROPERTY = "justmoreparts.screenshots";
    public static final int TARGET_WIDTH = 1920;
    public static final int TARGET_HEIGHT = 1080;
    public static final int GUI_SCALE = 3;
    public static final int SMALL_WIDTH = 640;
    public static final int SMALL_HEIGHT = 480;
    public static final int SMALL_GUI_SCALE = 2;
    private static final String DIR = System.getProperty(PROPERTY, "");
    private static final String READY = "shot_ready";
    private static final String SHOT = "shot_future";
    private static final int STEP_TICKS = 1200;
    private static final String RELOAD = "reload";
    private static int @Nullable [] savedWindow;
    private static @Nullable String savedLanguage;

    private Screenshots() {
    }

    public static boolean enabled() {
        return SelfTest.enabled() && !DIR.isBlank();
    }

    public static Path dir() {
        return Path.of(DIR);
    }

    public static void parkMouse(Minecraft mc) {
        mc.mouseHandler.onMove(mc.getWindow().handle(), 1.0, 1.0, 0.0, 0.0);
    }

    public static void clearOverlays(Minecraft mc) {
        mc.gui.hud.getChat().clearMessages(false);
        mc.gui.hud.setOverlayMessage(Component.empty(), false);
        mc.gui.hud.clearTitles();
        mc.gui.toastManager().clear();
    }

    public static int downscale(int width, int height) {
        for (int factor = 3; factor > 1; factor--) {
            if (width % factor == 0 && height % factor == 0 && width / factor >= TARGET_WIDTH) {
                return factor;
            }
        }
        return 1;
    }

    public static CompletableFuture<String> capture(String name) {
        CompletableFuture<String> done = new CompletableFuture<>();
        Path file = dir().resolve(name + ".png");
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException e) {
            done.completeExceptionally(e);
            return done;
        }
        RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        int factor = downscale(target.width, target.height);
        String size = (target.width / factor) + "x" + (target.height / factor);
        Screenshot.takeScreenshot(target, factor, image -> Util.ioPool().execute(() -> {
            try (image) {
                image.writeToFile(file);
                done.complete(size);
            } catch (Exception e) {
                done.completeExceptionally(e);
            }
        }));
        return done;
    }

    public static void step(String name, String file, int settleTicks, Scene scene) {
        SelfTest.clientStep(name, STEP_TICKS, ctx -> run(ctx, file, settleTicks, scene));
    }

    @SuppressWarnings("unchecked")
    private static StepResult run(SelfTest.StepContext ctx, String file, int settleTicks, Scene scene) {
        Minecraft mc = Minecraft.getInstance();
        Map<String, Object> state = ctx.state();
        if (!state.containsKey(READY)) {
            StepResult prepared;
            try {
                prepared = scene.prepare(ctx);
            } catch (RuntimeException e) {
                scene.finish(ctx);
                return StepResult.fail("Szene: " + e);
            }
            if (prepared.status() == StepResult.Status.FAIL) {
                scene.finish(ctx);
                return prepared;
            }
            if (prepared.status() == StepResult.Status.PENDING) {
                return prepared;
            }
            state.put(READY, ctx.tick());
            clearOverlays(mc);
            return StepResult.pending();
        }
        scene.hold(ctx);
        if (ctx.tick() - (int) state.get(READY) < settleTicks) {
            return StepResult.pending();
        }
        if (!state.containsKey(SHOT)) {
            state.put(SHOT, capture(file));
            return StepResult.pending();
        }
        CompletableFuture<String> shot = (CompletableFuture<String>) state.get(SHOT);
        if (!shot.isDone()) {
            return StepResult.pending();
        }
        scene.finish(ctx);
        if (shot.isCompletedExceptionally()) {
            return StepResult.fail("Bildschirmfoto " + file + ": " + shot.exceptionNow());
        }
        ctx.log("Bildschirmfoto " + file + ".png (" + shot.join() + ")");
        return StepResult.pass();
    }

    public static StepResult resizeWindow(SelfTest.StepContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();
        if (!ctx.state().containsKey("window")) {
            float density = Math.max(1.0F, window.getPixelDensity());
            ctx.state().put("window", new int[]{window.getScreenWidth(), window.getScreenHeight(), mc.options.guiScale().get()});
            window.setWindowed(Math.round(TARGET_WIDTH / density), Math.round(TARGET_HEIGHT / density));
            return StepResult.pending();
        }
        boolean sized = window.getWidth() >= TARGET_WIDTH && window.getHeight() >= TARGET_HEIGHT;
        if (!sized && ctx.tick() < 60) {
            return StepResult.pending();
        }
        mc.options.guiScale().set(GUI_SCALE);
        mc.resizeGui();
        savedWindow = (int[]) ctx.state().get("window");
        ctx.log("Fenster " + window.getWidth() + "x" + window.getHeight() + " (Dichte " + window.getPixelDensity() + "), GUI "
                + window.getGuiScaledWidth() + "x" + window.getGuiScaledHeight());
        return StepResult.pass();
    }

    public static StepResult smallWindow(SelfTest.StepContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();
        if (!ctx.state().containsKey("small")) {
            float density = Math.max(1.0F, window.getPixelDensity());
            ctx.state().put("small", Boolean.TRUE);
            window.setWindowed(Math.round(SMALL_WIDTH / density), Math.round(SMALL_HEIGHT / density));
            return StepResult.pending();
        }
        boolean sized = window.getWidth() == SMALL_WIDTH && window.getHeight() == SMALL_HEIGHT;
        if (!sized && ctx.tick() < 80) {
            return StepResult.pending();
        }
        if (!sized) {
            return StepResult.fail("Fenster wird nicht " + SMALL_WIDTH + "x" + SMALL_HEIGHT + ": " + window.getWidth() + "x" + window.getHeight());
        }
        mc.options.guiScale().set(SMALL_GUI_SCALE);
        mc.resizeGui();
        ctx.log("Fenster " + window.getWidth() + "x" + window.getHeight() + ", GUI " + window.getGuiScaledWidth() + "x" + window.getGuiScaledHeight());
        return window.getGuiScaledWidth() == SMALL_WIDTH / SMALL_GUI_SCALE ? StepResult.pass()
                : StepResult.fail("GUI ist " + window.getGuiScaledWidth() + " Punkte breit statt " + SMALL_WIDTH / SMALL_GUI_SCALE);
    }

    public static StepResult restoreWindow(SelfTest.StepContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        int[] saved = savedWindow;
        if (saved != null) {
            mc.options.guiScale().set(saved[2]);
            mc.getWindow().setWindowed(saved[0], saved[1]);
            mc.resizeGui();
            savedWindow = null;
        }
        return StepResult.pass();
    }

    public static StepResult language(SelfTest.StepContext ctx, String code) {
        Minecraft mc = Minecraft.getInstance();
        Map<String, Object> state = ctx.state();
        if (!state.containsKey(RELOAD)) {
            String current = mc.getLanguageManager().getSelected();
            if (savedLanguage == null) {
                savedLanguage = current;
            }
            if (code.equals(current)) {
                return StepResult.pass();
            }
            mc.gui.setScreen(null);
            mc.getLanguageManager().setSelected(code);
            mc.options.languageCode = code;
            state.put(RELOAD, mc.reloadResourcePacks());
            return StepResult.pending();
        }
        CompletableFuture<?> reload = (CompletableFuture<?>) state.get(RELOAD);
        if (!reload.isDone() || mc.gui.overlay() != null) {
            return StepResult.pending();
        }
        if (reload.isCompletedExceptionally()) {
            return StepResult.fail("Sprache " + code + ": " + reload.exceptionNow());
        }
        ctx.log("Sprache " + code + " aktiv");
        return StepResult.pass();
    }

    public static StepResult restoreLanguage(SelfTest.StepContext ctx) {
        String saved = savedLanguage;
        if (saved == null) {
            return StepResult.pass();
        }
        StepResult result = language(ctx, saved);
        if (result.status() != StepResult.Status.PENDING) {
            savedLanguage = null;
        }
        return result;
    }

    public interface Scene {
        StepResult prepare(SelfTest.StepContext ctx);

        default void hold(SelfTest.StepContext ctx) {
        }

        default void finish(SelfTest.StepContext ctx) {
        }
    }
}
