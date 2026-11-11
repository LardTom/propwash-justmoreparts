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
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MotorDataTest {
    private static final Pattern ID = Pattern.compile("^m(\\d{4,5})_(\\d{3,5})$");
    private static final Set<String> MARKERS = Set.of("minecraft:amethyst_shard", "minecraft:redstone", "minecraft:glowstone_dust");
    static final double NOMINAL_CELL_V = 3.7;
    static final double ALLROUND_WHOOP_KRPM = 100.0;
    static final double RACE_WHOOP_KRPM = 135.0;
    static final Set<String> RACE_WHOOPS = Set.of("m0702_30000", "m0702_35000", "m0802_27200");

    record Sheet(String maker, int kv, double currentA, double massG) {
    }

    record DataClass(String motor, List<Sheet> sheets) {
    }

    static final List<DataClass> DATA_CLASSES = List.of(
            new DataClass("m0702_30000", List.of(
                    new Sheet("A", 30000, Double.NaN, 1.55),
                    new Sheet("A", 30000, 6.03, 1.50),
                    new Sheet("B", 30000, Double.NaN, 1.29),
                    new Sheet("C", 28000, 4.74, 1.50),
                    new Sheet("D", 31800, 5.4, 1.15))),
            new DataClass("m0702_35000", List.of(
                    new Sheet("D", 33800, 5.7, 1.15),
                    new Sheet("E", 35000, Double.NaN, 1.39),
                    new Sheet("A", 36000, 7.17, 1.59))),
            new DataClass("m0802_27200", List.of(
                    new Sheet("F", 27000, 5.39, 1.91),
                    new Sheet("D", 27000, 7.2, 1.97),
                    new Sheet("B", 27400, 6.2, 2.15),
                    new Sheet("A", 28000, 9.0, 1.95))));

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

    static double median(List<Double> values) {
        List<Double> sorted = values.stream().sorted().toList();
        int n = sorted.size();
        return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    static double idleKrpm(MotorDefinition motor) {
        return motor.kv() * NOMINAL_CELL_V * motor.maxCells() / 1000.0;
    }

    @Test
    public void raceWhoopMotorsFollowTheirDatasheetClass() {
        Map<String, MotorDefinition> byPath = new HashMap<>();
        for (MotorDefinition motor : motors()) {
            byPath.put(motor.id().getPath(), motor);
        }
        assertEquals(RACE_WHOOPS, DATA_CLASSES.stream().map(DataClass::motor).collect(Collectors.toSet()));
        for (DataClass data : DATA_CLASSES) {
            MotorDefinition motor = byPath.get(data.motor());
            assertTrue(motor != null, data.motor() + " fehlt");
            assertTrue(data.sheets().stream().map(Sheet::maker).distinct().count() >= 3, data.motor() + ": weniger als drei Hersteller");
            double kv = median(data.sheets().stream().map(s -> (double) s.kv()).toList());
            assertEquals(Math.round(kv / 100.0) * 100, motor.kv(), data.motor() + ": KV ist nicht der Median der Datenblätter");
            List<Double> currents = data.sheets().stream().map(Sheet::currentA).filter(c -> !Double.isNaN(c)).toList();
            assertTrue(currents.size() >= 2, data.motor() + ": zu wenige Spitzenströme");
            assertEquals(Math.round(median(currents) * 10.0) / 10.0, motor.maxCurrentAmps(), 1e-9, data.motor() + ": Spitzenstrom");
            double heaviest = data.sheets().stream().mapToDouble(Sheet::massG).max().orElseThrow();
            assertTrue(motor.basics().massGrams() >= heaviest && motor.basics().massGrams() <= heaviest * 1.5,
                    data.motor() + ": Masse " + motor.basics().massGrams() + " g gegen Datenblätter bis " + heaviest + " g");
            for (MotorDefinition sibling : byPath.values()) {
                if (sibling.statorLabel().equals(motor.statorLabel())) {
                    assertEquals(sibling.basics().massGrams(), motor.basics().massGrams(), 1e-9, data.motor() + ": Masse wie " + sibling.id());
                }
            }
            assertEquals(115.0, motor.thermal().derateStartC(), 1e-9, data.motor() + ": Hitzegrenze wie der 0702/27000");
        }
    }

    @Test
    public void whoopIdleSpeedSeparatesAllroundAndRaceWhoops() {
        int race = 0;
        for (MotorDefinition motor : motors()) {
            if (!"propwash:m08".equals(motor.mount().toString()) || motor.maxCells() != 1) {
                continue;
            }
            double krpm = idleKrpm(motor);
            String path = motor.id().getPath();
            if (RACE_WHOOPS.contains(path)) {
                race++;
                assertTrue(krpm > ALLROUND_WHOOP_KRPM && krpm <= RACE_WHOOP_KRPM, path + ": " + krpm + " k/min außerhalb des Renn-Fensters");
            } else {
                assertTrue(krpm <= ALLROUND_WHOOP_KRPM, path + ": " + krpm + " k/min über dem Fenster der Allround-Whoops");
            }
        }
        assertEquals(RACE_WHOOPS.size(), race);
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
        Map<String, Map<Integer, Set<String>>> ingredientsByStator = new HashMap<>();
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
            Set<String> ingredients = new TreeSet<>();
            key.entrySet().forEach(e -> ingredients.add(e.getValue().getAsString()));
            ingredientsByStator.computeIfAbsent(motor.statorLabel(), k -> new TreeMap<>()).put(motor.kv(), ingredients);
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
            Set<String> lastIngredients = Set.of();
            for (Map.Entry<Integer, String> step : entry.getValue().entrySet()) {
                int index = order.indexOf(step.getValue());
                Set<String> ingredients = ingredientsByStator.get(entry.getKey()).get(step.getKey());
                assertTrue(index >= last, entry.getKey() + ": KV-Kennzeichen steigen nicht mit der KV " + entry.getValue());
                if (index == last) {
                    assertTrue(index == order.size() - 1 && !lastIngredients.containsAll(ingredients),
                            entry.getKey() + " " + step.getKey() + " KV: gleiches Kennzeichen nur bei hoher KV mit zusätzlicher Zutat");
                }
                last = index;
                lastIngredients = ingredients;
            }
        }
    }
}
