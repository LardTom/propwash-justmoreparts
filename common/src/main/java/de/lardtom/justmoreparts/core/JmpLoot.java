package de.lardtom.justmoreparts.core;

import de.lardtom.justmoreparts.frame.FrameLoot;
import de.lardtom.justmoreparts.motor.MotorLoot;
import de.lardtom.justmoreparts.power.PowerLoot;
import de.lardtom.justmoreparts.prop.PropLoot;
import de.lardtom.propwash.api.PropwashApi;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;

public final class JmpLoot {
    private JmpLoot() {
    }

    public record Injection(ResourceKey<LootTable> target, ResourceKey<LootTable> table) {
    }

    public static List<Injection> injections() {
        List<Injection> out = new ArrayList<>();
        MotorLoot.injections().forEach(i -> out.add(new Injection(i.target(), i.table())));
        PropLoot.injections().forEach(i -> out.add(new Injection(i.target(), i.table())));
        PowerLoot.injections().forEach(i -> out.add(new Injection(i.target(), i.table())));
        FrameLoot.injections().forEach(i -> out.add(new Injection(i.target(), i.table())));
        return List.copyOf(out);
    }

    public static int register() {
        List<Injection> all = injections();
        for (Injection injection : all) {
            PropwashApi.loot().inject(injection.target(), injection.table());
        }
        return all.size();
    }

    public static List<ResourceKey<LootTable>> tablesFor(ResourceKey<LootTable> target) {
        List<ResourceKey<LootTable>> tables = new ArrayList<>(4);
        for (Injection injection : injections()) {
            if (injection.target().equals(target)) {
                tables.add(injection.table());
            }
        }
        return tables;
    }
}
