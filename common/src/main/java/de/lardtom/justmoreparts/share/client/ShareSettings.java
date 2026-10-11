package de.lardtom.justmoreparts.share.client;

import de.lardtom.justmoreparts.share.ShareLayer;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class ShareSettings {
    private static final EnumSet<ShareLayer> EXPORT = EnumSet.copyOf(ShareLayer.DEFAULT_EXPORT);
    private static final EnumSet<ShareLayer> APPLY = EnumSet.of(ShareLayer.PARTS, ShareLayer.PAINT, ShareLayer.TUNE);
    private static String lastInput = "";

    private ShareSettings() {
    }

    public static synchronized Set<ShareLayer> exportLayers() {
        return Collections.unmodifiableSet(EnumSet.copyOf(EXPORT));
    }

    public static synchronized boolean exports(ShareLayer layer) {
        return EXPORT.contains(layer);
    }

    public static synchronized void toggleExport(ShareLayer layer) {
        if (layer == ShareLayer.PARTS) {
            return;
        }
        if (!EXPORT.remove(layer)) {
            EXPORT.add(layer);
        }
    }

    public static synchronized void setExport(Set<ShareLayer> layers) {
        EXPORT.clear();
        EXPORT.addAll(layers);
        EXPORT.add(ShareLayer.PARTS);
    }

    public static synchronized Set<ShareLayer> applyLayers() {
        return Collections.unmodifiableSet(EnumSet.copyOf(APPLY));
    }

    public static synchronized boolean applies(ShareLayer layer) {
        return APPLY.contains(layer);
    }

    public static synchronized void toggleApply(ShareLayer layer) {
        if (layer == ShareLayer.NAME) {
            return;
        }
        if (!APPLY.remove(layer)) {
            APPLY.add(layer);
        }
    }

    public static synchronized void setApply(Set<ShareLayer> layers) {
        APPLY.clear();
        APPLY.addAll(layers);
        APPLY.remove(ShareLayer.NAME);
    }

    public static synchronized String lastInput() {
        return lastInput;
    }

    public static synchronized void lastInput(String text) {
        lastInput = text == null ? "" : text;
    }
}
