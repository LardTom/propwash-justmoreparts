package de.lardtom.justmoreparts.client;

import de.lardtom.justmoreparts.core.DevHooks;
import de.lardtom.justmoreparts.frame.client.FrameClient;

public final class JustMorePartsClient {
    private static boolean initialized;

    private JustMorePartsClient() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        FrameClient.init();
        SchoolLinks.init();
        DevHooks.client();
    }
}
