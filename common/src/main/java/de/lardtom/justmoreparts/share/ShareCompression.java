package de.lardtom.justmoreparts.share;

import java.util.Locale;

public enum ShareCompression {
    AUTO,
    NEVER,
    ALWAYS;

    public String key() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
