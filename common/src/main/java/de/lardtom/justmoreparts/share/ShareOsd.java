package de.lardtom.justmoreparts.share;

import java.util.Objects;

public record ShareOsd(int version, String json) {
    public ShareOsd {
        Objects.requireNonNull(json, "json");
        if (version < 1) {
            throw new ShareException("justmoreparts.share.error.osd_version", version);
        }
        if (json.length() > ShareTables.MAX_OSD_CHARS) {
            throw new ShareException("justmoreparts.share.error.osd_length", json.length(), ShareTables.MAX_OSD_CHARS);
        }
    }
}
