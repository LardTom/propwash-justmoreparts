package de.lardtom.justmoreparts.prop;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.lardtom.justmoreparts.bench.PartFiles;
import de.lardtom.propwash.api.part.PropDefinition;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PropDataTest {
    private static final List<String> VARIANTS = List.of("", "_ccw", "_blur", "_worn", "_worn_ccw", "_damaged", "_damaged_ccw", "_broken",
            "_broken_ccw");

    static Path root() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("settings.gradle"))) {
            dir = dir.getParent();
        }
        assertNotNull(dir, "Projektwurzel");
        return dir;
    }

    static JsonObject resource(String path) {
        try (InputStream in = PropDataTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, "fehlt: " + path);
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static boolean exists(String path) {
        return PropDataTest.class.getClassLoader().getResource(path) != null;
    }

    static JsonObject file(Path path) {
        try {
            return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static List<String> props() {
        return PropBenchTest.propFiles();
    }

    @Test
    public void lootInjectionsPointAtOwnChestTables() {
        PartFiles.registerAll();
        Set<String> covered = new HashSet<>();
        for (PropLoot.Injection injection : PropLoot.injections()) {
            assertTrue(PropLoot.tableFor(injection.target()).isPresent());
        }
        for (ResourceKey<LootTable> table : PropLoot.tables()) {
            Identifier id = table.identifier();
            assertEquals("justmoreparts", id.getNamespace());
            assertTrue(id.getPath().startsWith(PropLoot.TABLE_PREFIX), id.toString());
            JsonObject loot = resource("data/justmoreparts/loot_table/" + id.getPath() + ".json");
            assertEquals("minecraft:chest", loot.get("type").getAsString());
            JsonArray pools = loot.getAsJsonArray("pools");
            assertEquals(1, pools.size(), id + ": ein Pool");
            JsonObject pool = pools.get(0).getAsJsonObject();
            assertEquals(1, pool.get("rolls").getAsInt());
            boolean hasEmpty = false;
            for (JsonElement element : pool.getAsJsonArray("entries")) {
                JsonObject entry = element.getAsJsonObject();
                String type = entry.get("type").getAsString();
                if (type.equals("minecraft:empty")) {
                    hasEmpty = entry.get("weight").getAsInt() >= 60;
                    continue;
                }
                assertEquals("minecraft:item", type);
                Identifier item = Identifier.parse(entry.get("name").getAsString());
                assertEquals("justmoreparts", item.getNamespace());
                PropDefinition prop = PropBenchTest.prop(item.getPath());
                int count = entry.getAsJsonObject("modifier").get("count").getAsInt();
                assertEquals(prop.diameterMm() >= PropModule.X_CLASS_MIN_MM ? 2 : 4, count, item + " Anzahl");
                covered.add(item.getPath());
            }
            assertTrue(hasEmpty, id + ": großer Leer-Eintrag, damit Funde selten bleiben");
        }
        assertEquals(new TreeSet<>(props()), new TreeSet<>(covered), "jeder Prop in mindestens einer Truhe");
    }

    @Test
    public void everyPropHasAUniqueTieredRecipe() {
        Map<String, String> grids = new HashMap<>();
        Map<Integer, String> dyeByDiameter = new HashMap<>();
        for (String id : props()) {
            JsonObject recipe = resource("data/justmoreparts/recipe/" + id + ".json");
            assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString(), id);
            assertEquals("equipment", recipe.get("category").getAsString(), id);
            assertEquals("propwash_prop", recipe.get("group").getAsString(), id);
            JsonObject result = recipe.getAsJsonObject("result");
            assertEquals("justmoreparts:" + id, result.get("id").getAsString());
            PropDefinition prop = PropBenchTest.prop(id);
            assertEquals(prop.diameterMm() >= PropModule.X_CLASS_MIN_MM ? 2 : 4, result.get("count").getAsInt(), id);
            JsonObject key = recipe.getAsJsonObject("key");
            List<String> pattern = new ArrayList<>();
            recipe.getAsJsonArray("pattern").forEach(e -> pattern.add(e.getAsString()));
            int blades = 0;
            for (String row : pattern) {
                for (char c : row.toCharArray()) {
                    if (c == 'B') {
                        blades++;
                    }
                }
            }
            assertEquals(prop.blades(), blades, id + ": Blattzahl im Rezept");
            String dye = key.get("C").getAsString();
            assertTrue(dye.endsWith("_dye"), id + ": Größen-Farbstoff");
            String previous = dyeByDiameter.putIfAbsent(prop.diameterMm(), dye);
            assertTrue(previous == null || previous.equals(dye), id + ": gleiche Größe, gleicher Farbstoff");
            String hub = key.get("N").getAsString();
            String expectedHub = prop.diameterMm() <= 89 ? "minecraft:iron_nugget" : prop.diameterMm() <= 203 ? "minecraft:iron_ingot"
                    : "minecraft:gold_ingot";
            assertEquals(expectedHub, hub, id + ": Nabe nach Größenklasse");
            for (String grid : grids(pattern, key)) {
                String clash = grids.put(grid, id);
                assertTrue(clash == null || clash.equals(id), id + " gleicht " + clash);
            }
        }
        assertEquals(15, new HashSet<>(dyeByDiameter.values()).size(), "15 Größen, 15 Farbstoffe");
    }

    static List<String> grids(List<String> pattern, JsonObject key) {
        List<String> rows = new ArrayList<>(pattern);
        StringBuilder normal = new StringBuilder();
        StringBuilder mirrored = new StringBuilder();
        for (String row : rows) {
            StringBuilder a = new StringBuilder();
            for (char c : row.toCharArray()) {
                a.append(c == ' ' ? "_" : key.get(String.valueOf(c)).getAsString()).append(',');
            }
            StringBuilder b = new StringBuilder();
            for (int i = row.length() - 1; i >= 0; i--) {
                char c = row.charAt(i);
                b.append(c == ' ' ? "_" : key.get(String.valueOf(c)).getAsString()).append(',');
            }
            normal.append(a).append('/');
            mirrored.append(b).append('/');
        }
        return List.of(normal.toString(), mirrored.toString());
    }

    @Test
    public void languageFilesAreCompleteAndFollowTheHouseStyle() {
        Map<String, String> de = PropBenchTest.lang("de_de");
        Map<String, String> en = PropBenchTest.lang("en_us");
        assertEquals(de.keySet(), en.keySet(), "gleiche Schlüssel in de_de und en_us");
        List<String> problems = new ArrayList<>();
        for (String id : props()) {
            String help = "justmoreparts.help." + id;
            for (String key : List.of("part.justmoreparts." + id, help + ".summary", help + ".what.1", help + ".what.2", help + ".use.1")) {
                if (!de.containsKey(key)) {
                    problems.add("fehlt: " + key);
                }
            }
            for (Map.Entry<String, String> entry : de.entrySet()) {
                String k = entry.getKey();
                String v = entry.getValue();
                if (!k.startsWith(help + ".")) {
                    continue;
                }
                if ((k.contains(".what.") || k.contains(".use.")) && (v.length() > 48 || en.get(k).length() > 48)) {
                    problems.add(k + ": länger als 48 Zeichen");
                }
                if (v.startsWith("- ") || en.get(k).startsWith("- ")) {
                    problems.add(k + ": Minus statt „−“");
                }
                if (v.contains("\"") || en.get(k).contains("\"")) {
                    problems.add(k + ": Zoll als ″ schreiben");
                }
                if (en.get(k).contains("fibre") || en.get(k).contains("Li-ion")) {
                    problems.add(k + ": „fiber“/„Li-Ion“ schreiben");
                }
                for (String bad : List.of("ae", "oe", "ue")) {
                    for (String word : v.split("[^A-Za-zÄÖÜäöüß]+")) {
                        String low = word.toLowerCase();
                        if (low.contains(bad) && !low.contains("bue") && !low.contains("aue") && !low.contains("eue") && !low.equals("quer")) {
                            problems.add(k + ": „" + word + "“ – Umlaut statt " + bad + "?");
                        }
                    }
                }
            }
            for (Map<String, String> table : List.of(de, en)) {
                String name = table.get("part.justmoreparts." + id);
                String summary = table.get(help + ".summary");
                if (name == null || summary == null || !name.endsWith(")") || !summary.contains(" – ")) {
                    continue;
                }
                List<String> attrs = List.of(name.substring(name.indexOf('(') + 1, name.length() - 1).split(", "));
                String head = summary.substring(0, summary.indexOf(" – "));
                String wanted = id.endsWith("_duct") ? " " + attrs.get(attrs.size() - 1) : ", " + attrs.get(2);
                if (!head.endsWith(wanted)) {
                    problems.add(help + ".summary: Kurzkennung „" + head + "“ endet nicht auf „" + wanted.strip() + "“ wie der Name");
                }
            }
            String what1 = de.get(help + ".what.1");
            if (what1 != null && !what1.contains(" · ")) {
                problems.add(help + ".what.1 ohne Kennzahlen");
            }
            if (!de.getOrDefault(help + ".use.1", "").startsWith("Einsatz: ") || !en.getOrDefault(help + ".use.1", "").startsWith("Use: ")) {
                problems.add(help + ".use.1 beginnt nicht mit „Einsatz:“/„Use:“");
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    public void everyPropShipsItsModelsTexturesAndIcon() {
        List<String> missing = new ArrayList<>();
        JsonObject paintFile = resource("assets/justmoreparts/drone_paint.json");
        JsonObject paintModels = paintFile.getAsJsonObject("models");
        int reference = paintFile.getAsJsonObject("channels").get("prop").getAsInt();
        for (String id : props()) {
            for (String variant : VARIANTS) {
                for (String paint : List.of("", "_paint")) {
                    String name = id + variant + paint;
                    String modelPath = "assets/justmoreparts/models/part/prop/" + name + ".json";
                    if (!exists(modelPath) || !exists("assets/justmoreparts/items/drone_render/prop/" + name + ".json")) {
                        missing.add(name + ": Modell oder Render-Definition fehlt");
                        continue;
                    }
                    JsonObject model = resource(modelPath);
                    JsonArray size = model.getAsJsonArray("texture_size");
                    if (size == null || size.get(0).getAsInt() != 64 || size.get(1).getAsInt() != 64) {
                        missing.add(name + ": Textur nicht 64 × 64 wie die Propwash-Props");
                    }
                    for (Map.Entry<String, JsonElement> texture : model.getAsJsonObject("textures").entrySet()) {
                        JsonElement value = texture.getValue();
                        String sprite = value.isJsonObject() ? value.getAsJsonObject().get("sprite").getAsString() : value.getAsString();
                        if (sprite.startsWith("justmoreparts:") && !exists("assets/" + sprite.replace(":", "/textures/") + ".png")) {
                            missing.add(name + ": Textur " + sprite + " fehlt");
                        }
                    }
                    JsonObject display = model.getAsJsonObject("display");
                    for (String context : List.of("gui", "ground", "fixed", "on_shelf", "thirdperson_righthand", "firstperson_righthand", "head")) {
                        if (display == null || !display.has(context)) {
                            missing.add(name + ": Anzeige " + context + " fehlt");
                        } else if (display.getAsJsonObject(context).getAsJsonArray("scale").get(0).getAsDouble() > 4.0) {
                            missing.add(name + ": Anzeige " + context + " über Skala 4");
                        }
                    }
                }
            }
            JsonObject icon = resource("assets/justmoreparts/items/" + id + ".json");
            String text = icon.toString();
            if (!text.contains("propwash:prop_damage") || !text.contains("justmoreparts:part/prop/" + id + "_broken")) {
                missing.add(id + ": Symbol ohne Schadensstufen");
            }
            if (text.contains("propwash:part") || text.contains("part_base")) {
                missing.add(id + ": Symbol über den Sonder-Renderer statt über das eigene Modell");
            }
            JsonArray groups = paintModels.getAsJsonArray("prop/" + id);
            JsonArray tints = resource("assets/justmoreparts/items/drone_render/prop/" + id + "_paint.json").getAsJsonObject("model")
                    .getAsJsonArray("tints");
            if (groups == null || groups.size() != tints.size()) {
                missing.add(id + ": Tint-Ebenen passen nicht zu drone_paint.json");
                continue;
            }
            for (int i = 0; i < groups.size(); i++) {
                JsonObject group = groups.get(i).getAsJsonObject();
                int base = Integer.parseInt(group.get("base").getAsString().substring(1), 16);
                int tint = tints.get(i).getAsJsonObject().get("default").getAsInt();
                if (!group.get("channel").getAsString().equals("prop")) {
                    missing.add(id + ": Lack-Ebene " + i + " nicht im Kanal prop");
                }
                for (int shift : new int[]{16, 8, 0}) {
                    int expected = Math.min(255, (((base >> shift) & 0xFF) * 255 + reference / 2) / reference);
                    if (((tint >> shift) & 0xFF) != expected) {
                        missing.add(id + ": Lack-Standardfarbe " + i + " ergibt nicht die Grundfarbe");
                        break;
                    }
                }
            }
            int accent = PropBenchTest.prop(id).basics().visual().accentRgb().orElseThrow();
            int first = Integer.parseInt(groups.get(0).getAsJsonObject().get("base").getAsString().substring(1), 16);
            if (Set.of(id.split("_")).contains("pc") && first != accent) {
                missing.add(id + ": PC-Prop nicht in der Akzentfarbe");
            }
        }
        assertTrue(missing.isEmpty(), String.join("\n", missing));
    }

    @Test
    public void xClassPropsAreRarerAndFullCarbonIsRare() {
        PartFiles.registerAll();
        List<PropDefinition> own = PropModule.ownProps();
        assertEquals(38, own.size());
        int uncommon = 0;
        int rare = 0;
        for (PropDefinition prop : own) {
            Rarity rarity = PropModule.rarityOf(prop);
            if (prop.diameterMm() < PropModule.X_CLASS_MIN_MM) {
                assertEquals(Rarity.COMMON, rarity, prop.id().toString());
            } else if (rarity == Rarity.RARE) {
                rare++;
            } else {
                assertEquals(Rarity.UNCOMMON, rarity, prop.id().toString());
                uncommon++;
            }
        }
        assertEquals(2, rare, "Vollcarbon 10″ und 13″");
        assertEquals(3, uncommon, "Carbonfaser-Nylon 10–13″");
        assertFalse(own.isEmpty());
    }
}
