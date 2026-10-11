package de.lardtom.justmoreparts.share;

import net.minecraft.network.chat.Component;

import java.util.Arrays;

public final class ShareException extends IllegalArgumentException {
    public static final String REJECTED = "justmoreparts.share.error.rejected";

    private final String key;
    private final Object[] args;

    ShareException(String key, Object... args) {
        super(key + " " + Arrays.toString(args), null);
        this.key = key;
        this.args = args.clone();
    }

    public String key() {
        return this.key;
    }

    public Object[] args() {
        return this.args.clone();
    }

    public Component reason() {
        return Component.translatable(this.key, this.args);
    }

    public static Component reason(RuntimeException e) {
        return e instanceof ShareException share ? share.reason() : Component.translatable(REJECTED);
    }
}
