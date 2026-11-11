package de.lardtom.justmoreparts.power;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PowerPlugModelsTest {
    private static final String MOUNTED = "_mounted";
    private static final String PAINT = "_paint";
    private static final String PIGTAIL = "pigtail";
    private static final String WIRE = "wire";
    private static final Set<String> PLUG = Set.of("connector", "connector_grip", "bt_connector");
    private static final Set<String> YELLOW = Set.of("XT30", "XT60", "XT90", "A30", "AS150");
    private static final Set<String> WHITE = Set.of("BT2.0", "BT3.0", "PH2.0");
    private static final String TOOL = " (python3 tools/connector_models.py)";

    private static Path assets() {
        return PowerFiles.resources().resolve("assets/" + PowerFiles.NS);
    }

    private static List<String> names(JsonObject model) {
        List<String> out = new ArrayList<>();
        for (JsonElement e : model.getAsJsonArray("elements")) {
            JsonObject element = e.getAsJsonObject();
            out.add(element.has("name") ? element.get("name").getAsString() : "");
        }
        return out;
    }

    private static boolean plugged(JsonObject model) {
        return names(model).stream().anyMatch(PLUG::contains);
    }

    private static double mm(JsonArray point, int axis) {
        return (point.get(axis).getAsDouble() - 8.0) * 8.0;
    }

    @Test
    public void everyStackWithPlugHasMountedVariantsAfterTheContract() {
        List<String> errors = new ArrayList<>();
        Path assets = assets();
        JsonObject paint = PowerFiles.json(assets.resolve("drone_paint.json")).getAsJsonObject("models");
        int mounted = 0;
        for (String id : PowerFiles.ids("stack")) {
            JsonObject base = PowerFiles.json(assets.resolve("models/part/stack/" + id + ".json"));
            if (!plugged(base)) {
                continue;
            }
            for (String variant : List.of(id, id + PAINT)) {
                String target = variant.endsWith(PAINT) ? id + MOUNTED + PAINT : id + MOUNTED;
                Path modelFile = assets.resolve("models/part/stack/" + target + ".json");
                Path renderFile = assets.resolve("items/drone_render/stack/" + target + ".json");
                if (!Files.exists(modelFile) || !Files.exists(renderFile)) {
                    errors.add(target + ": Modell oder Render-Definition fehlt" + TOOL);
                    continue;
                }
                mounted++;
                JsonObject source = PowerFiles.json(assets.resolve("models/part/stack/" + variant + ".json"));
                JsonObject model = PowerFiles.json(modelFile);
                for (String key : List.of("textures", "texture_size", "display", "gui_light")) {
                    if (!String.valueOf(source.get(key)).equals(String.valueOf(model.get(key)))) {
                        errors.add(target + ": " + key + " weicht vom Grundmodell ab");
                    }
                }
                List<JsonObject> kept = new ArrayList<>();
                for (JsonElement e : source.getAsJsonArray("elements")) {
                    JsonObject element = e.getAsJsonObject();
                    if (!PLUG.contains(element.has("name") ? element.get("name").getAsString() : "")) {
                        kept.add(element);
                    }
                }
                JsonArray elements = model.getAsJsonArray("elements");
                if (kept.size() != elements.size()) {
                    errors.add(target + ": " + elements.size() + " Elemente, Grundmodell ohne Stecker hat " + kept.size());
                    continue;
                }
                for (int i = 0; i < kept.size(); i++) {
                    JsonObject want = kept.get(i).deepCopy();
                    if (want.has("name") && WIRE.equals(want.get("name").getAsString())) {
                        want.addProperty("name", PIGTAIL);
                    }
                    if (!want.equals(elements.get(i))) {
                        errors.add(target + ": Element " + i + " (" + want.get("name") + ") weicht vom Grundmodell ab");
                        break;
                    }
                }
                List<String> got = names(model);
                long pigtails = got.stream().filter(PIGTAIL::equals).count();
                if (pigtails != 2 || got.contains(WIRE) || got.stream().anyMatch(PLUG::contains)) {
                    errors.add(target + ": " + pigtails + " Elemente pigtail, Stecker oder wire übrig");
                }
                List<double[]> ends = new ArrayList<>();
                for (JsonElement e : elements) {
                    JsonObject element = e.getAsJsonObject();
                    if (element.has("name") && PIGTAIL.equals(element.get("name").getAsString())) {
                        JsonArray from = element.getAsJsonArray("from");
                        JsonArray to = element.getAsJsonArray("to");
                        double d = Math.min(Math.abs(mm(to, 0) - mm(from, 0)), Math.abs(mm(to, 1) - mm(from, 1)));
                        ends.add(new double[]{(mm(from, 0) + mm(to, 0)) * 0.5, Math.max(mm(from, 2), mm(to, 2)), d});
                    }
                }
                if (ends.size() == 2 && (ends.get(0)[2] <= 0.0 || ends.get(1)[2] <= 0.0 || ends.get(0)[0] == ends.get(1)[0]
                        || ends.get(0)[1] <= 0.0 || ends.get(1)[1] <= 0.0)) {
                    errors.add(target + ": Pigtail-Enden nicht nebeneinander hinter der Stack-Mitte oder ohne Durchmesser");
                }
                JsonObject render = PowerFiles.json(renderFile).getAsJsonObject("model");
                JsonObject sourceRender = PowerFiles.json(assets.resolve("items/drone_render/stack/" + variant + ".json")).getAsJsonObject("model");
                if (!(PowerFiles.NS + ":part/stack/" + target).equals(render.get("model").getAsString())) {
                    errors.add(target + ": Render-Definition zeigt auf " + render.get("model"));
                }
                if (!String.valueOf(sourceRender.get("tints")).equals(String.valueOf(render.get("tints")))) {
                    errors.add(target + ": Lack-Tönung anders als beim Grundmodell");
                }
                if (Files.exists(assets.resolve("items/" + target + ".json"))) {
                    errors.add(target + ": darf kein Inventar-Item haben");
                }
            }
            JsonElement groups = paint.get("stack/" + id + MOUNTED);
            if (groups == null || !groups.equals(paint.get("stack/" + id))) {
                errors.add(id + MOUNTED + ": Lack-Gruppen in drone_paint.json fehlen oder weichen vom Grundmodell ab" + TOOL);
            }
        }
        assertTrue(errors.isEmpty(), String.join("\n", errors));
        assertEquals(2 * PowerFiles.ids("stack").size(), mounted, "jeder JMP-Stack hat einen Stecker und damit _mounted und _mounted_paint");
    }

    @Test
    public void mountedFilesBelongToStacksWithPlug() throws IOException {
        Set<String> allowed = new TreeSet<>();
        for (String id : PowerFiles.ids("stack")) {
            if (plugged(PowerFiles.json(assets().resolve("models/part/stack/" + id + ".json")))) {
                allowed.add(id + MOUNTED);
                allowed.add(id + MOUNTED + PAINT);
            }
        }
        List<String> errors = new ArrayList<>();
        for (String folder : List.of("models/part/stack", "items/drone_render/stack", "models/part/battery", "items/drone_render/battery", "items")) {
            try (Stream<Path> files = Files.list(assets().resolve(folder))) {
                for (Path file : files.filter(p -> p.getFileName().toString().contains(MOUNTED)).toList()) {
                    String stem = file.getFileName().toString().replace(".json", "");
                    if (!folder.endsWith("stack") || !allowed.contains(stem)) {
                        errors.add(folder + "/" + file.getFileName() + ": gehört zu keinem Stack mit Stecker" + TOOL);
                    }
                }
            }
        }
        assertTrue(errors.isEmpty(), String.join("\n", errors));
    }

    @Test
    public void everyBatteryModelHasASocketMatchingItsConnector() throws IOException {
        List<String> errors = new ArrayList<>();
        int sockets = 0;
        List<Path> models;
        try (Stream<Path> files = Files.list(assets().resolve("models/part/battery"))) {
            models = files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
        }
        for (Path file : models) {
            String key = file.getFileName().toString().replace(".json", "");
            String id = key.replace(PAINT, "").replace("_under", "");
            double[] lo = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
            double[] hi = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
            boolean bt = false;
            boolean found = false;
            for (JsonElement e : PowerFiles.json(file).getAsJsonArray("elements")) {
                JsonObject element = e.getAsJsonObject();
                String name = element.has("name") ? element.get("name").getAsString() : "";
                if (!name.equals("connector") && !name.equals("bt_connector")) {
                    continue;
                }
                if (element.has("rotation")) {
                    errors.add(key + ": Buchse gedreht");
                }
                found = true;
                bt |= name.equals("bt_connector");
                for (int axis = 0; axis < 3; axis++) {
                    double a = mm(element.getAsJsonArray("from"), axis);
                    double b = mm(element.getAsJsonArray("to"), axis);
                    lo[axis] = Math.min(lo[axis], Math.min(a, b));
                    hi[axis] = Math.max(hi[axis], Math.max(a, b));
                }
            }
            if (!found || hi[0] <= lo[0] || hi[1] <= lo[1] || hi[2] <= lo[2]) {
                errors.add(key + ": keine Buchse (Element connector oder bt_connector)");
                continue;
            }
            sockets++;
            JsonObject definition = PowerFiles.part("battery", id);
            String connector = definition.has("connector") ? definition.get("connector").getAsString()
                    .replace(" ", "").replace("-", "").toUpperCase(Locale.ROOT) : "";
            if (!YELLOW.contains(connector) && !WHITE.contains(connector)) {
                errors.add(key + ": Steckerart „" + connector + "“ ohne Farbe im Vertrag");
            } else if (WHITE.contains(connector) != bt) {
                errors.add(key + ": Steckerart " + connector + " passt nicht zur Buchse " + (bt ? "bt_connector" : "connector"));
            }
        }
        assertTrue(errors.isEmpty(), String.join("\n", errors));
        assertEquals(models.size(), sockets, "jedes Akku-Modell (auch _paint und _under) hat eine Buchse");
    }
}
