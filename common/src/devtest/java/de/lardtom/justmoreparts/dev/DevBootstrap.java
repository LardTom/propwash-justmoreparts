package de.lardtom.justmoreparts.dev;

public final class DevBootstrap {
    private DevBootstrap() {
    }

    public static void init() {
        ServerSmoke.init();
        WebExportRun.init();
    }
}
