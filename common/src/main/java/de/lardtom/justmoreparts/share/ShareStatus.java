package de.lardtom.justmoreparts.share;

import java.util.Locale;

public enum ShareStatus {
    OK,
    EMPTY,
    INVALID_PREFIX,
    BAD_CHARACTER,
    TOO_LONG,
    TRUNCATED,
    CHECKSUM,
    UNSUPPORTED_VERSION,
    TOO_LARGE,
    CORRUPT;

    public boolean ok() {
        return this == OK;
    }

    public String key() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "justmoreparts.share.status." + this.key();
    }
}
