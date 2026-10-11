package de.lardtom.justmoreparts.dev;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.core.JmpEvents;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.DronePreset;
import de.lardtom.propwash.api.part.PartDefinition;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class ServerSmoke {
    public static final String PROPERTY = "justmoreparts.serversmoke";
    public static final int RUN_TICKS = 100;

    private static final boolean ENABLED = Boolean.getBoolean(PROPERTY);
    private static int ticksSinceStart = -1;
    private static @Nullable String failure;
    private static boolean finished;

    private ServerSmoke() {
    }

    public static void init() {
        if (!ENABLED) {
            return;
        }
        JmpEvents.SERVER_STARTED.register(ServerSmoke::onStarted);
        JmpEvents.SERVER_TICK_END.register(ServerSmoke::onTickEnd);
    }

    private static void onStarted(MinecraftServer server) {
        ticksSinceStart = 0;
        if (!server.isDedicatedServer()) {
            fail("kein dedizierter Server");
            return;
        }
        try {
            if (!PropwashApi.parts().isFrozen()) {
                fail("Propwash-Teileregister nach dem Start nicht eingefroren");
            }
            for (PartDefinition definition : PropwashApi.parts().all()) {
                if (JustMoreParts.MOD_ID.equals(definition.id().getNamespace()) && PropwashApi.partItems().item(definition.id()).isEmpty()) {
                    fail("Teil ohne Item: " + definition.id());
                }
            }
            DronePreset freestyle = PropwashApi.builds().presets().stream()
                    .filter(p -> p.id().equals(JustMoreParts.propwash("freestyle"))).findFirst().orElse(null);
            if (freestyle == null) {
                fail("Propwash-Preset freestyle fehlt");
            } else {
                ItemStack drone = PropwashApi.builds().createDrone(freestyle.data());
                if (!PropwashApi.builds().isDrone(drone)) {
                    fail("createDrone lieferte keine Drohne");
                }
            }
            JustMoreParts.LOG.info("[JMP-Server-Rauchtest] gestartet, läuft {} Ticks", RUN_TICKS);
        } catch (RuntimeException e) {
            JustMoreParts.LOG.error("[JMP-Server-Rauchtest] Start fehlgeschlagen", e);
            fail("Ausnahme beim Start: " + e);
        }
    }

    private static void onTickEnd(MinecraftServer server) {
        if (ticksSinceStart < 0 || finished) {
            return;
        }
        ticksSinceStart++;
        if (failure == null && ticksSinceStart < RUN_TICKS) {
            return;
        }
        finished = true;
        String result = failure == null ? "PASS dedizierter Server, " + ticksSinceStart + " Ticks, " + JustMoreParts.items() + " JMP-Items"
                : "FAIL " + failure;
        JustMoreParts.LOG.info("[JMP-Server-Rauchtest] {}", result);
        DevResult.write(result);
        server.halt(false);
    }

    private static void fail(String reason) {
        if (failure == null) {
            failure = reason;
        }
    }
}
