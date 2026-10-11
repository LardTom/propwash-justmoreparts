package de.lardtom.justmoreparts.motor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.lardtom.justmoreparts.bench.PartFiles;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.part.MotorDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.storage.loot.LootTable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MotorLootTest {
    private static final Set<String> LATE_TABLES = Set.of("vault", "vault_ominous", "ancient_city", "end_city", "stronghold");

    @Test
    public void everyTargetIsARealVanillaChest() {
        for (MotorLoot.Injection injection : MotorLoot.injections()) {
            Identifier target = injection.target().identifier();
            assertEquals("minecraft", target.getNamespace());
            String path = "data/minecraft/loot_table/" + target.getPath() + ".json";
            assertNotNull(MotorLootTest.class.getClassLoader().getResource(path), "Vanilla-Tabelle fehlt: " + path);
        }
    }

    @Test
    public void tablesOnlyContainMotorsAndCoverAllOfThem() {
        PartFiles.registerAll();
        Set<String> seen = new TreeSet<>();
        Map<String, Double> chance = new HashMap<>();
        for (ResourceKey<LootTable> key : MotorLoot.tables()) {
            String name = key.identifier().getPath().substring(MotorLoot.TABLE_PREFIX.length());
            JsonObject table = read("data/justmoreparts/loot_table/" + key.identifier().getPath() + ".json");
            assertEquals("minecraft:chest", table.get("type").getAsString());
            assertEquals(1, table.getAsJsonArray("pools").size(), name);
            JsonObject pool = table.getAsJsonArray("pools").get(0).getAsJsonObject();
            assertEquals(1, pool.get("rolls").getAsInt(), name);
            int total = 0;
            int empty = 0;
            for (JsonElement element : pool.getAsJsonArray("entries")) {
                JsonObject entry = element.getAsJsonObject();
                int weight = entry.get("weight").getAsInt();
                assertTrue(weight > 0, name);
                total += weight;
                if ("minecraft:empty".equals(entry.get("type").getAsString())) {
                    empty += weight;
                    continue;
                }
                assertEquals("minecraft:item", entry.get("type").getAsString());
                Identifier id = Identifier.parse(entry.get("name").getAsString());
                MotorDefinition motor = PropwashApi.parts().get(MotorDefinition.class, id)
                        .orElseThrow(() -> new AssertionError(name + ": kein JMP-Motor " + id));
                assertEquals(PartFiles.NAMESPACE, id.getNamespace());
                seen.add(id.getPath());
                int count = entry.has("modifier") ? entry.getAsJsonObject("modifier").get("count").getAsInt() : 1;
                if (MotorModule.rarity(motor) == Rarity.COMMON) {
                    assertEquals(2, count, name + ": " + id + " kommt paarweise");
                } else {
                    assertEquals(1, count, name + ": " + id + " kommt einzeln");
                    assertTrue(LATE_TABLES.contains(name), name + ": X-Class-Motor " + id + " nur in späten Strukturen");
                }
            }
            chance.put(name, 1.0 - (double) empty / total);
        }
        Set<String> all = new TreeSet<>();
        for (Identifier id : PartFiles.registerAll().keySet()) {
            if (PropwashApi.parts().get(MotorDefinition.class, id).isPresent()) {
                all.add(id.getPath());
            }
        }
        assertEquals(all, seen, "jeder Motor liegt in mindestens einer Truhe");
        for (Map.Entry<String, Double> entry : chance.entrySet()) {
            assertTrue(entry.getValue() > 0.05 && entry.getValue() <= 0.40, entry.getKey() + ": Motor-Chance " + entry.getValue());
        }
    }

    @Test
    public void injectionsHaveNoDuplicateTargets() {
        List<MotorLoot.Injection> list = MotorLoot.injections();
        assertEquals(list.size(), list.stream().map(MotorLoot.Injection::target).distinct().count());
        for (MotorLoot.Injection injection : list) {
            assertEquals(injection.table(), MotorLoot.tableFor(injection.target()).orElseThrow());
        }
    }

    private static JsonObject read(String path) {
        try (InputStream in = MotorLootTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, "fehlt: " + path);
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Path projectRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("settings.gradle"))) {
            dir = dir.getParent();
        }
        assertNotNull(dir, "Projektwurzel nicht gefunden");
        return dir;
    }
}
