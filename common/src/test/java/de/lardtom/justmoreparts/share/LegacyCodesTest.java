package de.lardtom.justmoreparts.share;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.lardtom.justmoreparts.bench.PartFiles;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.build.DroneData;
import de.lardtom.propwash.api.part.AccessorySlot;
import de.lardtom.propwash.api.part.PartKind;
import de.lardtom.propwash.api.share.ShareResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LegacyCodesTest {
    static final String VECTORS_111 = "/share/legacy-vectors-1.1.1.json";
    static final String VECTORS_100 = "/share/pw1-vectors-1.0.0.json";

    static JsonArray vectors(String path) throws IOException {
        try (InputStream in = LegacyCodesTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path + " fehlt");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
        }
    }

    @Test
    public void everyVectorOfJmp111DecodesToTheSameBuildAsThen() throws IOException {
        PartFiles.registerAll();
        List<String> failures = new ArrayList<>();
        int pw1 = 0;
        int pw2 = 0;
        for (JsonElement element : vectors(VECTORS_111)) {
            JsonObject vector = element.getAsJsonObject();
            String format = vector.get("format").getAsString();
            if (format.equals("PW1")) {
                pw1++;
            } else if (format.equals("PW2")) {
                pw2++;
            }
            compare(vector, failures);
        }
        assertEquals(61, pw1);
        assertEquals(63, pw2);
        assertTrue(failures.isEmpty(), failures.size() + " Abweichungen:\n" + String.join("\n", failures));
    }

    @Test
    public void publishedPw1CodesOf100StillDecode() throws IOException {
        PartFiles.registerAll();
        List<String> failures = new ArrayList<>();
        JsonArray vectors = vectors(VECTORS_100);
        for (JsonElement element : vectors) {
            compare(element.getAsJsonObject(), failures);
        }
        assertEquals(61, vectors.size());
        assertTrue(failures.isEmpty(), failures.size() + " Abweichungen:\n" + String.join("\n", failures));
    }

    @Test
    public void legacyCodesWithJmpPartsConvertToPw3WithTheSameBuild() throws IOException {
        PartFiles.registerAll();
        List<String> failures = new ArrayList<>();
        int converted = 0;
        for (JsonElement element : vectors(VECTORS_111)) {
            JsonObject vector = element.getAsJsonObject();
            if (!vector.get("status").getAsString().equals("OK") || !vector.getAsJsonArray("unknown_parts").isEmpty()) {
                continue;
            }
            JsonObject build = vector.getAsJsonObject("decoded").getAsJsonObject("build");
            if (!build.toString().contains("\"justmoreparts:")) {
                continue;
            }
            String name = vector.get("name").getAsString();
            ShareResult result = PropwashApi.shareCodes().convert(vector.get("code").getAsString());
            if (!result.ok() || result.convertedCode().isEmpty()) {
                failures.add(name + ": nicht umgerechnet " + result.status());
                continue;
            }
            String code = result.convertedCode().get();
            ShareResult read = PropwashApi.shareCodes().decode(code);
            if (!read.ok() || read.format().orElse(null) != ShareResult.Format.PW3) {
                failures.add(name + ": PW3 nicht lesbar " + read.status());
                continue;
            }
            Map<String, String> expected = expectedBuild(build);
            Map<String, String> actual = actualBuild(read.data().orElseThrow().build());
            if (!expected.equals(actual)) {
                failures.add(name + ": PW3 " + actual + " statt " + expected);
            }
            converted++;
        }
        assertTrue(converted >= 40, "zu wenige Vektoren mit JMP-Teilen: " + converted);
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }

    static void compare(JsonObject vector, List<String> failures) {
        String name = vector.get("name").getAsString();
        String code = vector.get("code").getAsString();
        String status = vector.get("status").getAsString();
        ShareResult result = PropwashApi.shareCodes().decode(code);
        if (!result.status().name().equals(status)) {
            failures.add(name + ": Status " + result.status() + " statt " + status);
            return;
        }
        if (!result.ok()) {
            return;
        }
        if (vector.has("format") && !result.format().map(Enum::name).orElse("").equals(vector.get("format").getAsString())) {
            failures.add(name + ": Format " + result.format() + " statt " + vector.get("format").getAsString());
        }
        JsonObject decoded = vector.getAsJsonObject("decoded");
        DroneData data = result.data().orElseThrow();
        Map<String, String> expected = expectedBuild(decoded.getAsJsonObject("build"));
        Map<String, String> actual = actualBuild(data.build());
        boolean unknown = vector.has("unknown_parts") && !vector.getAsJsonArray("unknown_parts").isEmpty();
        if (unknown) {
            if (result.unknownParts().isEmpty()) {
                failures.add(name + ": unbekannte Teile nicht gemeldet");
            }
            expected.entrySet().removeIf(e -> e.getValue().startsWith("?") || e.getValue().startsWith("otheraddon:"));
            actual.keySet().retainAll(expected.keySet());
        }
        if (!expected.equals(actual)) {
            failures.add(name + ": Bau " + actual + " statt " + expected);
        }
        Map<String, Integer> paint = new TreeMap<>();
        if (decoded.has("paint") && decoded.get("paint").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : decoded.getAsJsonObject("paint").entrySet()) {
                paint.put(entry.getKey(), Integer.parseInt(entry.getValue().getAsString().substring(1), 16));
            }
        }
        if (!paint.equals(new TreeMap<>(data.paint().colors()))) {
            failures.add(name + ": Lack " + data.paint().colors() + " statt " + paint);
        }
        Map<String, Float> tune = new TreeMap<>();
        if (decoded.has("tune") && decoded.get("tune").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : decoded.getAsJsonObject("tune").entrySet()) {
                tune.put(entry.getKey(), entry.getValue().getAsFloat());
            }
        }
        if (!tune.equals(new TreeMap<>(data.tune().values()))) {
            failures.add(name + ": Tuning " + data.tune().values() + " statt " + tune);
        }
        String expectedName = decoded.has("name") && !decoded.get("name").isJsonNull() ? decoded.get("name").getAsString() : null;
        if (!Objects.equals(expectedName, data.name().orElse(null))) {
            failures.add(name + ": Name " + data.name() + " statt " + expectedName);
        }
        String osd = decoded.has("osd") && decoded.get("osd").isJsonObject() ? decoded.getAsJsonObject("osd").get("json").getAsString() : null;
        if (!Objects.equals(osd, result.osdJson().orElse(null))) {
            failures.add(name + ": OSD " + result.osdJson() + " statt " + osd);
        }
    }

    static Map<String, String> expectedBuild(JsonObject build) {
        Map<String, String> out = new TreeMap<>();
        for (PartKind kind : List.of(PartKind.FRAME, PartKind.STACK, PartKind.MOTOR, PartKind.PROP, PartKind.VIDEO, PartKind.BATTERY)) {
            String key = kind.name().toLowerCase(Locale.ROOT);
            out.put(key, build.get(key).getAsString());
        }
        if (build.has("accessories")) {
            for (Map.Entry<String, JsonElement> entry : build.getAsJsonObject("accessories").entrySet()) {
                out.put("accessory." + entry.getKey(), entry.getValue().getAsString());
            }
        }
        return out;
    }

    static Map<String, String> actualBuild(BuildSpec build) {
        Map<String, String> out = new TreeMap<>();
        out.put("frame", build.frame().toString());
        out.put("stack", build.stack().toString());
        out.put("motor", build.motor().toString());
        out.put("prop", build.prop().toString());
        out.put("video", build.video().toString());
        out.put("battery", build.battery().toString());
        for (Map.Entry<AccessorySlot, ?> entry : build.accessories().entrySet()) {
            out.put("accessory." + entry.getKey().name().toLowerCase(Locale.ROOT), entry.getValue().toString());
        }
        return out;
    }
}
