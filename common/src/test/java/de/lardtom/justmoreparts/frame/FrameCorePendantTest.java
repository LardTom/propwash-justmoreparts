package de.lardtom.justmoreparts.frame;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.lardtom.justmoreparts.bench.Bench;
import de.lardtom.justmoreparts.bench.PartFiles;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.BuildCheck;
import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.part.AccessoryDefinition;
import de.lardtom.propwash.api.part.AccessorySlot;
import de.lardtom.propwash.api.part.AnchorSpec;
import de.lardtom.propwash.api.part.FrameDefinition;
import de.lardtom.propwash.api.part.FrameRole;
import de.lardtom.propwash.api.part.Vec3Mm;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FrameCorePendantTest {
    private static final String NS = "justmoreparts";
    private static final String CORE = "propwash";
    private static final Identifier FRONT_BOX = Identifier.fromNamespaceAndPath("jmp_test", "front_box");
    private static final double MODEL_UNIT_MM = 8.0;
    private static final double MAX_NOSE_GAP_MM = 12.0;
    private static final double MIN_FRONT_AHEAD_OF_BOTTOM_MM = 8.0;
    private static final double EPS = 1e-6;

    @BeforeAll
    public static void register() {
        PartFiles.registerAll();
        if (PropwashApi.parts().get(AccessoryDefinition.class, FRONT_BOX).isEmpty()) {
            PropwashApi.parts().register(AccessoryDefinition.builder(FRONT_BOX).mass(150.0).slot(AccessorySlot.FRONT)
                    .size(62.0, 58.0, 64.0).frontalArea(0.0036).build());
        }
    }

    @Test
    public void everyFrameNamesACoreFrameAsItsPendant() {
        Map<String, Identifier> pendants = pendants();
        assertEquals(FrameModule.FRAMES.size(), pendants.size(), "jeder Rahmen braucht genau einen Eintrag in der Teile-Tabelle");
        for (String frame : FrameModule.FRAMES) {
            Identifier pendant = pendants.get(frame);
            assertTrue(pendant != null, frame + " ohne Ersatz in der Teile-Tabelle");
            assertEquals(CORE, pendant.getNamespace(), frame + ": Ersatz " + pendant + " ist kein Propwash-Rahmen");
            assertTrue(PropwashApi.parts().get(FrameDefinition.class, pendant).isPresent(), frame + ": Ersatz " + pendant + " unbekannt");
        }
    }

    @Test
    public void everyFrameTakesTheAccessoriesOfItsCorePendant() {
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (Map.Entry<String, Identifier> entry : pendants().entrySet()) {
            FrameDefinition jmp = frame(Bench.id(entry.getKey()));
            FrameDefinition core = frame(entry.getValue());
            for (AccessorySlot slot : core.accessoryMounts()) {
                checked++;
                if (!jmp.accessoryMounts().contains(slot)) {
                    problems.add(entry.getKey() + ": keine Halterung " + slot.key() + " (" + core.id() + " hat sie)");
                }
                Identifier accessory = accessory(slot);
                if (!mountProblem(core.id(), slot, accessory) && mountProblem(jmp.id(), slot, accessory)) {
                    problems.add(entry.getKey() + ": " + accessory + " passt nicht (" + slot.key() + "), an " + core.id() + " schon");
                }
            }
        }
        assertTrue(checked >= 50, "nur " + checked + " Halterungen geprüft");
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    public void batteryLiesWhereItLiesOnTheCorePendant() {
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, Identifier> entry : pendants().entrySet()) {
            FrameDefinition jmp = frame(Bench.id(entry.getKey()));
            FrameDefinition core = frame(entry.getValue());
            boolean expected = jmp.role() == core.role() ? core.geometry().batteryOnTop() : batteryOnTopFor(jmp.role());
            if (jmp.geometry().batteryOnTop() != expected) {
                problems.add(entry.getKey() + ": Akku " + (jmp.geometry().batteryOnTop() ? "oben" : "unten") + ", erwartet "
                        + (expected ? "oben" : "unten") + " wie " + core.id());
            }
            if (core.geometry().batteryOnTop() != batteryOnTopFor(core.role())) {
                problems.add(core.id() + ": Akku-Lage passt nicht zur Rolle " + core.role().key());
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    public void everyMountHasItsOwnAnchorShapedLikeOnTheCorePendant() {
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, Identifier> entry : pendants().entrySet()) {
            FrameDefinition jmp = frame(Bench.id(entry.getKey()));
            FrameDefinition core = frame(entry.getValue());
            Map<AccessorySlot, AnchorSpec> anchors = jmp.geometry().accessoryAnchors();
            for (AccessorySlot slot : jmp.accessoryMounts()) {
                AnchorSpec anchor = anchors.get(slot);
                if (anchor == null) {
                    problems.add(entry.getKey() + ": Halterung " + slot.key() + " ohne eigenen Anker (Propwash rechnet sonst mit der Ersatzformel)");
                    continue;
                }
                Optional<AnchorSpec> reference = Optional.ofNullable(core.geometry().accessoryAnchors().get(slot));
                if (slot == AccessorySlot.FRONT && reference.isPresent()) {
                    if (Math.abs(anchor.tiltDeg() - reference.get().tiltDeg()) > EPS || Math.abs(anchor.lengthMm() - reference.get().lengthMm()) > EPS) {
                        problems.add(entry.getKey() + ": Anker vorne " + anchor.tiltDeg() + "°/" + anchor.lengthMm() + " mm, " + core.id() + " "
                                + reference.get().tiltDeg() + "°/" + reference.get().lengthMm() + " mm");
                    }
                }
                if (slot == AccessorySlot.BOTTOM) {
                    if (!(anchor.lengthMm() > 0.0)) {
                        problems.add(entry.getKey() + ": Anker unten ohne Streifenlänge");
                    }
                    if (jmp.role() == core.role() && reference.isPresent() && Math.abs(anchor.tiltDeg() - reference.get().tiltDeg()) > EPS) {
                        problems.add(entry.getKey() + ": Anker unten " + anchor.tiltDeg() + "°, " + core.id() + " " + reference.get().tiltDeg() + "°");
                    }
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    public void frontAnchorSitsAtTheNoseLikeOnTheCorePendant() {
        List<String> problems = new ArrayList<>();
        int expected = 0;
        int checked = 0;
        Map<Identifier, Boolean> coreChecked = new LinkedHashMap<>();
        for (Map.Entry<String, Identifier> entry : pendants().entrySet()) {
            FrameDefinition core = frame(entry.getValue());
            if (!core.accessoryMounts().contains(AccessorySlot.FRONT)) {
                continue;
            }
            expected++;
            coreChecked.putIfAbsent(core.id(), true);
            FrameDefinition jmp = frame(Bench.id(entry.getKey()));
            if (jmp.geometry().accessoryAnchors().containsKey(AccessorySlot.FRONT)) {
                checked++;
                front(jmp, jmpModel(entry.getKey()), problems);
            }
        }
        assertTrue(coreChecked.size() >= 3, "nur " + coreChecked.size() + " Propwash-Rahmen mit Halterung vorne");
        assertEquals(expected, checked, "Rahmen mit Anker vorne (so viele, wie ihr Propwash-Gegenstück vorne nimmt)");
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    private static void front(FrameDefinition frame, double[][] model, List<String> problems) {
        String name = frame.id().toString();
        AnchorSpec front = frame.geometry().accessoryAnchors().get(AccessorySlot.FRONT);
        if (front == null) {
            problems.add(name + ": Halterung vorne ohne Anker");
            return;
        }
        double nose = nose(model);
        double gap = nose - front.z();
        if (!(gap >= -EPS && gap <= MAX_NOSE_GAP_MM)) {
            problems.add(String.format(Locale.ROOT, "%s: Anker vorne bei z %.1f, Nase des Rahmens bei z %.1f (Abstand %.1f statt 0–%.0f mm)",
                    name, front.z(), nose, gap, MAX_NOSE_GAP_MM));
        }
        if (Math.abs(front.x()) > EPS) {
            problems.add(name + ": Anker vorne nicht mittig (x " + front.x() + ")");
        }
        AnchorSpec bottom = frame.geometry().accessoryAnchors().get(AccessorySlot.BOTTOM);
        double lowest = bottom == null ? Double.NEGATIVE_INFINITY : bottom.y();
        if (!(front.y() <= EPS && front.y() >= lowest - EPS)) {
            problems.add(String.format(Locale.ROOT, "%s: Anker vorne in der Höhe %.1f mm, erlaubt %.1f bis 0 (Bodenplatte)", name, front.y(), lowest));
        }
        if (bottom != null && !(front.z() <= bottom.z() - MIN_FRONT_AHEAD_OF_BOTTOM_MM + EPS)) {
            problems.add(String.format(Locale.ROOT, "%s: Anker vorne (z %.1f) zu dicht am Anker unten (z %.1f)", name, front.z(), bottom.z()));
        }
        Optional<Vec3Mm> camera = frame.geometry().cameraMm();
        if (camera.isPresent() && !(front.z() <= camera.get().z() + EPS)) {
            problems.add(String.format(Locale.ROOT, "%s: Anker vorne (z %.1f) hinter der FPV-Kamera (z %.1f)", name, front.z(), camera.get().z()));
        }
    }

    private static double nose(double[][] boxes) {
        double nose = Double.POSITIVE_INFINITY;
        for (double[] box : boxes) {
            if (box[0] <= EPS && box[3] >= -EPS) {
                nose = Math.min(nose, box[2]);
            }
        }
        assertTrue(Double.isFinite(nose), "Modell ohne Teile auf der Mittellinie");
        return nose;
    }

    private static boolean mountProblem(Identifier frame, AccessorySlot slot, Identifier accessory) {
        BuildSpec spec = Bench.spec(frame.toString(), "propwash:stack_30", "propwash:m2207_1750", "propwash:p51_3b", "propwash:digital_hd",
                "propwash:b6s_1300").withAccessory(slot, accessory);
        BuildCheck check = Bench.check(spec);
        assertTrue(check.known(), spec + " " + check.unknownParts());
        return check.problems().contains("accessory_mount");
    }

    private static Identifier accessory(AccessorySlot slot) {
        return switch (slot) {
            case TOP -> Identifier.fromNamespaceAndPath(CORE, "action_cam_light");
            case BOTTOM -> Identifier.fromNamespaceAndPath(CORE, "led_strip");
            default -> FRONT_BOX;
        };
    }

    private static boolean batteryOnTopFor(FrameRole role) {
        return switch (role) {
            case WHOOP, RACE -> false;
            default -> true;
        };
    }

    private static FrameDefinition frame(Identifier id) {
        return PropwashApi.parts().get(FrameDefinition.class, id).orElseThrow(() -> new IllegalStateException(id + " unbekannt"));
    }

    static Map<String, Identifier> pendants() {
        JsonObject table = FrameDataTest.read(FrameDataTest.resources().resolve("data/" + NS + "/propwash_share/table.json"));
        Map<String, Identifier> out = new LinkedHashMap<>();
        for (JsonElement element : table.getAsJsonObject("parts").getAsJsonArray("frame")) {
            JsonObject entry = element.getAsJsonObject();
            out.put(entry.get("id").getAsString(), Identifier.parse(entry.get("fallback").getAsString()));
        }
        return out;
    }

    private static double[][] jmpModel(String frame) {
        Path assets = FrameDataTest.resources().resolve("assets/" + NS);
        JsonObject model = FrameDataTest.read(assets.resolve("models/part/frame/" + frame + ".json"));
        JsonObject render = FrameDataTest.read(assets.resolve("items/drone_render/frame/" + frame + ".json")).getAsJsonObject("model");
        return boxes(model, scale(render));
    }

    private static double[][] coreModel(String frame) {
        JsonObject model = resource("assets/" + CORE + "/models/part/frame/" + frame + ".json");
        JsonObject render = resource("assets/" + CORE + "/items/drone_render/frame/" + frame + ".json").getAsJsonObject("model");
        return boxes(model, scale(render));
    }

    private static double scale(JsonObject render) {
        if (!render.has("transformation")) {
            return 1.0;
        }
        return render.getAsJsonObject("transformation").getAsJsonArray("scale").get(0).getAsDouble();
    }

    private static JsonObject resource(String path) {
        try (InputStream in = FrameCorePendantTest.class.getClassLoader().getResourceAsStream(path)) {
            assertTrue(in != null, path + " fehlt im Klassenpfad");
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static double[][] boxes(JsonObject model, double scale) {
        List<double[]> out = new ArrayList<>();
        for (JsonElement entry : model.getAsJsonArray("elements")) {
            JsonObject element = entry.getAsJsonObject();
            double[] from = vector(element.getAsJsonArray("from"));
            double[] to = vector(element.getAsJsonArray("to"));
            double[] origin = {8.0, 8.0, 8.0};
            Map<Integer, Double> angles = new HashMap<>();
            if (element.has("rotation")) {
                JsonObject rotation = element.getAsJsonObject("rotation");
                if (rotation.has("origin")) {
                    origin = vector(rotation.getAsJsonArray("origin"));
                }
                if (rotation.has("axis")) {
                    angles.put("xyz".indexOf(rotation.get("axis").getAsString()), rotation.get("angle").getAsDouble());
                } else {
                    for (int axis = 0; axis < 3; axis++) {
                        String key = String.valueOf("xyz".charAt(axis));
                        if (rotation.has(key)) {
                            angles.put(axis, rotation.get(key).getAsDouble());
                        }
                    }
                }
            }
            double[] box = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                    Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
            for (int i = 0; i < 8; i++) {
                double[] p = {((i & 4) == 0 ? from[0] : to[0]) - origin[0], ((i & 2) == 0 ? from[1] : to[1]) - origin[1],
                        ((i & 1) == 0 ? from[2] : to[2]) - origin[2]};
                for (int axis = 0; axis < 3; axis++) {
                    p = rotate(p, axis, angles.getOrDefault(axis, 0.0));
                }
                for (int k = 0; k < 3; k++) {
                    double mm = (p[k] + origin[k] - 8.0) * MODEL_UNIT_MM * scale;
                    box[k] = Math.min(box[k], mm);
                    box[k + 3] = Math.max(box[k + 3], mm);
                }
            }
            out.add(box);
        }
        return out.toArray(new double[0][]);
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
}
