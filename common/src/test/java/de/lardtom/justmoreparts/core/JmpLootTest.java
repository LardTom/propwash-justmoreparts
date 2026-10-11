package de.lardtom.justmoreparts.core;

import de.lardtom.justmoreparts.frame.FrameLoot;
import de.lardtom.justmoreparts.motor.MotorLoot;
import de.lardtom.justmoreparts.power.PowerLoot;
import de.lardtom.justmoreparts.prop.PropLoot;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JmpLootTest {
    static Path root() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("settings.gradle"))) {
            dir = dir.getParent();
        }
        assertNotNull(dir, "Projektwurzel nicht gefunden");
        return dir;
    }

    @Test
    public void everyModuleInjectionGoesThroughThePropwashLootApi() {
        List<JmpLoot.Injection> all = JmpLoot.injections();
        assertEquals(MotorLoot.injections().size() + PropLoot.injections().size() + PowerLoot.injections().size()
                + FrameLoot.injections().size(), all.size());
        Set<JmpLoot.Injection> pairs = new HashSet<>();
        for (JmpLoot.Injection injection : all) {
            assertTrue(pairs.add(injection), "doppelte Einspeisung " + injection);
            assertEquals("minecraft", injection.target().identifier().getNamespace(), injection.toString());
            assertEquals("justmoreparts", injection.table().identifier().getNamespace(), injection.toString());
            assertTrue(Files.exists(root().resolve("common/src/main/resources/data/justmoreparts/loot_table/"
                    + injection.table().identifier().getPath() + ".json")), "Beutetabelle fehlt: " + injection.table().identifier());
            assertTrue(JmpLoot.tablesFor(injection.target()).contains(injection.table()));
        }
        for (MotorLoot.Injection injection : MotorLoot.injections()) {
            assertTrue(all.contains(new JmpLoot.Injection(injection.target(), injection.table())));
        }
        for (FrameLoot.Injection injection : FrameLoot.injections()) {
            assertTrue(all.contains(new JmpLoot.Injection(injection.target(), injection.table())));
        }
    }

    @Test
    public void noLoaderSpecificLootModifiersAreLeft() {
        for (String module : List.of("common", "neoforge", "fabric")) {
            Path dir = root().resolve(module + "/src/main/resources/data/justmoreparts/loot_modifiers");
            assertFalse(Files.exists(dir), "überzählig: " + dir + " (Beute hängt Propwash über PropwashApi.loot() ein)");
        }
    }

    @Test
    public void tablesForListsEveryTableOfATarget() {
        ResourceKey<LootTable> dungeon = MotorLoot.injections().stream().map(MotorLoot.Injection::target)
                .filter(t -> t.identifier().getPath().equals("chests/simple_dungeon")).findFirst().orElseThrow();
        List<ResourceKey<LootTable>> tables = JmpLoot.tablesFor(dungeon);
        assertTrue(tables.size() >= 3, "Verlies: Motoren, Props, Akkus/Stacks und Rahmen erwartet: " + tables);
    }
}
