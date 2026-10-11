package de.lardtom.justmoreparts.power;

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

public final class PowerLoot {
    public static final String TABLE_PREFIX = "inject/power/";

    private static final Map<ResourceKey<LootTable>, ResourceKey<LootTable>> BY_TARGET = new LinkedHashMap<>();

    static {
        inject("village_house", "chests/village/village_plains_house", "chests/village/village_desert_house",
                "chests/village/village_savanna_house", "chests/village/village_snowy_house", "chests/village/village_taiga_house");
        inject("village_toolsmith", "chests/village/village_toolsmith");
        inject("abandoned_mineshaft", "chests/abandoned_mineshaft");
        inject("simple_dungeon", "chests/simple_dungeon");
        inject("temple", "chests/desert_pyramid", "chests/jungle_temple");
        inject("pillager_outpost", "chests/pillager_outpost");
        inject("stronghold", "chests/stronghold_corridor", "chests/stronghold_crossing");
        inject("shipwreck_supply", "chests/shipwreck_supply");
        inject("trial_chambers", "chests/trial_chambers/supply", "chests/trial_chambers/intersection");
        inject("vault", "chests/trial_chambers/reward");
        inject("vault_ominous", "chests/trial_chambers/reward_ominous");
        inject("ancient_city", "chests/ancient_city");
        inject("end_city", "chests/end_city_treasure");
    }

    private PowerLoot() {
    }

    public record Injection(ResourceKey<LootTable> target, ResourceKey<LootTable> table) {
    }

    public static Optional<ResourceKey<LootTable>> tableFor(ResourceKey<LootTable> target) {
        return Optional.ofNullable(BY_TARGET.get(target));
    }

    public static List<Injection> injections() {
        List<Injection> out = new ArrayList<>();
        BY_TARGET.forEach((target, table) -> out.add(new Injection(target, table)));
        return Collections.unmodifiableList(out);
    }

    public static Set<ResourceKey<LootTable>> tables() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(BY_TARGET.values()));
    }

    private static void inject(String table, String... vanillaTargets) {
        ResourceKey<LootTable> tableKey = ResourceKey.create(Registries.LOOT_TABLE, JustMoreParts.id(TABLE_PREFIX + table));
        for (String target : vanillaTargets) {
            ResourceKey<LootTable> targetKey = ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace(target));
            if (BY_TARGET.put(targetKey, tableKey) != null) {
                throw new IllegalStateException("Doppelte Akku-/Stack-Beute für " + target);
            }
        }
    }
}
