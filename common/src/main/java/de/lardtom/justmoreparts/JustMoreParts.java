package de.lardtom.justmoreparts;

import de.lardtom.justmoreparts.core.DevHooks;
import de.lardtom.justmoreparts.core.JmpLoot;
import de.lardtom.justmoreparts.core.JmpTab;
import de.lardtom.justmoreparts.platform.Services;
import de.lardtom.justmoreparts.power.PowerModule;
import de.lardtom.justmoreparts.preset.PresetModule;
import de.lardtom.justmoreparts.share.ShareModule;
import de.lardtom.justmoreparts.video.VideoModule;
import de.lardtom.propwash.api.PropwashApi;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JustMoreParts {
    public static final String MOD_ID = "justmoreparts";
    public static final String MOD_NAME = "Propwash: Just More Parts";
    public static final String PROPWASH_ID = PropwashApi.MOD_ID;
    public static final String REQUIRED_API = "1.4";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    private static int bundledParts = -1;
    private static int items = -1;
    private static int loot = -1;

    private JustMoreParts() {
    }

    public static synchronized void init() {
        if (bundledParts >= 0) {
            return;
        }
        PropwashApi.requireVersion(MOD_ID, REQUIRED_API);
        bundledParts = PropwashApi.parts().loadBundled(MOD_ID);
        PowerModule.init();
        VideoModule.init();
        loot = JmpLoot.register();
        items = PropwashApi.partItems().registerItems(MOD_ID);
        PresetModule.init();
        JmpTab.init();
        ShareModule.init();
        DevHooks.common();
        LOG.info("{} geladen ({}): {} Teile aus JSON, {} Teil-Items, {} Truhen-Einspeisungen, Propwash-API {}", MOD_NAME,
                Services.PLATFORM.name(), bundledParts, items, loot, PropwashApi.API_VERSION);
    }

    public static int bundledParts() {
        return bundledParts;
    }

    public static int items() {
        return items;
    }

    public static int loot() {
        return loot;
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static Identifier propwash(String path) {
        return Identifier.fromNamespaceAndPath(PROPWASH_ID, path);
    }
}
