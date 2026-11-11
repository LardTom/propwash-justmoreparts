package de.lardtom.justmoreparts.dev;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.core.JmpEvents;
import de.lardtom.justmoreparts.platform.Services;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class WebExportRun {
    public static final String PROPERTY = "justmoreparts.webexport";
    public static final String SELECTION_PROPERTY = "justmoreparts.webexport.selection";
    public static final String COMMAND = "propwash webexport ";

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
        String selection = System.getProperty(SELECTION_PROPERTY, "all").strip();
        String result;
        try {
            Path source = Services.PLATFORM.gameDir().resolve("propwash/web-export").toAbsolutePath().normalize();
            Path target = Path.of(System.getProperty(PROPERTY)).toAbsolutePath().normalize();
            int status = server.getCommands().getDispatcher().execute(COMMAND + selection, server.createCommandSourceStack());
            if (status == 1 && Files.isRegularFile(source.resolve("manifest.json"))) {
                copy(source, target);
                result = "PASS /" + COMMAND + selection + " nach " + target;
            } else {
                result = "FAIL /" + COMMAND + selection + " meldet " + status + " (Log: Propwash-Web-Export)";
            }
        } catch (CommandSyntaxException | RuntimeException e) {
            JustMoreParts.LOG.error("[JMP-Web-Export] abgebrochen", e);
            result = "FAIL Ausnahme im Web-Export: " + e;
        }
        JustMoreParts.LOG.info("[JMP-Web-Export] {}", result);
        DevResult.write(result);
        server.halt(false);
    }

    private static void copy(Path source, Path target) {
        try {
            if (Files.exists(target)) {
                try (Stream<Path> old = Files.walk(target)) {
                    for (Path path : old.sorted(Comparator.reverseOrder()).toList()) {
                        Files.delete(path);
                    }
                }
            }
            List<Path> files;
            try (Stream<Path> walk = Files.walk(source)) {
                files = walk.sorted().toList();
            }
            for (Path path : files) {
                Path to = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(to);
                } else {
                    Files.copy(path, to);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
