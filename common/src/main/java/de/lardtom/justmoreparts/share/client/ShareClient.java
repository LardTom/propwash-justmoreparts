package de.lardtom.justmoreparts.share.client;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.propwash.api.client.PropwashClientApi;

public final class ShareClient {
    private static boolean initialized;

    private ShareClient() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        PropwashClientApi.registerWorkbenchExtension(new ShareWorkbenchExtension());
        PropwashClientApi.registerConfigTab(ShareConfigTab::new);
        try {
            PropwashClientApi.registerFlightSchoolScene(ShareScene::create);
        } catch (IllegalArgumentException e) {
            JustMoreParts.LOG.error("Flugschul-Szene „Builds teilen“ nicht angemeldet", e);
        }
    }
}
