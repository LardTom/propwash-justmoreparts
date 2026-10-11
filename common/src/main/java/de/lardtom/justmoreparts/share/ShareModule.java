package de.lardtom.justmoreparts.share;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.propwash.api.build.ImportRequest;
import de.lardtom.propwash.api.build.ImportResult;
import de.lardtom.propwash.api.build.ImportStatus;
import de.lardtom.propwash.api.event.PropwashEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class ShareModule {
    private static final Map<ImportStatus, AtomicInteger> IMPORTS = new ConcurrentHashMap<>();
    private static boolean initialized;

    private ShareModule() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        PropwashEvents.BUILD_IMPORTED.register(ShareModule::onImported);
    }

    private static void onImported(ServerPlayer player, ImportRequest request, ImportResult result) {
        IMPORTS.computeIfAbsent(result.status(), status -> new AtomicInteger()).incrementAndGet();
        JustMoreParts.LOG.debug("Import von {}: Ziel {}, Ebenen {}, Ergebnis {}", player.getScoreboardName(), request.target(), request.layers(),
                result.status());
    }

    public static Map<ImportStatus, Integer> importCounts() {
        EnumMap<ImportStatus, Integer> out = new EnumMap<>(ImportStatus.class);
        IMPORTS.forEach((status, count) -> out.put(status, count.get()));
        return out;
    }
}
