package de.lardtom.justmoreparts.power;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import de.lardtom.justmoreparts.bench.PartFiles;
import de.lardtom.propwash.build.BatteryFit;
import de.lardtom.propwash.build.BatteryType;
import de.lardtom.propwash.build.FrameType;
import de.lardtom.propwash.build.StackType;
import de.lardtom.propwash.drone.client.render.DroneLayout;
import de.lardtom.propwash.drone.client.render.DroneLayoutParser;
import de.lardtom.propwash.drone.client.render.DroneVisualScale;
import de.lardtom.propwash.drone.client.render.PartModel;
import de.lardtom.propwash.drone.client.render.PlugAnchors;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CovPowerLeadTest {
    static final Set<String> BATTERY_PLUG = Set.of(PlugAnchors.CONNECTOR, PlugAnchors.CONNECTOR_GRIP, PlugAnchors.BT_CONNECTOR,
            PlugAnchors.BALANCE_PLUG, PlugAnchors.WIRE);
    static final Map<String, String> FACES = new LinkedHashMap<>();
    static final Map<String, String> CLASSES = new LinkedHashMap<>();
    static final Map<Identifier, Optional<JsonObject>> MODELS = new HashMap<>();
    static final Map<PartModel, Checked> CHECKED = new HashMap<>();

    static {
        FACES.put("down", "y-");
        FACES.put("up", "y+");
        FACES.put("north", "z-");
        FACES.put("south", "z+");
        FACES.put("west", "x-");
        FACES.put("east", "x+");
        CLASSES.put("whoop_", "Whoop");
        CLASSES.put("tp_", "Toothpick");
        CLASSES.put("cw_", "Cinewhoop");
        CLASSES.put("fs_", "5″ Freestyle");
        CLASSES.put("race_", "5″ Freestyle");
        CLASSES.put("deadcat_", "Deadcat");
        CLASSES.put("hframe_", "H-Frame");
        CLASSES.put("lr_", "Long Range");
        CLASSES.put("xclass_", "X-Class");
    }

    record Face(String element, Vector3f[] corners) {
    }

    record Checked(List<String> problems, int hidden, int kept) {
    }

    record Fitted(FrameType frame, BatteryType battery, StackType stack, boolean below, PartModel batteryModel, PartModel mounted) {
        String name() {
            return this.frame.id() + " + " + this.battery.id() + " + " + this.stack.id() + (this.below ? " (unten)" : " (oben)");
        }
    }

    static String sizeClass(FrameType frame) {
        for (Map.Entry<String, String> entry : CLASSES.entrySet()) {
            if (frame.id().getPath().startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "?";
    }

    static boolean jmp(Identifier id) {
        return PartFiles.NAMESPACE.equals(id.getNamespace());
    }

    static boolean shipped(Identifier id) {
        return jmp(id) || "propwash".equals(id.getNamespace());
    }

    static @Nullable JsonObject resource(Identifier file) {
        String path = "assets/" + file.getNamespace() + "/" + file.getPath();
        try (InputStream in = CovPowerLeadTest.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static @Nullable Identifier referenced(JsonElement node) {
        if (node.isJsonObject()) {
            JsonObject object = node.getAsJsonObject();
            JsonElement type = object.get("type");
            JsonElement model = object.get("model");
            if (type != null && type.isJsonPrimitive() && "minecraft:model".equals(type.getAsString()) && model != null && model.isJsonPrimitive()) {
                return Identifier.tryParse(model.getAsString());
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                Identifier found = referenced(entry.getValue());
                if (found != null) {
                    return found;
                }
            }
        } else if (node.isJsonArray()) {
            for (JsonElement element : node.getAsJsonArray()) {
                Identifier found = referenced(element);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    static Optional<JsonObject> model(PartModel part) {
        return MODELS.computeIfAbsent(part.itemModelFile(), file -> {
            JsonObject item = resource(file);
            if (item == null) {
                return Optional.empty();
            }
            Identifier id = referenced(item);
            Identifier current = id == null ? part.modelFile() : id.withPath(path -> "models/" + path + ".json");
            for (int depth = 0; depth < 8 && current != null; depth++) {
                JsonObject json = resource(current);
                if (json == null) {
                    return Optional.empty();
                }
                if (json.has("elements")) {
                    return Optional.of(json);
                }
                JsonElement parent = json.get("parent");
                Identifier next = parent != null && parent.isJsonPrimitive() ? Identifier.tryParse(parent.getAsString()) : null;
                current = next == null ? null : next.withPath(path -> "models/" + path + ".json");
            }
            return Optional.empty();
        });
    }

    static float number(@Nullable JsonElement element) {
        return element instanceof JsonPrimitive primitive && primitive.isNumber() ? primitive.getAsFloat() : 0.0F;
    }

    static Vector3f vector(JsonElement element) {
        JsonArray array = element.getAsJsonArray();
        return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    static @Nullable Matrix3f rotation(@Nullable JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject rotation = element.getAsJsonObject();
        float x = 0.0F;
        float y = 0.0F;
        float z = 0.0F;
        if (rotation.get("axis") instanceof JsonPrimitive axis && axis.isString()) {
            float angle = number(rotation.get("angle"));
            switch (axis.getAsString()) {
                case "x" -> x = angle;
                case "y" -> y = angle;
                case "z" -> z = angle;
                default -> {
                    return null;
                }
            }
        } else {
            x = number(rotation.get("x"));
            y = number(rotation.get("y"));
            z = number(rotation.get("z"));
        }
        if (x == 0.0F && y == 0.0F && z == 0.0F) {
            return null;
        }
        return new Matrix3f().rotationZ((float) Math.toRadians(z)).rotateY((float) Math.toRadians(y)).rotateX((float) Math.toRadians(x));
    }

    static float millimetres(float modelUnits) {
        return (modelUnits - PlugAnchors.MODEL_ANCHOR) * DroneVisualScale.MILLIMETRES_PER_MODEL_UNIT;
    }

    static List<Face> faces(JsonObject model) {
        List<Face> out = new ArrayList<>();
        for (JsonElement entry : model.getAsJsonArray("elements")) {
            JsonObject element = entry.getAsJsonObject();
            if (!element.has("faces") || !element.get("faces").isJsonObject()) {
                continue;
            }
            String name = element.get("name") instanceof JsonPrimitive primitive && primitive.isString() ? primitive.getAsString() : "";
            Vector3f from = vector(element.get("from"));
            Vector3f to = vector(element.get("to"));
            Matrix3f matrix = rotation(element.get("rotation"));
            Vector3f origin = matrix == null ? null : vector(element.getAsJsonObject("rotation").get("origin"));
            Vector3f[] corners = new Vector3f[8];
            for (int i = 0; i < 8; i++) {
                Vector3f corner = new Vector3f((i & 1) == 0 ? from.x() : to.x(), (i & 2) == 0 ? from.y() : to.y(), (i & 4) == 0 ? from.z() : to.z());
                if (matrix != null) {
                    corner.sub(origin);
                    matrix.transform(corner);
                    corner.add(origin);
                }
                corners[i] = new Vector3f(millimetres(corner.x()), millimetres(corner.y()), millimetres(corner.z()));
            }
            for (String side : element.getAsJsonObject("faces").keySet()) {
                String code = FACES.get(side);
                if (code == null) {
                    continue;
                }
                int bit = code.charAt(0) == 'x' ? 1 : code.charAt(0) == 'y' ? 2 : 4;
                boolean high = code.charAt(1) == '+';
                List<Vector3f> face = new ArrayList<>(4);
                for (int i = 0; i < 8; i++) {
                    if (((i & bit) != 0) == high) {
                        face.add(corners[i]);
                    }
                }
                out.add(new Face(name + " " + side, face.toArray(Vector3f[]::new)));
            }
        }
        return out;
    }

    static boolean hidden(Face face, List<PlugAnchors.Box> mask) {
        for (PlugAnchors.Box box : mask) {
            boolean inside = true;
            for (Vector3f corner : face.corners()) {
                if (!box.contains(corner.x(), corner.y(), corner.z(), PlugAnchors.TOUCH_MM)) {
                    inside = false;
                    break;
                }
            }
            if (inside) {
                return true;
            }
        }
        return false;
    }

    static Checked masked(String what, JsonObject model, List<PlugAnchors.Box> mask, Set<String> hiddenNames) {
        List<String> problems = new ArrayList<>();
        int hidden = 0;
        int kept = 0;
        List<String> visible = new ArrayList<>();
        List<String> lost = new ArrayList<>();
        for (Face face : faces(model)) {
            boolean gone = hidden(face, mask);
            boolean wanted = hiddenNames.contains(face.element().substring(0, face.element().indexOf(' ')));
            if (gone) {
                hidden++;
            } else {
                kept++;
            }
            if (wanted && !gone) {
                visible.add(face.element());
            } else if (!wanted && gone) {
                lost.add(face.element());
            }
        }
        if (!visible.isEmpty()) {
            problems.add(what + ": " + visible.size() + " Flächen von Stecker oder Kabel bleiben sichtbar " + visible.subList(0, Math.min(6, visible.size())));
        }
        if (!lost.isEmpty()) {
            problems.add(what + ": " + lost.size() + " Flächen fehlen, die keine Stecker oder Kabel sind " + lost.subList(0, Math.min(6, lost.size())));
        }
        return new Checked(List.copyOf(problems), hidden, kept);
    }

    static Checked battery(PartModel part) {
        return CHECKED.computeIfAbsent(part, key -> {
            Optional<JsonObject> json = model(key);
            if (json.isEmpty()) {
                return new Checked(List.of(key.name() + ": Akku-Modell fehlt"), 0, 0);
            }
            List<PlugAnchors.Box> mask = PlugAnchors.batteryPlug(json.get());
            if (mask.isEmpty()) {
                return new Checked(List.of(key.name() + ": keine Buchse (connector oder bt_connector) – Stecker bliebe sichtbar"), 0, 0);
            }
            return masked(key.name(), json.get(), mask, BATTERY_PLUG);
        });
    }

    static Checked mounted(PartModel part) {
        return CHECKED.computeIfAbsent(part, key -> {
            Optional<JsonObject> json = model(key);
            if (json.isEmpty()) {
                return new Checked(List.of(key.name() + ": Stack _mounted fehlt – Akku wird nicht gesteckt gezeichnet"), 0, 0);
            }
            List<String> problems = new ArrayList<>();
            if (PlugAnchors.hasPlug(json.get())) {
                problems.add(key.name() + ": Stecker-Element im Stack _mounted");
            }
            if (PlugAnchors.count(json.get(), PlugAnchors.WIRE) != 0) {
                problems.add(key.name() + ": " + PlugAnchors.count(json.get(), PlugAnchors.WIRE) + " Elemente wire statt pigtail");
            }
            List<PlugAnchors.Box> mask = PlugAnchors.pigtails(json.get());
            if (mask.size() != 2) {
                problems.add(key.name() + ": nicht genau zwei pigtail – Propwash fällt auf den Stack mit Stecker zurück");
                return new Checked(List.copyOf(problems), 0, 0);
            }
            Checked faces = masked(key.name(), json.get(), mask, Set.of(PlugAnchors.PIGTAIL));
            problems.addAll(faces.problems());
            return new Checked(List.copyOf(problems), faces.hidden(), faces.kept());
        });
    }

    static Map<FrameType, DroneLayout> fileLayouts() {
        Map<FrameType, DroneLayout> out = new HashMap<>();
        for (String namespace : List.of("propwash", PartFiles.NAMESPACE)) {
            JsonObject json = resource(Identifier.fromNamespaceAndPath(namespace, "drone_layout.json"));
            if (json != null) {
                out.putAll(DroneLayoutParser.parse(json, namespace).layouts());
            }
        }
        return out;
    }

    static Fitted fit(FrameType frame, BatteryType battery, StackType stack, Map<FrameType, DroneLayout> layouts) {
        DroneLayout layout = layouts.getOrDefault(frame, DroneLayout.fromDefinition(frame));
        PartModel batteryModel = PartModel.of(battery);
        if (layout.batteryBelow()) {
            PartModel under = PartModel.batteryUnder(battery);
            if (model(under).isPresent()) {
                batteryModel = under;
            }
        }
        return new Fitted(frame, battery, stack, layout.batteryBelow(), batteryModel, PartModel.stackMounted(stack));
    }

    static List<Fitted> validJmpCombinations() {
        PartFiles.registerAll();
        Map<FrameType, DroneLayout> layouts = fileLayouts();
        List<Fitted> out = new ArrayList<>();
        for (FrameType frame : FrameType.values()) {
            for (BatteryType battery : BatteryType.values()) {
                if (!BatteryFit.check(frame, battery).allowed()) {
                    continue;
                }
                for (StackType stack : StackType.values()) {
                    if (!stack.fits(frame) || !(jmp(frame.id()) || jmp(battery.id()) || jmp(stack.id()))
                            || !(shipped(frame.id()) && shipped(battery.id()) && shipped(stack.id()))) {
                        continue;
                    }
                    out.add(fit(frame, battery, stack, layouts));
                }
            }
        }
        return out;
    }

    @Test
    public void everyValidJmpCombinationHidesPlugAndLeadsWithTheBatteryOnTopAndBelow() {
        List<Fitted> all = validJmpCombinations();
        List<String> failures = new ArrayList<>();
        Map<String, int[]> perClass = new TreeMap<>();
        Set<String> framesTop = new TreeSet<>();
        Set<String> framesBelow = new TreeSet<>();
        Set<PartModel> batteries = new TreeSet<>((a, b) -> a.name().compareTo(b.name()));
        Set<PartModel> stacks = new TreeSet<>((a, b) -> a.name().compareTo(b.name()));
        for (Fitted f : all) {
            List<String> wrong = new ArrayList<>(battery(f.batteryModel()).problems());
            wrong.addAll(mounted(f.mounted()).problems());
            if (!wrong.isEmpty()) {
                failures.add(f.name() + ": " + String.join("; ", wrong));
            }
            batteries.add(f.batteryModel());
            stacks.add(f.mounted());
            if (jmp(f.frame().id())) {
                perClass.computeIfAbsent(sizeClass(f.frame()), key -> new int[2])[f.below() ? 1 : 0]++;
                (f.below() ? framesBelow : framesTop).add(f.frame().id().getPath());
            }
        }
        int plugFaces = 0;
        int packFaces = 0;
        for (PartModel part : batteries) {
            plugFaces += battery(part).hidden();
            packFaces += battery(part).kept();
        }
        int pigtailFaces = 0;
        for (PartModel part : stacks) {
            pigtailFaces += mounted(part).hidden();
        }
        StringBuilder report = new StringBuilder();
        report.append(String.format(Locale.ROOT, "%d gültige Kombinationen mit JMP-Teilen, %d Akku-Modelle, %d Stacks _mounted%n", all.size(),
                batteries.size(), stacks.size()));
        perClass.forEach((name, counts) -> report.append(String.format(Locale.ROOT, "  %-13s Akku oben %4d, unten %4d%n", name, counts[0], counts[1])));
        report.append("  Rahmen mit Akku oben: ").append(framesTop).append('\n');
        report.append("  Rahmen mit Akku unten: ").append(framesBelow).append('\n');
        report.append(String.format(Locale.ROOT, "  ausgeblendet: %d Flächen von Akku-Steckern und Kabeln, %d Flächen von Pigtails; Akku-Rest %d Flächen sichtbar%n",
                plugFaces, pigtailFaces, packFaces));
        System.out.print(report);
        assertTrue(all.size() >= 200, "zu wenige Kombinationen: " + all.size());
        for (String name : new TreeSet<>(CLASSES.values())) {
            assertTrue(perClass.containsKey(name), "keine gültige Kombination für " + name);
        }
        assertFalse(framesTop.isEmpty(), "kein JMP-Rahmen mit Akku oben");
        assertFalse(framesBelow.isEmpty(), "kein JMP-Rahmen mit Akku unten");
        int frames = 0;
        for (FrameType frame : FrameType.values()) {
            if (jmp(frame.id())) {
                frames++;
                assertTrue(framesTop.contains(frame.id().getPath()) || framesBelow.contains(frame.id().getPath()), frame.id() + ": keine gültige Kombination");
            }
        }
        assertEquals(22, frames, "JMP-Rahmen");
        assertTrue(failures.isEmpty(), failures.size() + " von " + all.size() + " Kombinationen zeigen Stecker oder Kabel bzw. verlieren Flächen:\n"
                + String.join("\n", failures.subList(0, Math.min(40, failures.size()))));
    }
}
