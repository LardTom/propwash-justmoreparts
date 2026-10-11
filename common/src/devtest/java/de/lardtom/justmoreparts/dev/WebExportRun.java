package de.lardtom.justmoreparts.dev;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.core.JmpEvents;
import de.lardtom.justmoreparts.export.WebExport;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;

public final class WebExportRun {
    public static final String PROPERTY = "justmoreparts.webexport";

    private WebExportRun() {
    }

    public static void init() {
        String target = System.getProperty(PROPERTY);
        if (target == null || target.isBlank()) {
            return;
        }
        JmpEvents.SERVER_STARTED.register(WebExportRun::onStarted);
    }

    private static void onStarted(MinecraftServer server) {
        String result;
        try {
            Path target = Path.of(System.getProperty(PROPERTY)).toAbsolutePath().normalize();
            long start = System.nanoTime();
            WebExport.Result export = WebExport.run(target);
            result = export.summary();
            JustMoreParts.LOG.info("[JMP-Web-Export] {} nach {} in {} ms", result, target, (System.nanoTime() - start) / 1_000_000L);
        } catch (RuntimeException e) {
            JustMoreParts.LOG.error("[JMP-Web-Export] abgebrochen", e);
            result = "FAIL Ausnahme im Web-Export: " + e;
        }
        DevResult.write(result);
        server.halt(false);
    }
}
