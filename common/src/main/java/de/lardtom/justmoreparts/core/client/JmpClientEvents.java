package de.lardtom.justmoreparts.core.client;

import de.lardtom.justmoreparts.core.Hook;
import net.minecraft.client.Minecraft;

import java.util.function.Consumer;

public final class JmpClientEvents {
    public static final Hook<Consumer<Minecraft>> TICK_END = new Hook<>();

    private JmpClientEvents() {
    }
}
