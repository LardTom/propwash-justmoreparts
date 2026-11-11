package de.lardtom.justmoreparts.video;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.lardtom.justmoreparts.bench.PartFiles;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VtxAntennaModelsTest {
    private static final String PAINT = "_paint";
    private static final String STALK = "antenna";
    private static final Set<String> ANTENNA_PARTS = Set.of("antenna", "antenna_tip", "antenna_socket", "antenna_tee",
            "antenna_joint", "antenna_lead");
    private static final double EPS = 1.0E-4;
    private static final String TOOL = " (python3 tools/assets/style_generate.py --category video --write)";

    private static Path resources() {
        Path parts = PartFiles.root();
        if (parts == null) {
            throw new IllegalStateException(PartFiles.ROOT + " nicht auf dem Klassenpfad");
        }
        return parts.getParent().getParent().getParent();
    }

    private static List<String> videoIds() {
        try (Stream<Path> files = Files.list(PartFiles.root().resolve("video"))) {
            return files.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".json")).sorted()
                    .map(n -> n.substring(0, n.length() - 5)).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path modelFile(String variant) {
        return resources().resolve("assets/" + PartFiles.NAMESPACE + "/models/part/video/" + variant + ".json");
    }

    private static List<JsonObject> elements(String variant) {
        try {
            JsonObject model = JsonParser.parseString(Files.readString(modelFile(variant), StandardCharsets.UTF_8)).getAsJsonObject();
            List<JsonObject> out = new ArrayList<>();
            for (JsonElement e : model.getAsJsonArray("elements")) {
                out.add(e.getAsJsonObject());
            }
            return out;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String name(JsonObject element) {
        return element.has("name") ? element.get("name").getAsString() : "";
    }

    private static double[] pivot(JsonObject element) {
        if (!element.has("rotation")) {
            return null;
        }
        JsonArray origin = element.getAsJsonObject("rotation").getAsJsonArray("origin");
        return new double[]{origin.get(0).getAsDouble(), origin.get(1).getAsDouble(), origin.get(2).getAsDouble()};
    }

    private static boolean same(double[] a, double[] b) {
        return Math.abs(a[0] - b[0]) < EPS && Math.abs(a[1] - b[1]) < EPS && Math.abs(a[2] - b[2]) < EPS;
    }

    private static List<double[]> stalkPivots(List<JsonObject> elements) {
        List<double[]> out = new ArrayList<>();
        for (JsonObject element : elements) {
            double[] p = pivot(element);
            if (STALK.equals(name(element)) && p != null) {
                out.add(p);
            }
        }
        return out;
    }

    private static List<String> variants() {
        List<String> out = new ArrayList<>();
        for (String id : videoIds()) {
            out.add(id);
            out.add(id + PAINT);
        }
        return out;
    }

    @Test
    public void everyVtxModelAndPaintVariantHasAntennaStalks() {
        List<String> errors = new ArrayList<>();
        List<String> variants = variants();
        assertEquals(14, variants.size(), "7 Videosysteme mit Grund- und Lack-Modell erwartet");
        for (String variant : variants) {
            if (!Files.exists(modelFile(variant))) {
                errors.add(variant + ": Modell fehlt" + TOOL);
                continue;
            }
            if (stalkPivots(elements(variant)).isEmpty()) {
                errors.add(variant + ": kein gedrehtes Element \"" + STALK + "\"");
            }
        }
        assertTrue(errors.isEmpty(), String.join("\n", errors));
    }

    @Test
    public void everyPartTurningWithTheAntennaCarriesAnAntennaName() {
        List<String> errors = new ArrayList<>();
        for (String variant : variants()) {
            List<JsonObject> elements = elements(variant);
            List<double[]> pivots = stalkPivots(elements);
            for (int i = 0; i < elements.size(); i++) {
                JsonObject element = elements.get(i);
                double[] p = pivot(element);
                if (p == null || pivots.stream().noneMatch(s -> same(s, p))) {
                    continue;
                }
                if (!ANTENNA_PARTS.contains(name(element))) {
                    errors.add(variant + ": Element " + i + " \"" + name(element)
                            + "\" dreht mit der Antenne, heißt aber nicht " + ANTENNA_PARTS + TOOL);
                }
            }
        }
        assertTrue(errors.isEmpty(), errors.size() + " Antennen-Teile ohne Antennen-Namen:\n" + String.join("\n", errors));
    }

    @Test
    public void antennaPartsAreIdenticalInBaseAndPaintModel() {
        List<String> errors = new ArrayList<>();
        for (String id : videoIds()) {
            List<String> base = antennaParts(elements(id));
            List<String> paint = antennaParts(elements(id + PAINT));
            if (!base.equals(paint)) {
                errors.add(id + ": Antennen-Teile im Lack-Modell weichen ab\n  " + base + "\n  " + paint);
            }
        }
        assertTrue(errors.isEmpty(), String.join("\n", errors));
    }

    @Test
    public void withoutAntennaPartsNothingTurnsAroundAnAntennaPivot() {
        List<String> errors = new ArrayList<>();
        for (String variant : variants()) {
            List<JsonObject> elements = elements(variant);
            List<double[]> pivots = stalkPivots(elements);
            List<JsonObject> kept = elements.stream().filter(e -> !ANTENNA_PARTS.contains(name(e))).toList();
            if (kept.isEmpty()) {
                errors.add(variant + ": ohne Antennen-Teile bleibt nichts übrig");
            }
            for (JsonObject element : kept) {
                double[] p = pivot(element);
                if (p != null && pivots.stream().anyMatch(s -> same(s, p))) {
                    errors.add(variant + ": \"" + name(element) + "\" bliebe ohne Antenne an deren Drehpunkt stehen");
                }
            }
        }
        assertTrue(errors.isEmpty(), errors.size() + " Reste nach dem Auslassen der Antennen-Teile:\n" + String.join("\n", errors));
    }

    private static List<String> antennaParts(List<JsonObject> elements) {
        List<String> out = new ArrayList<>();
        for (JsonObject element : elements) {
            if (ANTENNA_PARTS.contains(name(element))) {
                out.add(name(element) + element.get("from") + element.get("to") + element.get("rotation"));
            }
        }
        return out;
    }
}
