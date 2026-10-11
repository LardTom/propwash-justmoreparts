package de.lardtom.justmoreparts.platform;

import java.util.ServiceLoader;

public final class Services {
    public static final Platform PLATFORM = load(Platform.class);

    private Services() {
    }

    public static <T> T load(Class<T> type) {
        return ServiceLoader.load(type, Services.class.getClassLoader()).findFirst()
                .orElseThrow(() -> new IllegalStateException("Kein Dienst für " + type.getName() + " im Loader-Modul"));
    }
}
