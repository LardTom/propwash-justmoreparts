package de.lardtom.justmoreparts.motor;

import de.lardtom.justmoreparts.JustMoreParts;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MotorLoot {
    public static final String TABLE_PREFIX = "inject/motor/";

    public record Injection(ResourceKey<LootTable> target, ResourceKey<LootTable> table) {
    }

    private static final List<Injection> INJECTIONS = new ArrayList<>();

    static {
        inject("village_house", "chests/village/village_plains_house", "chests/village/village_desert_house",
                "chests/village/village_savanna_house", "chests/village/village_snowy_house", "chests/village/village_taiga_house");
        inject("village_toolsmith", "chests/village/village_toolsmith");
        inject("abandoned_mineshaft", "chests/abandoned_mineshaft");
        inject("simple_dungeon", "chests/simple_dungeon");
        inject("pillager_outpost", "chests/pillager_outpost");
        inject("temple", "chests/desert_pyramid", "chests/jungle_temple");
        inject("shipwreck_supply", "chests/shipwreck_supply");
        inject("stronghold", "chests/stronghold_corridor", "chests/stronghold_crossing");
        inject("trial_chambers", "chests/trial_chambers/supply", "chests/trial_chambers/intersection");
        inject("vault", "chests/trial_chambers/reward");
        inject("vault_ominous", "chests/trial_chambers/reward_ominous");
        inject("ancient_city", "chests/ancient_city");
        inject("end_city", "chests/end_city_treasure");
    }

    private MotorLoot() {
    }

    public static List<Injection> injections() {
        return List.copyOf(INJECTIONS);
    }

    public static List<ResourceKey<LootTable>> tables() {
        return INJECTIONS.stream().map(Injection::table).distinct().toList();
    }

    public static Optional<ResourceKey<LootTable>> tableFor(ResourceKey<LootTable> target) {
        return INJECTIONS.stream().filter(i -> i.target().equals(target)).map(Injection::table).findFirst();
    }

    private static void inject(String table, String... vanillaTargets) {
        ResourceKey<LootTable> tableKey = ResourceKey.create(Registries.LOOT_TABLE, JustMoreParts.id(TABLE_PREFIX + table));
        for (String target : vanillaTargets) {
            ResourceKey<LootTable> targetKey = ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace(target));
            if (tableFor(targetKey).isPresent()) {
                throw new IllegalStateException("Doppelte Motor-Beute für " + target);
            }
            INJECTIONS.add(new Injection(targetKey, tableKey));
        }
    }
}
