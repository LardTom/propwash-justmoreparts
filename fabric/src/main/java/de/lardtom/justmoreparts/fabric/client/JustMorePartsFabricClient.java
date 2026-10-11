package de.lardtom.justmoreparts.fabric.client;

import de.lardtom.justmoreparts.client.JustMorePartsClient;
import de.lardtom.justmoreparts.core.client.JmpClientEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class JustMorePartsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        JustMorePartsClient.init();
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> JmpClientEvents.TICK_END.fire(l -> l.accept(minecraft)));
    }
}
