package de.lardtom.justmoreparts.frame;

import de.lardtom.justmoreparts.JustMoreParts;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class FrameLoot {
    public static final String INJECT_PREFIX = "inject/frame/";

    private static final Map<ResourceKey<LootTable>, ResourceKey<LootTable>> BY_TARGET = new LinkedHashMap<>();

    static {
        inject("common", "chests/village/village_plains_house", "chests/village/village_desert_house", "chests/village/village_savanna_house",
                "chests/village/village_snowy_house", "chests/village/village_taiga_house", "chests/simple_dungeon", "chests/abandoned_mineshaft");
        inject("uncommon", "chests/desert_pyramid", "chests/jungle_temple", "chests/pillager_outpost", "chests/shipwreck_supply",
                "chests/trial_chambers/supply");
        inject("rare", "chests/stronghold_corridor", "chests/stronghold_crossing", "chests/woodland_mansion", "chests/trial_chambers/reward");
        inject("epic", "chests/ancient_city", "chests/end_city_treasure", "chests/bastion_treasure", "chests/trial_chambers/reward_ominous");
    }

    private FrameLoot() {
    }

    public record Injection(ResourceKey<LootTable> target, ResourceKey<LootTable> table) {
    }

    public static Optional<ResourceKey<LootTable>> tableFor(ResourceKey<LootTable> target) {
        return Optional.ofNullable(BY_TARGET.get(target));
    }

    public static List<Injection> injections() {
        List<Injection> list = new ArrayList<>();
        BY_TARGET.forEach((target, table) -> list.add(new Injection(target, table)));
        return Collections.unmodifiableList(list);
    }

    public static Set<ResourceKey<LootTable>> tables() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(BY_TARGET.values()));
    }

    private static void inject(String tier, String... vanillaTargets) {
        ResourceKey<LootTable> table = ResourceKey.create(Registries.LOOT_TABLE, JustMoreParts.id(INJECT_PREFIX + tier));
        for (String target : vanillaTargets) {
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace(target));
            if (BY_TARGET.put(key, table) != null) {
                throw new IllegalStateException("Doppelte Rahmen-Beute für " + target);
            }
        }
    }
}
