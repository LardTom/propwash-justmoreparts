package de.lardtom.justmoreparts.neoforge;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.command.JmpCommands;
import de.lardtom.justmoreparts.core.JmpEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(JustMoreParts.MOD_ID)
public final class JustMorePartsNeoForge {
    public JustMorePartsNeoForge(IEventBus modBus, ModContainer container) {
        JustMoreParts.init();
        IEventBus bus = NeoForge.EVENT_BUS;
        bus.addListener(ServerStartedEvent.class, e -> JmpEvents.SERVER_STARTED.fire(l -> l.accept(e.getServer())));
        bus.addListener(ServerStoppedEvent.class, e -> JmpEvents.SERVER_STOPPED.fire(l -> l.accept(e.getServer())));
        bus.addListener(ServerTickEvent.Post.class, e -> JmpEvents.SERVER_TICK_END.fire(l -> l.accept(e.getServer())));
        bus.addListener(RegisterCommandsEvent.class, e -> JmpCommands.register(e.getDispatcher(), e.getBuildContext(), e.getCommandSelection()));
    }
}
