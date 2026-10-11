package de.lardtom.justmoreparts.power;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.lardtom.justmoreparts.bench.PartFiles;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.part.BatteryDefinition;
import de.lardtom.propwash.api.part.CellChemistry;
import de.lardtom.propwash.api.part.PartKind;
import de.lardtom.propwash.api.part.StackDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PowerDataTest {
    private static final char MINUS = '−';
    private static final Set<String> CONNECTORS = Set.of("PH2.0", "BT2.0", "BT3.0", "A30", "XT30", "XT60", "XT90", "AS150");
    private static final List<String> TRANSLITERATIONS = List.of("fuer", "ueber", "groess", "gross", "hoeher", "laenger", "kuerzer",
            "schwaech", "staerk", "waerme", "moeglich", "traeg", "spuer", "faehig", "naechst", "haelt", "faellt", "duenn", "koenn",
            "muess", "zusaetz", "flaech", "guenstig", "leistungsfaeh", "heiss", "weiss", "schliess", "fliess", "spaet", "gewoehn",
            "maessig", "abhaeng");
    private static final List<String> DISPLAY_CONTEXTS = List.of("gui", "ground", "fixed", "on_shelf", "thirdperson_righthand",
            "firstperson_righthand", "head");
    private static final Set<String> LATE_STRUCTURES = Set.of("stronghold", "vault", "vault_ominous", "ancient_city", "end_city");
    private static final Pattern BATTERY_DE = Pattern.compile("^(\\d+)S-Akku (\\d+) mAh(?: \\((.+)\\))?$");
    private static final Pattern BATTERY_EN = Pattern.compile("^(\\d+)S (\\d+) mAh Battery(?: \\((.+)\\))?$");

    @Test
    public void partFilesFollowTheCatalogRules() {
        PartFiles.registerAll();
        List<String> errors = new ArrayList<>();
        for (String id : PowerFiles.ids("battery")) {
            JsonObject json = PowerFiles.part("battery", id);
            BatteryDefinition b = PropwashApi.parts().get(BatteryDefinition.class, Identifier.fromNamespaceAndPath(PowerFiles.NS, id)).orElseThrow();
            if (!id.matches("b" + b.cells() + "s_" + b.capacityMah() + "(_hv|_lion|_ss)?")) {
                errors.add(id + ": Id passt nicht zu Zellen/Kapazität");
            }
            String suffix = id.endsWith("_hv") ? "propwash:lihv" : id.endsWith("_lion") ? "propwash:lion" : id.endsWith("_ss")
                    ? "justmoreparts:lihv_ss" : "propwash:lipo";
            if (!b.chemistry().toString().equals(suffix)) {
                errors.add(id + ": Chemie " + b.chemistry() + " passt nicht zur Endung");
            }
            int sort = json.get("sort").getAsInt();
            if (sort < 300 || sort > 399) {
                errors.add(id + ": sort " + sort + " außerhalb 300–399");
            }
            if (!json.has("visual") || !json.getAsJsonObject("visual").has("accent")) {
                errors.add(id + ": Akzentfarbe fehlt");
            }
            if (!CONNECTORS.contains(b.connector())) {
                errors.add(id + ": Stecker " + b.connector());
            }
            if (b.cells() == 1 && !b.connector().startsWith("PH") && !b.connector().startsWith("BT")) {
                errors.add(id + ": 1S-Akku mit großem Stecker " + b.connector());
            }
            if (json.has("chemistry") && json.get("chemistry").getAsString().equals("propwash:lipo")) {
                errors.add(id + ": Standardchemie ausgeschrieben");
            }
            double nominal = PropwashApi.parts().chemistry(b.chemistry()).orElseThrow().nominalCellVoltage();
            double whPerKg = b.cells() * nominal * b.capacityMah() / b.basics().massGrams();
            double[] range = energyDensity(b);
            if (whPerKg < range[0] || whPerKg > range[1]) {
                errors.add(String.format(Locale.ROOT, "%s: %.1f Wh/kg außerhalb %.0f–%.0f", id, whPerKg, range[0], range[1]));
            }
            boolean lion = id.endsWith("_lion");
            if (lion != b.packSize().isPresent()) {
                errors.add(id + ": pack_mm nur und immer bei Li-Ion");
            }
            if (lion) {
                double cellVolume = Math.PI * 1.05 * 1.05 * 6.5;
                int parallel = b.capacityMah() > 5000 ? 2 : 1;
                double volume = b.packSize().get().lengthMm() * b.packSize().get().widthMm() * b.packSize().get().heightMm() / 1000.0;
                if (volume < b.cells() * parallel * cellVolume) {
                    errors.add(id + ": Packmaße zu klein für " + b.cells() * parallel + " Rundzellen");
                }
            }
        }
        for (String id : PowerFiles.ids("stack")) {
            JsonObject json = PowerFiles.part("stack", id);
            StackDefinition s = PropwashApi.parts().get(StackDefinition.class, Identifier.fromNamespaceAndPath(PowerFiles.NS, id)).orElseThrow();
            String prefix = s.allInOne() ? "aio_" : "stack_";
            int amps = (int) Math.round(s.escContinuousAmps());
            if (!id.startsWith(prefix) || !id.endsWith("_" + s.maxCells() + "s_" + amps + "a")) {
                errors.add(id + ": Id passt nicht zu AIO/Zellen/Strom");
            }
            int sort = json.get("sort").getAsInt();
            if (sort < 400 || sort > 499) {
                errors.add(id + ": sort " + sort + " außerhalb 400–499");
            }
            if (!json.has("visual") || !json.getAsJsonObject("visual").has("accent")) {
                errors.add(id + ": Akzentfarbe fehlt");
            }
            if (json.has("all_in_one") && !json.get("all_in_one").getAsBoolean()) {
                errors.add(id + ": Standardwert all_in_one=false ausgeschrieben");
            }
            if (json.has("esc_burst_a") && Math.abs(json.get("esc_burst_a").getAsDouble() - 1.3 * s.escContinuousAmps()) < 1e-9) {
                errors.add(id + ": Standardwert esc_burst_a ausgeschrieben");
            }
            if (s.minCells() == 1 && s.brownoutVoltage().orElse(0.0) != 2.9) {
                errors.add(id + ": 1S-fähig ohne Brownout 2,9 V");
            }
        }
        assertTrue(errors.isEmpty(), String.join("\n", errors));
    }

    private static double[] energyDensity(BatteryDefinition b) {
        String id = b.id().getPath();
        if (id.endsWith("_lion")) {
            return new double[]{205, 225};
        }
        if (id.endsWith("_ss")) {
            return new double[]{220, 250};
        }
        if (b.cells() == 1) {
            return new double[]{145, 160};
        }
        if (b.capacityMah() <= 650) {
            return new double[]{120, 142};
        }
        if (b.capacityMah() <= 1500) {
            return new double[]{125, 142};
        }
        return new double[]{138, 152};
    }

    @Test
    public void semiSolidChemistryMatchesTheCatalog() {
        PartFiles.registerAll();
        CellChemistry c = PropwashApi.parts().chemistry(PowerModule.SEMI_SOLID).orElseThrow();
        assertEquals(4.45, c.fullCellVoltage(), 1e-9);
        assertEquals(3.85, c.nominalCellVoltage(), 1e-9);
        assertEquals(3.5, c.emptyCellVoltage(), 1e-9);
        assertEquals(3.3, c.cutoffCellVoltage(), 1e-9);
        assertEquals(3.9, c.storageCellVoltage(), 1e-9);
        assertEquals(2.0, c.densityGramsPerCm3(), 1e-9);
        CellChemistry lipo = PropwashApi.parts().chemistry(BatteryDefinition.LIPO).orElseThrow();
        assertEquals(lipo.ocvShape(), c.ocvShape());
        assertTrue(c.fullCellVoltage() > PropwashApi.parts().chemistry(BatteryDefinition.LIHV).orElseThrow().fullCellVoltage());
    }

    @Test
    public void namesMatchTheCatalog() throws IOException {
        Map<String, String[]> catalog = new HashMap<>();
        Pattern row = Pattern.compile("^\\| `((?:b|aio_|stack_)[a-z0-9_]+)` \\| ([^|]+) \\| ([^|]+) \\|");
        for (String line : Files.readAllLines(PowerFiles.project().resolve("docs/design/parts-catalog.md"), StandardCharsets.UTF_8)) {
            Matcher m = row.matcher(line);
            if (m.find()) {
                catalog.put(m.group(1), new String[]{m.group(2).trim(), m.group(3).trim()});
            }
        }
        Map<String, String> de = PowerFiles.lang("de_de");
        Map<String, String> en = PowerFiles.lang("en_us");
        List<String> errors = new ArrayList<>();
        for (String id : PowerFiles.powerIds()) {
            String[] names = catalog.get(id);
            if (names == null) {
                errors.add(id + ": fehlt im Teile-Katalog §5");
                continue;
            }
            if (!names[0].equals(de.get("part." + PowerFiles.NS + "." + id))) {
                errors.add(id + ": deutscher Name weicht vom Katalog ab: " + de.get("part." + PowerFiles.NS + "." + id));
            }
            if (!names[1].equals(en.get("part." + PowerFiles.NS + "." + id))) {
                errors.add(id + ": englischer Name weicht vom Katalog ab: " + en.get("part." + PowerFiles.NS + "." + id));
            }
        }
        assertTrue(errors.isEmpty(), String.join("\n", errors));
    }

    @Test
    public void tooltipsHaveProsConsAndMatchingLines() {
        Map<String, String> de = PowerFiles.lang("de_de");
        Map<String, String> en = PowerFiles.lang("en_us");
        assertEquals(de.keySet(), en.keySet(), "de_de und en_us haben verschiedene Schlüssel");
        List<String> errors = new ArrayList<>();
        for (String id : PowerFiles.powerIds()) {
            for (Map<String, String> table : List.of(de, en)) {
                String name = table.get("part." + PowerFiles.NS + "." + id);
                String summary = table.get(PowerFiles.NS + ".help." + id + ".summary");
                if (name == null || summary == null) {
                    errors.add(id + ": Name oder Zusammenfassung fehlt");
                    continue;
                }
                String head = summaryHead(id, name, table == de);
                if (!summary.startsWith(head + " – ")) {
                    errors.add(id + ": Zusammenfassung beginnt nicht mit „" + head + " – “: " + summary);
                }
                List<String> what = lines(table, id, "what");
                List<String> use = lines(table, id, "use");
                if (what.size() < 3 || use.isEmpty()) {
                    errors.add(id + ": zu wenige Hilfezeilen");
                    continue;
                }
                if (!what.get(0).contains(" · ") || what.get(0).startsWith("+") || what.get(0).charAt(0) == MINUS) {
                    errors.add(id + ": what.1 muss die Kennzahlen tragen");
                }
                boolean plus = false;
                boolean minus = false;
                for (String line : what.subList(1, what.size())) {
                    plus |= line.startsWith("+ ");
                    minus |= line.charAt(0) == MINUS && line.charAt(1) == ' ';
                    if (!line.startsWith("+ ") && line.charAt(0) != MINUS) {
                        errors.add(id + ": Zeile ohne +/−: " + line);
                    }
                }
                if (!plus || !minus) {
                    errors.add(id + ": braucht mindestens ein Pro und ein Contra");
                }
                for (String line : what) {
                    if (line.length() > 48) {
                        errors.add(id + ": zu lang: " + line);
                    }
                    if (line.startsWith("- ")) {
                        errors.add(id + ": Bindestrich statt Minuszeichen: " + line);
                    }
                }
                for (String line : use) {
                    if (line.length() > 48) {
                        errors.add(id + ": zu lang: " + line);
                    }
                }
            }
            if (lines(de, id, "what").size() != lines(en, id, "what").size() || lines(de, id, "use").size() != lines(en, id, "use").size()) {
                errors.add(id + ": de und en haben verschieden viele Zeilen");
            }
        }
        for (String key : de.keySet()) {
            String value = de.get(key);
            for (String word : value.toLowerCase(Locale.ROOT).split("[^a-zäöüß]+")) {
                for (String bad : TRANSLITERATIONS) {
                    if (word.contains(bad)) {
                        errors.add("de_de " + key + ": Umschreibung '" + word + "' statt Umlaut/ß");
                    }
                }
            }
            if (value.contains("\"")) {
                errors.add("de_de " + key + ": gerade Anführungszeichen statt „…“");
            }
        }
        assertTrue(de.containsKey(PowerFiles.NS + ".chemistry.lihv_ss"));
        assertTrue(errors.isEmpty(), String.join("\n", errors));
    }

    private static List<String> lines(Map<String, String> table, String id, String kind) {
        List<String> out = new ArrayList<>();
        for (int n = 1; n <= 8; n++) {
            String line = table.get(PowerFiles.NS + ".help." + id + "." + kind + "." + n);
            if (line == null) {
                break;
            }
            out.add(line);
        }
        for (int n = out.size() + 2; n <= 8; n++) {
            if (table.containsKey(PowerFiles.NS + ".help." + id + "." + kind + "." + n)) {
                out.add("LÜCKE in " + kind);
            }
        }
        return out;
    }

    private record Recipe(String file, String type, String result, List<String> grid, List<String> multiset) {
    }

    private static Recipe recipe(String file, JsonObject json) {
        String type = json.get("type").getAsString();
        String result = json.has("result") && json.get("result").isJsonObject() ? json.getAsJsonObject("result").get("id").getAsString() : "";
        List<String> multiset = new ArrayList<>();
        List<String> grid = new ArrayList<>();
        if (type.equals("minecraft:crafting_shaped")) {
            JsonObject key = json.getAsJsonObject("key");
            for (JsonElement rowElement : json.getAsJsonArray("pattern")) {
                String row = rowElement.getAsString();
                StringBuilder cells = new StringBuilder();
                for (char c : row.toCharArray()) {
                    String ingredient = c == ' ' ? "_" : key.get(String.valueOf(c)).toString();
                    cells.append(ingredient).append(';');
                    if (c != ' ') {
                        multiset.add(ingredient);
                    }
                }
                grid.add(cells.toString());
            }
        } else if (type.equals("minecraft:crafting_shapeless")) {
            for (JsonElement e : json.getAsJsonArray("ingredients")) {
                multiset.add(e.toString());
            }
        }
        multiset.sort(String::compareTo);
        return new Recipe(file, type, result, grid, multiset);
    }

    private static List<String> mirror(List<String> grid) {
        List<String> out = new ArrayList<>();
        for (String row : grid) {
            String[] cells = row.split(";");
            StringBuilder sb = new StringBuilder();
            for (int i = cells.length - 1; i >= 0; i--) {
                sb.append(cells[i]).append(';');
            }
            out.add(sb.toString());
        }
        return out;
    }

    @Test
    public void recipesAreUniqueBalancedAndComplete() {
        PartFiles.registerAll();
        Path recipeDir = PowerFiles.resources().resolve("data/" + PowerFiles.NS + "/recipe");
        List<Recipe> own = new ArrayList<>();
        List<Recipe> all = new ArrayList<>();
        for (Path path : PowerFiles.walk(recipeDir)) {
            Recipe r = recipe(path.getFileName().toString(), PowerFiles.json(path));
            all.add(r);
            String id = path.getFileName().toString().replace(".json", "");
            if (PowerFiles.powerIds().contains(id)) {
                own.add(r);
            }
        }
        for (Map.Entry<String, JsonObject> entry : PowerFiles.classpathDirectory("data/propwash/recipe", "b6s_1300.json").entrySet()) {
            all.add(recipe(entry.getKey(), entry.getValue()));
        }
        assertTrue(all.size() > own.size() + 20, "Propwash-Rezepte nicht gefunden");
        List<String> errors = new ArrayList<>();
        Set<String> covered = new HashSet<>();
        for (Recipe r : own) {
            String id = r.file().replace(".json", "");
            covered.add(id);
            JsonObject json = PowerFiles.json(recipeDir.resolve(r.file()));
            if (!r.result().equals(PowerFiles.NS + ":" + id)) {
                errors.add(r.file() + ": Ergebnis " + r.result());
            }
            String group = id.startsWith("b") ? "propwash_battery" : "propwash_stack";
            if (!group.equals(json.get("group").getAsString()) || !"equipment".equals(json.get("category").getAsString())) {
                errors.add(r.file() + ": Gruppe/Reiter falsch");
            }
            for (Recipe other : all) {
                if (other == r) {
                    continue;
                }
                boolean clash = false;
                if (r.type().equals(other.type()) && r.type().endsWith("shapeless")) {
                    clash = r.multiset().equals(other.multiset());
                } else if (r.type().equals(other.type()) && r.type().endsWith("shaped")) {
                    clash = r.grid().equals(other.grid()) || r.grid().equals(mirror(other.grid()));
                } else if (r.type().startsWith("minecraft:crafting") && other.type().startsWith("minecraft:crafting")) {
                    clash = r.multiset().equals(other.multiset());
                }
                if (clash) {
                    errors.add(r.file() + " kollidiert mit " + other.file());
                }
            }
            if (id.startsWith("b")) {
                BatteryDefinition b = PropwashApi.parts().get(BatteryDefinition.class, Identifier.fromNamespaceAndPath(PowerFiles.NS, id)).orElseThrow();
                int cells = 0;
                for (String ingredient : r.multiset()) {
                    if (ingredient.equals("\"propwash:lipo_cell\"")) {
                        cells++;
                    } else if (ingredient.startsWith("\"" + PowerFiles.NS + ":b")) {
                        String inner = ingredient.substring(PowerFiles.NS.length() + 2, ingredient.length() - 1);
                        cells += PropwashApi.parts().get(BatteryDefinition.class, Identifier.fromNamespaceAndPath(PowerFiles.NS, inner))
                                .orElseThrow().cells();
                    }
                }
                int expected = b.cells() * (b.capacityMah() > 5000 && id.endsWith("_lion") ? 2 : 1);
                if (cells != expected) {
                    errors.add(r.file() + ": " + cells + " LiPo-Zellen statt " + expected);
                }
            } else if (r.multiset().stream().noneMatch(i -> i.equals("\"propwash:circuit_board\""))) {
                errors.add(r.file() + ": Stack ohne Platine");
            }
            Path advancement = PowerFiles.resources().resolve("data/" + PowerFiles.NS + "/advancement/recipes/"
                    + (id.startsWith("b") ? "battery" : "stack") + "/" + id + ".json");
            if (!Files.exists(advancement)) {
                errors.add(r.file() + ": Rezept-Fortschritt fehlt (tools/parts_sync.py --write)");
            }
        }
        for (String id : PowerFiles.powerIds()) {
            if (!covered.contains(id)) {
                errors.add(id + ": kein Rezept");
            }
        }
        assertTrue(errors.isEmpty(), String.join("\n", errors));
    }

    @Test
    public void recipeCostGrowsWithTheTier() {
        Map<String, Integer> weight = Map.ofEntries(Map.entry("propwash:lipo_cell", 4), Map.entry("propwash:circuit_board", 5),
                Map.entry("minecraft:copper_nugget", 1), Map.entry("minecraft:copper_ingot", 3), Map.entry("minecraft:iron_nugget", 1),
                Map.entry("minecraft:iron_ingot", 4), Map.entry("minecraft:iron_block", 30), Map.entry("minecraft:gold_ingot", 8),
                Map.entry("minecraft:redstone", 1), Map.entry("minecraft:glowstone_dust", 2), Map.entry("minecraft:quartz", 2),
                Map.entry("minecraft:amethyst_shard", 3), Map.entry("minecraft:diamond", 30), Map.entry("minecraft:slime_ball", 3),
                Map.entry("minecraft:slime_block", 25), Map.entry("minecraft:string", 1));
        Map<String, Integer> cost = new TreeMap<>();
        Path recipeDir = PowerFiles.resources().resolve("data/" + PowerFiles.NS + "/recipe");
        for (String id : PowerFiles.powerIds()) {
            Recipe r = recipe(id + ".json", PowerFiles.json(recipeDir.resolve(id + ".json")));
            int total = 0;
            for (String ingredient : r.multiset()) {
                String item = ingredient.substring(1, ingredient.length() - 1);
                total += item.startsWith(PowerFiles.NS + ":") ? cost.getOrDefault(item.substring(PowerFiles.NS.length() + 1), 0)
                        : weight.getOrDefault(item, 1000);
            }
            cost.put(id, total);
        }
        String[][] tiers = {{"b1s_250_hv", "b1s_380_hv", "b1s_450_hv", "b1s_550_hv", "b1s_750_hv"}, {"b2s_300_hv", "b2s_450_hv", "b2s_650"},
                {"b3s_450", "b3s_650"}, {"b4s_450", "b4s_850", "b4s_1100", "b4s_1500", "b4s_1800"},
                {"b6s_550", "b6s_750", "b6s_850", "b6s_1500", "b6s_1800", "b6s_2200", "b6s_5000"},
                {"b6s_3000_lion", "b6s_4500_lion", "b6s_8400_lion"}, {"b8s_2200", "b8s_5000"},
                {"aio_whoop_1s_6a", "aio_whoop_2s_12a", "aio_tp_4s_20a", "aio_20_4s_35a", "stack_30_4s_55a"},
                {"aio_tp_6s_20a", "aio_20_6s_45a", "stack_30_6s_60a", "stack_30_6s_80a"},
                {"stack_30_6s_60a", "stack_30_8s_65a", "stack_30_8s_100a"}};
        List<String> errors = new ArrayList<>();
        for (String[] tier : tiers) {
            for (int i = 1; i < tier.length; i++) {
                if (cost.get(tier[i]) < cost.get(tier[i - 1])) {
                    errors.add(tier[i] + " (" + cost.get(tier[i]) + ") billiger als " + tier[i - 1] + " (" + cost.get(tier[i - 1]) + ")");
                }
            }
        }
        assertTrue(cost.get("b8s_5000_ss") > cost.get("b8s_2200"));
        assertTrue(cost.get("b6s_3000_ss") > cost.get("b6s_2200"));
        assertTrue(errors.isEmpty(), String.join("\n", errors) + "\n" + cost);
    }

    @Test
    public void itemsTagsAndRarities() {
        PartFiles.registerAll();
        List<String> errors = new ArrayList<>();
        for (String category : List.of("battery", "stack")) {
            JsonObject tag = PowerFiles.json(PowerFiles.resources().resolve("data/propwash/tags/item/parts/" + category + ".json"));
            Set<String> values = new HashSet<>();
            for (JsonElement e : tag.getAsJsonArray("values")) {
                values.add(e.getAsString());
            }
            if (tag.get("replace").getAsBoolean()) {
                errors.add(category + ": Tag ersetzt Propwash-Einträge");
            }
            for (String id : PowerFiles.ids(category)) {
                if (!values.contains(PowerFiles.NS + ":" + id)) {
                    errors.add(category + ": " + id + " fehlt im Tag");
                }
                if (!Files.exists(PowerFiles.resources().resolve("assets/" + PowerFiles.NS + "/items/" + id + ".json"))) {
                    errors.add(id + ": Item-Symbol fehlt");
                }
                if (!PropwashApi.parts().get(Identifier.fromNamespaceAndPath(PowerFiles.NS, id)).orElseThrow().basics().creativeListed()) {
                    errors.add(id + ": nicht im Kreativ-Tab");
                }
            }
        }
        for (String id : PowerModule.rarities().keySet()) {
            if (!PowerFiles.powerIds().contains(id)) {
                errors.add("Seltenheit für unbekanntes Teil " + id);
            }
        }
        assertTrue(errors.isEmpty(), String.join("\n", errors));
    }

    @Test
    public void chestLootIsConsistentAndCoversEveryPart() {
        PartFiles.registerAll();
        Path tableDir = PowerFiles.resources().resolve("data/" + PowerFiles.NS + "/loot_table/" + PowerLoot.TABLE_PREFIX);
        Map<String, JsonObject> tables = new LinkedHashMap<>();
        for (Path path : PowerFiles.walk(tableDir)) {
            tables.put(PowerFiles.NS + ":" + PowerLoot.TABLE_PREFIX + path.getFileName().toString().replace(".json", ""), PowerFiles.json(path));
        }
        Set<String> expectedTables = new HashSet<>();
        for (ResourceKey<LootTable> key : PowerLoot.tables()) {
            expectedTables.add(key.identifier().toString());
        }
        assertEquals(expectedTables, tables.keySet());
        for (PowerLoot.Injection injection : PowerLoot.injections()) {
            String vanilla = "data/minecraft/loot_table/" + injection.target().identifier().getPath() + ".json";
            if (PowerFiles.class.getClassLoader().getResource("data/minecraft/loot_table/chests/simple_dungeon.json") != null) {
                assertNotNull(PowerFiles.class.getClassLoader().getResource(vanilla), "Vanilla-Tabelle fehlt: " + vanilla);
            }
        }
        Set<String> found = new HashSet<>();
        List<String> errors = new ArrayList<>();
        for (Map.Entry<String, JsonObject> entry : tables.entrySet()) {
            JsonObject table = entry.getValue();
            String name = entry.getKey().substring(entry.getKey().lastIndexOf('/') + 1);
            JsonArray pools = table.getAsJsonArray("pools");
            if (!"minecraft:chest".equals(table.get("type").getAsString()) || pools.size() != 1) {
                errors.add(name + ": Typ oder Pools falsch");
                continue;
            }
            JsonObject pool = pools.get(0).getAsJsonObject();
            if (pool.get("rolls").getAsInt() != 1) {
                errors.add(name + ": genau ein Wurf");
            }
            int empty = 0;
            int items = 0;
            for (JsonElement e : pool.getAsJsonArray("entries")) {
                JsonObject entryJson = e.getAsJsonObject();
                int w = entryJson.get("weight").getAsInt();
                if (w <= 0) {
                    errors.add(name + ": Gewicht ≤ 0");
                }
                if (entryJson.get("type").getAsString().equals("minecraft:empty")) {
                    empty += w;
                    continue;
                }
                items += w;
                Identifier item = Identifier.parse(entryJson.get("name").getAsString());
                String path = item.getPath();
                if (!item.getNamespace().equals(PowerFiles.NS) || !PowerFiles.powerIds().contains(path)) {
                    errors.add(name + ": fremdes Item " + item);
                }
                if (PowerModule.rarities().containsKey(path) && !LATE_STRUCTURES.contains(name)) {
                    errors.add(name + ": X-Class-/Semi-Solid-Teil " + path + " zu früh");
                }
                found.add(path);
            }
            double chance = 100.0 * items / (items + empty);
            if (chance < 5.0 || chance > 30.0) {
                errors.add(String.format(Locale.ROOT, "%s: Fundchance %.0f %% außerhalb 5–30 %%", name, chance));
            }
        }
        for (String id : PowerFiles.powerIds()) {
            if (!found.contains(id)) {
                errors.add(id + ": in keiner Truhe");
            }
        }
        assertTrue(errors.isEmpty(), String.join("\n", errors));
    }

    @Test
    public void modelsAndTexturesExistForEveryPart() throws IOException {
        List<String> errors = new ArrayList<>();
        Path assets = PowerFiles.resources().resolve("assets/" + PowerFiles.NS);
        for (String category : List.of("battery", "stack")) {
            for (String id : PowerFiles.ids(category)) {
                for (String variant : List.of(id, id + "_paint")) {
                    Path model = assets.resolve("models/part/" + category + "/" + variant + ".json");
                    Path texture = assets.resolve("textures/item/part/" + category + "/" + variant + ".png");
                    Path render = assets.resolve("items/drone_render/" + category + "/" + variant + ".json");
                    if (!Files.exists(model) || !Files.exists(texture) || !Files.exists(render)) {
                        errors.add(variant + ": Modell, Textur oder Render-Definition fehlt (tools/assets/style_generate.py --write)");
                        continue;
                    }
                    JsonObject json = PowerFiles.json(model);
                    String textureId = PowerFiles.NS + ":item/part/" + category + "/" + variant;
                    if (!textureId.equals(json.getAsJsonObject("textures").get("0").getAsString())) {
                        errors.add(variant + ": Modell zeigt auf falsche Textur");
                    }
                    for (JsonElement e : json.getAsJsonArray("elements")) {
                        JsonObject element = e.getAsJsonObject();
                        for (String key : List.of("from", "to")) {
                            for (JsonElement v : element.getAsJsonArray(key)) {
                                if (v.getAsDouble() < -16.0 || v.getAsDouble() > 32.0) {
                                    errors.add(variant + ": Element " + element.get("name") + " außerhalb −16…32");
                                }
                            }
                        }
                    }
                    byte[] png = Files.readAllBytes(texture);
                    int width = ((png[16] & 0xFF) << 24) | ((png[17] & 0xFF) << 16) | ((png[18] & 0xFF) << 8) | (png[19] & 0xFF);
                    int height = ((png[20] & 0xFF) << 24) | ((png[21] & 0xFF) << 16) | ((png[22] & 0xFF) << 8) | (png[23] & 0xFF);
                    if (png[1] != 'P' || width != 64 || height != 64) {
                        errors.add(variant + ": Textur kein 64×64-PNG");
                    }
                    String modelRef = PowerFiles.json(render).getAsJsonObject("model").get("model").getAsString();
                    if (!modelRef.equals(PowerFiles.NS + ":part/" + category + "/" + variant)) {
                        errors.add(variant + ": Render-Definition zeigt auf " + modelRef);
                    }
                    if (!"side".equals(json.has("gui_light") ? json.get("gui_light").getAsString() : "")) {
                        errors.add(variant + ": gui_light nicht side");
                    }
                    JsonObject display = json.getAsJsonObject("display");
                    for (String context : DISPLAY_CONTEXTS) {
                        if (display == null || !display.has(context)) {
                            errors.add(variant + ": Anzeige-Transformation " + context + " fehlt");
                            continue;
                        }
                        for (JsonElement v : display.getAsJsonObject(context).getAsJsonArray("scale")) {
                            if (v.getAsDouble() <= 0.0 || v.getAsDouble() > 4.0) {
                                errors.add(variant + ": Skala " + context + " außerhalb 0…4");
                            }
                        }
                    }
                }
                JsonObject item = PowerFiles.json(assets.resolve("items/" + id + ".json")).getAsJsonObject("model");
                if (!"minecraft:select".equals(item.get("type").getAsString())
                        || !"minecraft:display_context".equals(item.get("property").getAsString())) {
                    errors.add(id + ": Item-Definition nicht im Propwash-Format (select auf display_context)");
                    continue;
                }
                String own = PowerFiles.NS + ":part/" + category + "/" + id;
                if (!own.equals(item.getAsJsonObject("fallback").get("model").getAsString())) {
                    errors.add(id + ": Item-Definition zeigt nicht auf " + own);
                }
                for (JsonElement e : item.getAsJsonArray("cases")) {
                    if (!own.equals(e.getAsJsonObject().getAsJsonObject("model").get("model").getAsString())) {
                        errors.add(id + ": Fall " + e.getAsJsonObject().get("when") + " zeigt nicht auf " + own);
                    }
                }
            }
        }
        JsonObject paint = PowerFiles.json(PowerFiles.resources().resolve("assets/" + PowerFiles.NS + "/drone_paint.json")).getAsJsonObject("models");
        for (String category : List.of("battery", "stack")) {
            for (String id : PowerFiles.ids(category)) {
                JsonArray layers = paint.getAsJsonArray(category + "/" + id);
                if (layers == null || layers.isEmpty()) {
                    errors.add(id + ": Lack-Eintrag fehlt in drone_paint.json");
                    continue;
                }
                for (JsonElement layer : layers) {
                    if (!category.equals(layer.getAsJsonObject().get("channel").getAsString())) {
                        errors.add(id + ": Lack-Kanal nicht " + category);
                    }
                }
            }
        }
        assertTrue(errors.isEmpty(), String.join("\n", errors));
    }

    @Test
    public void mergedLanguageFilesContainThePowerKeys() {
        for (String language : List.of("de_de", "en_us")) {
            JsonObject merged = PowerFiles.json(PowerFiles.resources().resolve("assets/" + PowerFiles.NS + "/lang/" + language + ".json"));
            for (Map.Entry<String, String> entry : PowerFiles.lang(language).entrySet()) {
                assertTrue(merged.has(entry.getKey()) && merged.get(entry.getKey()).getAsString().equals(entry.getValue()),
                        language + ": " + entry.getKey() + " fehlt in der zusammengeführten Datei (:common:mergeLang)");
            }
        }
    }

    static String summaryHead(String id, String name, boolean german) {
        if (id.startsWith("b")) {
            Matcher m = (german ? BATTERY_DE : BATTERY_EN).matcher(name);
            if (!m.matches()) {
                return name;
            }
            String cells = m.group(1) + "S" + ("b6s_8400_lion".equals(id) ? "2P" : "");
            String chemistry = m.group(3) == null ? "LiPo" : m.group(3);
            return cells + (german ? "-" : " ") + chemistry + ", " + m.group(2) + " mAh";
        }
        return german
                ? name.replace("-AIO-Board ", "-AIO ").replace("-FC/ESC-Stack ", "-Stack ")
                : name.replace(" AIO Board ", " AIO ").replace(" FC/ESC Stack ", " stack ");
    }
}
