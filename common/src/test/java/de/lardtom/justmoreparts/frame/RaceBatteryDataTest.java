package de.lardtom.justmoreparts.frame;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaceBatteryDataTest {
    private static final String NS = "justmoreparts";

    private static Path data(String category) {
        return FrameDataTest.resources().resolve("data/" + NS + "/propwash_parts/" + category);
    }

    private static Path assets() {
        return FrameDataTest.resources().resolve("assets/" + NS);
    }

    private static float[] yRange(JsonObject model, String name) {
        float lo = Float.POSITIVE_INFINITY;
        float hi = Float.NEGATIVE_INFINITY;
        for (JsonElement e : model.getAsJsonArray("elements")) {
            JsonObject el = e.getAsJsonObject();
            if (!name.equals(el.has("name") ? el.get("name").getAsString() : "")) {
                continue;
            }
            for (String corner : new String[]{"from", "to"}) {
                JsonArray v = el.getAsJsonArray(corner);
                float mm = (v.get(1).getAsFloat() - 8.0F) * 8.0F;
                lo = Math.min(lo, mm);
                hi = Math.max(hi, mm);
            }
        }
        return new float[]{lo, hi};
    }

    @Test
    void raceFramesCarryTheBatteryUnderTheFrameAndOthersKeepTheirSide() {
        List<String> race = new ArrayList<>();
        for (String stem : FrameDataTest.stems(data("frame"))) {
            JsonObject frame = FrameDataTest.read(data("frame").resolve(stem + ".json"));
            JsonObject battery = frame.getAsJsonObject("battery");
            boolean onTop = battery.get("on_top").getAsBoolean();
            String role = frame.get("role").getAsString();
            if (role.equals("race")) {
                race.add(stem);
                assertFalse(onTop, stem + ": Race-Rahmen tragen den Akku unter dem Rahmen");
                assertEquals(-2.8, battery.get("plate_mm").getAsDouble(), 1e-9, stem + ": Bodenplatte 2 mm + Pad 0,8 mm");
            } else if (!role.equals("whoop")) {
                assertTrue(onTop, stem + ": nur Race-Rahmen und Whoops tragen den Akku unten");
            }
        }
        assertEquals(List.of("race_5_stretched"), race);
    }

    @Test
    void strappedBatteriesHangFromTheirTopWithTheStrapAroundTheBottom() {
        Path models = assets().resolve("models/part/battery");
        JsonObject paint = FrameDataTest.read(assets().resolve("drone_paint.json")).getAsJsonObject("models");
        int hanging = 0;
        for (String stem : FrameDataTest.stems(data("battery"))) {
            JsonObject base = FrameDataTest.read(models.resolve(stem + ".json"));
            boolean strapped = Float.isFinite(yRange(base, "strap")[0]);
            Path under = models.resolve(stem + "_under.json");
            assertEquals(strapped, Files.exists(under), stem + ": hängende Variante genau für Akkus mit Riemen");
            if (!strapped) {
                continue;
            }
            hanging++;
            JsonObject model = FrameDataTest.read(under);
            float[] pack = yRange(model, "pack");
            float[] basePack = yRange(base, "pack");
            assertEquals(0.0F, pack[1], 0.01F, stem + "_under hängt mit der Oberseite am Anbaupunkt");
            assertEquals(basePack[1] - basePack[0], pack[1] - pack[0], 0.01F, stem + "_under gleiche Packhöhe");
            float[] strap = yRange(model, "strap");
            assertTrue(strap[0] < pack[0] && strap[1] > 0.0F && strap[1] <= 4.0F, stem + "_under: Riemen um Boden und Platte");
            assertTrue(Files.isRegularFile(assets().resolve("items/drone_render/battery/" + stem + "_under.json")), stem);
            assertTrue(Files.isRegularFile(models.resolve(stem + "_under_paint.json")), stem);
            assertTrue(paint.has("battery/" + stem + "_under"), stem + "_under fehlt in drone_paint.json");
            assertFalse(Files.exists(assets().resolve("items/" + stem + "_under.json")), "kein Inventar-Item für " + stem + "_under");
        }
        assertTrue(hanging >= 10, "hängende Varianten: " + hanging);
    }
}
