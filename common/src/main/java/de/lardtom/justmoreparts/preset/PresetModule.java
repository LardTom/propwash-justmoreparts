package de.lardtom.justmoreparts.preset;

import de.lardtom.propwash.api.PropwashApi;

public final class PresetModule {
    private static int registered;

    private PresetModule() {
    }

    public static synchronized void init() {
        if (registered > 0) {
            return;
        }
        for (JmpPresets.Entry entry : JmpPresets.ALL) {
            PropwashApi.builds().registerPreset(entry.preset());
            registered++;
        }
    }

    public static int registered() {
        return registered;
    }
}
