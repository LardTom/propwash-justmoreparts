package de.lardtom.justmoreparts.core;

import de.lardtom.justmoreparts.JustMoreParts;

public final class DevHooks {
    public static final String COMMON = "de.lardtom.justmoreparts.dev.DevBootstrap";
    public static final String CLIENT = "de.lardtom.justmoreparts.dev.client.DevClientBootstrap";

    private DevHooks() {
    }

    public static boolean requested() {
        return Boolean.getBoolean("justmoreparts.selftest") || Boolean.getBoolean("justmoreparts.smoketest")
                || Boolean.getBoolean("justmoreparts.serversmoke") || System.getProperty("justmoreparts.webexport") != null;
    }

    public static void common() {
        run(COMMON);
    }

    public static void client() {
        run(CLIENT);
    }

    private static void run(String name) {
        Class<?> type = find(name);
        if (type == null) {
            if (requested()) {
                JustMoreParts.LOG.error("JMP-Entwicklertests angefordert, aber {} fehlt im Klassenpfad (Release-Jar enthält sie nicht)", name);
            }
            return;
        }
        try {
            type.getMethod("init").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("JMP-Entwicklertests konnten nicht starten: " + name, e);
        }
    }

    private static Class<?> find(String name) {
        try {
            return Class.forName(name, true, DevHooks.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }
}
