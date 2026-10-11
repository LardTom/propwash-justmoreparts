package de.lardtom.justmoreparts.neoforge.client;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.client.JustMorePartsClient;
import de.lardtom.justmoreparts.core.client.JmpClientEvents;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = JustMoreParts.MOD_ID, dist = Dist.CLIENT)
public final class JustMorePartsNeoForgeClient {
    public JustMorePartsNeoForgeClient(IEventBus modBus, ModContainer container) {
        JustMorePartsClient.init();
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> {
            Minecraft minecraft = Minecraft.getInstance();
            JmpClientEvents.TICK_END.fire(l -> l.accept(minecraft));
        });
    }
}
