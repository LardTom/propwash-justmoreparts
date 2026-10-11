package de.lardtom.justmoreparts.frame.client;

import de.lardtom.propwash.api.client.PropwashClientApi;

public final class FrameClient {
    private static boolean initialized;

    private FrameClient() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        PropwashClientApi.registerFlightSchoolScene(FrameShapesScene::create);
    }
}
