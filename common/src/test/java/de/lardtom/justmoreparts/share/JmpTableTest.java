package de.lardtom.justmoreparts.share;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.share.ShareTableCheck;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JmpTableTest {
    static final String PREFIX = JustMoreParts.MOD_ID + ":";
    static final String RELEASED = "1.0.0";

    static JsonObject resource(String path) throws IOException {
        try (InputStream in = JmpTableTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path + " fehlt");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    static JsonObject table() throws IOException {
        return ShareTableTest.table(ShareTableTest.resources());
    }

    static Optional<JsonObject> directoryTable() throws IOException {
        try (InputStream in = PropwashApi.class.getResourceAsStream("/data/" + PropwashApi.MOD_ID + "/propwash_share/directory.json")) {
            assertNotNull(in, "Verzeichnis-Abzug im Propwash-Jar fehlt");
            JsonObject directory = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement element : directory.getAsJsonArray("addons")) {
                JsonObject addon = element.getAsJsonObject();
                if (addon.get("namespace").getAsString().equals(JustMoreParts.MOD_ID)) {
                    assertEquals(2, addon.get("short_id").getAsInt());
                    return Optional.of(addon.getAsJsonObject("table"));
                }
            }
        }
        return Optional.empty();
    }

    @Test
    public void tableIsThePw2TableFilteredOnJmpThenNewPartsAtTheEnd() throws IOException {
        JsonObject pw2 = resource("/share/pw2-parts-1.1.1.json").getAsJsonObject("parts");
        JsonObject parts = table().getAsJsonObject("parts");
        int released = 0;
        for (String kind : ShareTableCheck.KINDS) {
            List<String> expected = new ArrayList<>();
            if (pw2.has(kind)) {
                for (JsonElement element : pw2.getAsJsonArray(kind)) {
                    String id = element.getAsString();
                    if (id.startsWith(PREFIX)) {
                        expected.add(id.substring(PREFIX.length()));
                    }
                }
            }
            List<JsonObject> entries = entries(parts, kind);
            assertTrue(entries.size() >= expected.size(), kind + ": weniger Einträge als in PW2");
            for (int i = 0; i < entries.size(); i++) {
                JsonObject entry = entries.get(i);
                if (i < expected.size()) {
                    assertEquals(expected.get(i), entry.get("id").getAsString(), kind + " Nummer " + i);
                    assertEquals(RELEASED, entry.get("since").getAsString(), kind + " " + expected.get(i));
                    released++;
                } else {
                    assertTrue(ShareTableCheck.parseVersion(entry.get("since").getAsString()).map(v -> v[0] > 1 || v[1] >= 2).orElse(false),
                            kind + " " + entry.get("id").getAsString() + ": neue Teile kommen mit 1.2.0 oder später");
                }
            }
        }
        assertEquals(143, released, "143 Teile aus JMP 1.1.1 in der PW2-Reihenfolge");
    }

    @Test
    public void tableMatchesTheDirectorySnapshotInPropwash() throws IOException {
        JsonObject snapshot = directoryTable().orElseThrow(() -> new AssertionError("JMP fehlt im Verzeichnis-Abzug"));
        JsonObject own = table();
        assertEquals(snapshot.get("short_id").getAsInt(), own.get("short_id").getAsInt());
        assertEquals(snapshot.get("namespace").getAsString(), own.get("namespace").getAsString());
        JsonObject theirs = snapshot.getAsJsonObject("parts");
        JsonObject ours = own.getAsJsonObject("parts");
        for (String kind : ShareTableCheck.KINDS) {
            List<JsonObject> a = entries(theirs, kind);
            List<JsonObject> b = entries(ours, kind);
            assertTrue(b.size() >= a.size(), kind + ": Tabelle kürzer als der Abzug");
            for (int i = 0; i < a.size(); i++) {
                JsonObject expected = a.get(i).deepCopy();
                expected.remove("name");
                assertEquals(expected, b.get(i), kind + " Nummer " + i);
            }
        }
    }

    static List<JsonObject> entries(JsonObject parts, String kind) {
        List<JsonObject> out = new ArrayList<>();
        if (parts.has(kind)) {
            for (JsonElement element : parts.getAsJsonArray(kind)) {
                out.add(element.getAsJsonObject());
            }
        }
        return out;
    }
}
