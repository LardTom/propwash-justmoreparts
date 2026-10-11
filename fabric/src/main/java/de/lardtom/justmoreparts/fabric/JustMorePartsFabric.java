package de.lardtom.justmoreparts.fabric;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.command.JmpCommands;
import de.lardtom.justmoreparts.core.JmpEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class JustMorePartsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        JustMoreParts.init();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> JmpEvents.SERVER_STARTED.fire(l -> l.accept(server)));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> JmpEvents.SERVER_STOPPED.fire(l -> l.accept(server)));
        ServerTickEvents.END_SERVER_TICK.register(server -> JmpEvents.SERVER_TICK_END.fire(l -> l.accept(server)));
        CommandRegistrationCallback.EVENT.register(JmpCommands::register);
    }
}
