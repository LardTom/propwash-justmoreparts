package de.lardtom.justmoreparts.motor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.lardtom.justmoreparts.bench.PartFiles;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.part.MotorDefinition;
import de.lardtom.propwash.api.part.MotorThermal;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Rarity;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MotorDataTest {
    private static final Pattern ID = Pattern.compile("^m(\\d{4,5})_(\\d{3,5})$");
    private static final Set<String> MARKERS = Set.of("minecraft:amethyst_shard", "minecraft:redstone", "minecraft:glowstone_dust");

    static List<MotorDefinition> motors() {
        List<MotorDefinition> out = new ArrayList<>();
        for (Identifier id : PartFiles.registerAll().keySet()) {
            PropwashApi.parts().get(MotorDefinition.class, id).ifPresent(out::add);
        }
        return out;
    }

    @Test
    public void idsMatchStatorAndKv() {
        for (MotorDefinition motor : motors()) {
            Matcher m = ID.matcher(motor.id().getPath());
            assertTrue(m.matches(), motor.id().toString());
            assertEquals(motor.statorLabel().replace(".", ""), m.group(1), motor.id().toString());
            assertEquals(Integer.parseInt(m.group(2)), motor.kv(), motor.id().toString());
        }
    }

    @Test
    public void kvSqrtRIsSharedWithinAStatorAndFollowsSize() {
        Map<String, List<Double>> byStator = new TreeMap<>();
        for (MotorDefinition motor : motors()) {
            byStator.computeIfAbsent(motor.statorLabel(), k -> new ArrayList<>()).add(motor.kv() * Math.sqrt(motor.resistanceOhm()));
        }
        for (Map.Entry<String, List<Double>> entry : byStator.entrySet()) {
            double min = entry.getValue().stream().mapToDouble(Double::doubleValue).min().orElseThrow();
            double max = entry.getValue().stream().mapToDouble(Double::doubleValue).max().orElseThrow();
            assertTrue(max / min < 1.04, entry.getKey() + ": KV·√R streut " + entry.getValue());
        }
        double previous = Double.MAX_VALUE;
        for (String stator : List.of("0702", "0802", "1102", "1103", "1404", "1505", "2004", "2207", "2507", "2806.5", "2807", "3115", "4214")) {
            double value = byStator.get(stator).getFirst();
            assertTrue(value < previous, stator + ": KV·√R " + value + " nicht kleiner als beim kleineren Stator");
            previous = value;
        }
    }

    @Test
    public void thermalLimitsStayPlausible() {
        for (MotorDefinition motor : motors()) {
            MotorThermal t = motor.thermal();
            assertTrue(t.derateStartC() >= 115.0 && t.derateStartC() <= 145.0, motor.id().toString());
            assertTrue(t.maxTempC() - t.derateStartC() >= 20.0, motor.id().toString());
            double scale = Math.pow(motor.basics().massGrams() / 33.0, 2.0 / 3.0);
            if (t.coolingWPerKPerRadS().isPresent()) {
                assertTrue(t.coolingWPerKPerRadS().getAsDouble() <= 1.5e-4 * scale * 2.5 + 1e-9, motor.id() + ": Fahrtwind-Kühlung über 2,5 × Standard");
            }
            if (t.coolingWPerK().isPresent()) {
                assertTrue(t.coolingWPerK().getAsDouble() <= 0.075 * scale * 2.0 + 1e-9, motor.id() + ": Standkühlung über 2 × Standard");
            }
        }
    }

    @Test
    public void rarityFollowsStatorSize() {
        int uncommon = 0;
        int rare = 0;
        for (MotorDefinition motor : motors()) {
            Rarity rarity = MotorModule.rarity(motor);
            if (motor.statorLabel().startsWith("31")) {
                assertEquals(Rarity.UNCOMMON, rarity, motor.id().toString());
                uncommon++;
            } else if (motor.statorLabel().startsWith("42")) {
                assertEquals(Rarity.RARE, rarity, motor.id().toString());
                rare++;
            } else {
                assertEquals(Rarity.COMMON, rarity, motor.id().toString());
            }
        }
        assertEquals(3, uncommon);
        assertEquals(3, rare);
    }

    @Test
    public void mountsMatchStatorClass() {
        for (MotorDefinition motor : motors()) {
            double d = motor.statorDiameterMm();
            String expected = d <= 11 && motor.statorHeightMm() <= 2 ? "propwash:m08" : d <= 20 ? "propwash:m14" : d < 25 ? "propwash:m22"
                    : d < 30 ? "propwash:m28" : d < 40 ? "justmoreparts:m31" : "justmoreparts:m42";
            assertEquals(expected, motor.mount().toString(), motor.id().toString());
        }
    }

    @Test
    public void recipesAreTieredMirrorSafeAndUnique() throws IOException {
        Path dir = MotorLootTest.projectRoot().resolve("common/src/main/resources/data/justmoreparts/recipe");
        Map<String, String> signatures = new HashMap<>();
        Map<String, Map<Integer, String>> markersByStator = new HashMap<>();
        for (MotorDefinition motor : motors()) {
            String path = motor.id().getPath();
            JsonObject recipe = JsonParser.parseString(Files.readString(dir.resolve(path + ".json"), StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString(), path);
            assertEquals("equipment", recipe.get("category").getAsString(), path);
            assertEquals("propwash_motor", recipe.get("group").getAsString(), path);
            assertEquals(motor.id().toString(), recipe.getAsJsonObject("result").get("id").getAsString(), path);
            int count = recipe.getAsJsonObject("result").get("count").getAsInt();
            assertEquals(motor.statorDiameterMm() >= 30 ? 1 : 2, count, path + ": Ausbeute");
            List<String> rows = new ArrayList<>();
            recipe.getAsJsonArray("pattern").forEach(e -> rows.add(e.getAsString()));
            for (String row : rows) {
                assertEquals(new StringBuilder(row).reverse().toString(), row, path + ": Muster nicht spiegelgleich");
            }
            JsonObject key = recipe.getAsJsonObject("key");
            assertTrue(key.has("L") && "propwash:copper_coil".equals(key.get("L").getAsString()), path + ": Kupferspule fehlt");
            String marker = key.get("K").getAsString();
            assertTrue(MARKERS.contains(marker), path + ": KV-Kennzeichen " + marker);
            markersByStator.computeIfAbsent(motor.statorLabel(), k -> new TreeMap<>()).put(motor.kv(), marker);
            StringBuilder signature = new StringBuilder();
            for (String row : rows) {
                for (char c : row.toCharArray()) {
                    signature.append(c == ' ' ? "_" : key.get(String.valueOf(c)).getAsString()).append(',');
                }
                signature.append('|');
            }
            String previous = signatures.put(signature.toString(), path);
            assertTrue(previous == null, path + " hat dasselbe Rezept wie " + previous);
        }
        List<String> order = List.of("minecraft:amethyst_shard", "minecraft:redstone", "minecraft:glowstone_dust");
        for (Map.Entry<String, Map<Integer, String>> entry : markersByStator.entrySet()) {
            int last = -1;
            for (String marker : entry.getValue().values()) {
                int index = order.indexOf(marker);
                assertTrue(index > last, entry.getKey() + ": KV-Kennzeichen steigen nicht mit der KV " + entry.getValue());
                last = index;
            }
        }
    }
}
