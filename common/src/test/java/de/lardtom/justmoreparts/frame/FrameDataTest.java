package de.lardtom.justmoreparts.frame;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.lardtom.justmoreparts.bench.Bench;
import de.lardtom.justmoreparts.bench.PartFiles;
import de.lardtom.justmoreparts.frame.client.FrameShapesScene;
import de.lardtom.justmoreparts.preset.JmpPresets;
import de.lardtom.justmoreparts.video.VideoModule;
import de.lardtom.propwash.api.build.BuildCheck;
import de.lardtom.propwash.api.build.PaintSpec;
import de.lardtom.propwash.guide.scene.Scene;
import de.lardtom.propwash.guide.scene.SceneStep;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FrameDataTest {
    private static final String NS = "justmoreparts";
    private static final double MIN_LENS_ABOVE_DUCTS_MM = 3.0;
    private static final double MODEL_UNIT_MM = 8.0;

    @Test
    public void moduleListsMatchThePartFiles() {
        assertEquals(new TreeSet<>(FrameModule.FRAMES), stems(resources().resolve("data/justmoreparts/propwash_parts/frame")));
        assertEquals(new TreeSet<>(VideoModule.VIDEOS), stems(resources().resolve("data/justmoreparts/propwash_parts/video")));
        assertTrue(FrameModule.FRAMES.containsAll(FrameModule.X_CLASS));
    }

    @Test
    public void everyPartHasNamesAndShortHelpInBothLanguages() {
        List<String> problems = new ArrayList<>();
        Map<String, JsonObject> lang = new HashMap<>();
        for (String code : List.of("de_de", "en_us")) {
            JsonObject merged = new JsonObject();
            for (String module : List.of("frame", "video", "preset")) {
                read(project().resolve("common/src/lang/" + module + "/" + code + ".json")).entrySet().forEach(e -> merged.add(e.getKey(), e.getValue()));
            }
            lang.put(code, merged);
        }
        List<String> parts = new ArrayList<>(FrameModule.FRAMES);
        parts.addAll(VideoModule.VIDEOS);
        for (String path : parts) {
            Map<String, int[]> counts = new HashMap<>();
            for (Map.Entry<String, JsonObject> e : lang.entrySet()) {
                JsonObject t = e.getValue();
                require(t, "part." + NS + "." + path, e.getKey(), problems);
                require(t, NS + ".help." + path + ".summary", e.getKey(), problems);
                if (FrameModule.FRAMES.contains(path)) {
                    require(t, NS + ".frame_short." + path, e.getKey(), problems);
                }
                int[] n = new int[2];
                for (int i = 1; i <= 8; i++) {
                    for (int k = 0; k < 2; k++) {
                        String key = NS + ".help." + path + "." + (k == 0 ? "what." : "use.") + i;
                        if (t.has(key)) {
                            n[k] = i;
                            String line = t.get(key).getAsString();
                            if (line.length() > 48) {
                                problems.add(e.getKey() + " " + key + ": " + line.length() + " Zeichen");
                            }
                        }
                    }
                }
                if (n[0] < 2 || n[1] < 1) {
                    problems.add(e.getKey() + " " + path + ": zu wenig Hilfezeilen");
                }
                counts.put(e.getKey(), n);
                String what2 = t.has(NS + ".help." + path + ".what.2") ? t.get(NS + ".help." + path + ".what.2").getAsString() : "";
                if (!what2.startsWith("+ ") && !what2.startsWith("− ")) {
                    problems.add(e.getKey() + " " + path + ": what.2 ohne „+ “/„− “");
                }
            }
            if (counts.get("de_de")[0] != counts.get("en_us")[0] || counts.get("de_de")[1] != counts.get("en_us")[1]) {
                problems.add(path + ": de/en mit unterschiedlicher Zeilenzahl");
            }
        }
        for (JmpPresets.Entry entry : JmpPresets.ALL) {
            for (Map.Entry<String, JsonObject> e : lang.entrySet()) {
                require(e.getValue(), entry.translationKey(), e.getKey(), problems);
            }
        }
        String prefix = NS + ".guide.scene.frames.";
        for (Map.Entry<String, JsonObject> e : lang.entrySet()) {
            limit(e.getValue(), prefix + "title", 38, e.getKey(), problems);
            limit(e.getValue(), prefix + "description", 64, e.getKey(), problems);
            for (String step : FrameShapesScene.STEPS) {
                limit(e.getValue(), prefix + "step." + step + ".title", 38, e.getKey(), problems);
                limit(e.getValue(), prefix + "step." + step + ".text", 150, e.getKey(), problems);
            }
            for (String callout : FrameShapesScene.CALLOUTS) {
                limit(e.getValue(), prefix + "callout." + callout, 90, e.getKey(), problems);
            }
        }
        for (Map.Entry<String, JsonElement> e : lang.get("de_de").entrySet()) {
            String value = e.getValue().getAsString().toLowerCase();
            for (String bad : List.of("ae", "oe", "ue")) {
                for (String word : value.split("[^a-zäöüß]+")) {
                    if (word.contains(bad) && !List.of("true", "aero", "blue").contains(word)) {
                        problems.add("de_de " + e.getKey() + ": „" + word + "“ – Umlaut?");
                    }
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    public void recipesAreUniqueAndBelongToTheParts() {
        Path recipes = resources().resolve("data/justmoreparts/recipe");
        Set<String> signatures = new HashSet<>();
        List<String> parts = new ArrayList<>(FrameModule.FRAMES);
        parts.addAll(VideoModule.VIDEOS);
        for (String path : parts) {
            JsonObject recipe = read(recipes.resolve(path + ".json"));
            assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString(), path);
            assertEquals("equipment", recipe.get("category").getAsString(), path);
            assertEquals(FrameModule.FRAMES.contains(path) ? "propwash_frame" : "propwash_video", recipe.get("group").getAsString(), path);
            assertEquals(NS + ":" + path, recipe.getAsJsonObject("result").get("id").getAsString());
            JsonObject key = recipe.getAsJsonObject("key");
            StringBuilder signature = new StringBuilder();
            for (JsonElement row : recipe.getAsJsonArray("pattern")) {
                for (char c : row.getAsString().toCharArray()) {
                    signature.append(c == ' ' ? "_" : key.get(String.valueOf(c)).getAsString()).append(',');
                }
                signature.append('|');
            }
            assertTrue(signatures.add(signature.toString()), "Rezept doppelt: " + path);
            assertTrue(Files.exists(resources().resolve("data/justmoreparts/advancement/recipes/" + (FrameModule.FRAMES.contains(path) ? "frame" : "video")
                    + "/" + path + ".json")), "Fortschritt fehlt: " + path);
        }
    }

    @Test
    public void lootTablesAndNeoForgeModifiersMatchFrameLoot() {
        Set<String> own = new HashSet<>();
        FrameModule.FRAMES.forEach(p -> own.add(NS + ":" + p));
        VideoModule.VIDEOS.forEach(p -> own.add(NS + ":" + p));
        Set<String> dropped = new HashSet<>();
        for (var table : FrameLoot.tables()) {
            JsonObject json = read(resources().resolve("data/" + table.identifier().getNamespace() + "/loot_table/" + table.identifier().getPath() + ".json"));
            assertEquals("minecraft:chest", json.get("type").getAsString());
            int empty = 0;
            int items = 0;
            for (JsonElement pool : json.getAsJsonArray("pools")) {
                for (JsonElement entry : pool.getAsJsonObject().getAsJsonArray("entries")) {
                    JsonObject e = entry.getAsJsonObject();
                    if (e.get("type").getAsString().equals("minecraft:empty")) {
                        empty += e.get("weight").getAsInt();
                    } else {
                        assertTrue(own.contains(e.get("name").getAsString()), table.identifier() + ": " + e.get("name"));
                        items += e.get("weight").getAsInt();
                        dropped.add(e.get("name").getAsString());
                    }
                }
            }
            assertTrue(empty > items, table.identifier() + ": Rahmen-Beute soll seltener sein als leer");
        }
        assertEquals(own, dropped, "jedes Teil liegt in mindestens einer Truhe");
    }

    @Test
    public void generatedModelsAreCompleteAndOnlyForOwnParts() {
        Path assets = resources().resolve("assets/" + NS);
        JsonObject paint = read(assets.resolve("drone_paint.json")).getAsJsonObject("models");
        Set<String> modelled = new TreeSet<>();
        for (String category : List.of("frame", "video")) {
            for (String stem : stems(assets.resolve("items/drone_render/" + category))) {
                String part = stem.endsWith("_paint") ? stem.substring(0, stem.length() - 6) : stem;
                assertTrue(Files.exists(resources().resolve("data/" + NS + "/propwash_parts/" + category + "/" + part + ".json")), stem);
                JsonObject model = read(assets.resolve("items/drone_render/" + category + "/" + stem + ".json")).getAsJsonObject("model");
                assertEquals(NS + ":part/" + category + "/" + stem, model.get("model").getAsString());
                JsonObject file = read(assets.resolve("models/part/" + category + "/" + stem + ".json"));
                String texture = file.getAsJsonObject("textures").get("0").getAsString();
                assertTrue(Files.exists(assets.resolve("textures/" + texture.substring(texture.indexOf(':') + 1) + ".png")), texture);
                if (stem.endsWith("_paint")) {
                    JsonArray layers = paint.getAsJsonArray(category + "/" + part);
                    assertEquals(layers.size(), model.getAsJsonArray("tints").size(), stem);
                } else {
                    modelled.add(part);
                }
            }
        }
        Set<String> expected = new TreeSet<>(FrameModule.FRAMES);
        expected.addAll(VideoModule.VIDEOS);
        assertEquals(expected, modelled);
    }

    @Test
    public void xClassDroneModelsAreScaledBackToRealSize() {
        Path assets = resources().resolve("assets/" + NS);
        for (String x : FrameModule.X_CLASS) {
            JsonObject render = read(assets.resolve("items/drone_render/frame/" + x + ".json")).getAsJsonObject("model");
            JsonObject transformation = render.getAsJsonObject("transformation");
            assertTrue(transformation != null, x + " ohne transformation");
            double k = transformation.getAsJsonArray("scale").get(0).getAsDouble();
            assertTrue(k > 1.0 && k < 4.0, x + " Skala " + k);
            for (int i = 0; i < 3; i++) {
                assertEquals(k, transformation.getAsJsonArray("scale").get(i).getAsDouble(), 1e-9, x);
                assertEquals(0.5 - 0.5 * k, transformation.getAsJsonArray("translation").get(i).getAsDouble(), 1e-4, x);
            }
            JsonObject paintRender = read(assets.resolve("items/drone_render/frame/" + x + "_paint.json")).getAsJsonObject("model");
            assertEquals(transformation, paintRender.getAsJsonObject("transformation"), x + "_paint");
            double reach = 0.0;
            for (String stem : List.of(x, x + "_paint")) {
                for (JsonElement element : read(assets.resolve("models/part/frame/" + stem + ".json")).getAsJsonArray("elements")) {
                    for (String key : List.of("from", "to")) {
                        for (JsonElement value : element.getAsJsonObject().getAsJsonArray(key)) {
                            double v = value.getAsDouble();
                            assertTrue(v >= -16.0 && v <= 32.0, stem + " außerhalb der Modellgrenzen: " + v);
                        }
                    }
                    JsonObject cuboid = element.getAsJsonObject();
                    if (cuboid.get("name") != null && "motor_pad".equals(cuboid.get("name").getAsString()) && !cuboid.has("rotation")) {
                        double x0 = cuboid.getAsJsonArray("from").get(0).getAsDouble();
                        double x1 = cuboid.getAsJsonArray("to").get(0).getAsDouble();
                        reach = Math.max(reach, Math.abs((x0 + x1) / 2.0 - 8.0) * k * 8.0);
                    }
                }
            }
            JsonObject definition = read(resources().resolve("data/" + NS + "/propwash_parts/frame/" + x + ".json"));
            double expected = definition.has("arms") ? Math.abs(definition.getAsJsonObject("arms").getAsJsonObject("front_right")
                    .getAsJsonArray("pos").get(0).getAsDouble()) : definition.get("wheelbase_mm").getAsDouble() / (2.0 * Math.sqrt(2.0));
            assertEquals(expected, reach, 2.0, x + " Motorpad nicht an der echten Motorposition");
        }
        for (String stem : stems(assets.resolve("items/drone_render/frame"))) {
            String part = stem.endsWith("_paint") ? stem.substring(0, stem.length() - 6) : stem;
            if (!FrameModule.X_CLASS.contains(part)) {
                JsonObject model = read(assets.resolve("items/drone_render/frame/" + stem + ".json")).getAsJsonObject("model");
                assertFalse(model.has("transformation"), stem + " passt in die Modellgrenzen und braucht keine transformation");
            }
        }
    }

    @Test
    public void presetsAreBuildableAndPainted() {
        PartFiles.registerAll();
        Set<String> seen = new LinkedHashSet<>();
        for (JmpPresets.Entry entry : JmpPresets.ALL) {
            assertTrue(seen.add(entry.path()), entry.path());
            BuildCheck check = Bench.check(entry.build());
            assertTrue(check.known() && check.buildable(), entry.path() + " " + check.unknownParts() + check.problems());
            for (String slot : entry.paint().colors().keySet()) {
                assertTrue(PaintSpec.SLOTS.contains(slot), slot);
            }
            assertTrue(entry.paint().color("frame").isPresent() && entry.paint().color("motors").isPresent(), entry.path());
        }
        assertEquals(22, seen.size());
        assertEquals(JmpPresets.SizeClass.values().length, JmpPresets.ALL.stream().map(JmpPresets.Entry::sizeClass).distinct().count());
    }

    @Test
    public void flightSchoolSceneBuildsWithAllSteps() {
        PartFiles.registerAll();
        Scene scene = FrameShapesScene.create();
        assertEquals(FrameShapesScene.ID, scene.id());
        List<String> names = scene.steps().stream().map(SceneStep::name).toList();
        assertEquals(FrameShapesScene.STEPS, names);
        for (FrameShapesScene.Actor actor : FrameShapesScene.actors()) {
            BuildCheck check = Bench.check(actor.build());
            assertTrue(check.known() && check.buildable(), actor.name() + " " + check.problems());
        }
    }

    @Test
    public void ductFramesCarryTheCameraAboveTheDucts() {
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (String frame : FrameModule.FRAMES) {
            JsonObject data = read(resources().resolve("data/justmoreparts/propwash_parts/frame/" + frame + ".json"));
            if (!data.has("duct")) {
                continue;
            }
            double lens = data.getAsJsonObject("camera").getAsJsonArray("pos").get(1).getAsDouble();
            double top = ductTop(read(resources().resolve("assets/justmoreparts/models/part/frame/" + frame + ".json")));
            checked++;
            if (!(lens >= top + MIN_LENS_ABOVE_DUCTS_MM)) {
                problems.add(frame + ": Linse " + lens + " mm über dem Rahmen, Oberkante der Ducts " + top + " mm");
            }
        }
        assertTrue(checked >= 5, "nur " + checked + " Duct-Rahmen gefunden");
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    private static double ductTop(JsonObject model) {
        double top = Double.NEGATIVE_INFINITY;
        for (JsonElement entry : model.getAsJsonArray("elements")) {
            JsonObject element = entry.getAsJsonObject();
            String name = element.has("name") ? element.get("name").getAsString() : "";
            if (!name.equals("duct") && !name.equals("duct_lip") && !name.equals("duct_rim")) {
                continue;
            }
            double[] from = vector(element.getAsJsonArray("from"));
            double[] to = vector(element.getAsJsonArray("to"));
            double[] origin = {8.0, 8.0, 8.0};
            double[] angles = {0.0, 0.0, 0.0};
            if (element.has("rotation")) {
                JsonObject rotation = element.getAsJsonObject("rotation");
                origin = vector(rotation.getAsJsonArray("origin"));
                if (rotation.has("axis")) {
                    angles["xyz".indexOf(rotation.get("axis").getAsString())] = rotation.get("angle").getAsDouble();
                } else {
                    for (int axis = 0; axis < 3; axis++) {
                        String key = String.valueOf("xyz".charAt(axis));
                        angles[axis] = rotation.has(key) ? rotation.get(key).getAsDouble() : 0.0;
                    }
                }
            }
            for (int i = 0; i < 8; i++) {
                double[] p = {((i & 4) == 0 ? from[0] : to[0]) - origin[0], ((i & 2) == 0 ? from[1] : to[1]) - origin[1],
                        ((i & 1) == 0 ? from[2] : to[2]) - origin[2]};
                p = rotate(p, 0, angles[0]);
                p = rotate(p, 1, angles[1]);
                p = rotate(p, 2, angles[2]);
                top = Math.max(top, (p[1] + origin[1] - 8.0) * MODEL_UNIT_MM);
            }
        }
        return top;
    }

    private static double[] rotate(double[] p, int axis, double degrees) {
        if (degrees == 0.0) {
            return p;
        }
        double c = Math.cos(Math.toRadians(degrees));
        double s = Math.sin(Math.toRadians(degrees));
        return switch (axis) {
            case 0 -> new double[]{p[0], p[1] * c - p[2] * s, p[1] * s + p[2] * c};
            case 1 -> new double[]{p[0] * c + p[2] * s, p[1], -p[0] * s + p[2] * c};
            default -> new double[]{p[0] * c - p[1] * s, p[0] * s + p[1] * c, p[2]};
        };
    }

    private static double[] vector(JsonArray array) {
        return new double[]{array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble()};
    }

    private static void require(JsonObject table, String key, String language, List<String> problems) {
        if (!table.has(key) || table.get(key).getAsString().isBlank()) {
            problems.add(language + ": " + key + " fehlt");
        }
    }

    private static void limit(JsonObject table, String key, int max, String language, List<String> problems) {
        if (!table.has(key)) {
            problems.add(language + ": " + key + " fehlt");
        } else if (table.get(key).getAsString().length() > max) {
            problems.add(language + ": " + key + " länger als " + max);
        }
    }

    static Path resources() {
        return PartFiles.root().getParent().getParent().getParent();
    }

    static Path project() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("settings.gradle"))) {
            dir = dir.getParent();
        }
        return dir;
    }

    static JsonObject read(Path file) {
        try {
            return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Set<String> stems(Path folder) {
        try (Stream<Path> files = Files.list(folder)) {
            Set<String> out = new TreeSet<>();
            files.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                String name = p.getFileName().toString();
                out.add(name.substring(0, name.length() - 5));
            });
            return out;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
