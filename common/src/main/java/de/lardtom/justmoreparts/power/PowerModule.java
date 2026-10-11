package de.lardtom.justmoreparts.power;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.propwash.api.PropwashApi;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Rarity;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PowerModule {
    public static final Identifier SEMI_SOLID = JustMoreParts.id("lihv_ss");
    private static final Map<String, Rarity> RARITIES = new LinkedHashMap<>();

    static {
        for (String path : new String[]{"b6s_5000", "b6s_8400_lion", "b8s_2200", "b8s_4500_lion", "b8s_5000", "stack_30_8s_65a",
                "stack_30_8s_100a"}) {
            RARITIES.put(path, Rarity.UNCOMMON);
        }
        RARITIES.put("b6s_3000_ss", Rarity.RARE);
        RARITIES.put("b8s_5000_ss", Rarity.RARE);
    }

    private PowerModule() {
    }

    public static void init() {
        if (PropwashApi.parts().chemistry(SEMI_SOLID).isEmpty()) {
            JustMoreParts.LOG.error("Zellchemie {} fehlt: Semi-Solid-Akkus können nicht laden", SEMI_SOLID);
        }
    }

    public static Map<String, Rarity> rarities() {
        return Map.copyOf(RARITIES);
    }
}
