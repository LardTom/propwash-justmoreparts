package de.lardtom.justmoreparts.core;

import net.minecraft.server.MinecraftServer;

import java.util.function.Consumer;

public final class JmpEvents {
    public static final Hook<Consumer<MinecraftServer>> SERVER_STARTED = new Hook<>();
    public static final Hook<Consumer<MinecraftServer>> SERVER_TICK_END = new Hook<>();
    public static final Hook<Consumer<MinecraftServer>> SERVER_STOPPED = new Hook<>();

    private JmpEvents() {
    }
}
