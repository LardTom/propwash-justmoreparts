package de.lardtom.justmoreparts.share;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.bench.PartFiles;
import de.lardtom.propwash.api.part.CellChemistry;
import de.lardtom.propwash.api.share.ShareTableCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShareTableTest {
    static final String TABLE = "data/" + JustMoreParts.MOD_ID + "/propwash_share/table.json";

    static Path root() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("settings.gradle"))) {
            dir = dir.getParent();
        }
        assertNotNull(dir, "Projektwurzel nicht gefunden");
        return dir;
    }

    static Path resources() {
        return root().resolve("common/src/main/resources");
    }

    static Path baseline() {
        return root().resolve("common/sharecode-baseline.json");
    }

    static String version() {
        try {
            return properties().getProperty("version");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static JsonObject table(Path resources) throws IOException {
        return JsonParser.parseString(Files.readString(resources.resolve(TABLE), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    public void tableHasNoFindingsAgainstTheBaseline() throws IOException {
        List<ShareTableCheck.Finding> findings = ShareTableCheck.check(resources(), version(), baseline());
        assertTrue(findings.isEmpty(), findings.toString());
        List<ShareTableCheck.Finding> withoutBaseline = ShareTableCheck.check(resources(), version(), null);
        assertTrue(withoutBaseline.isEmpty(), withoutBaseline.toString());
    }

    @Test
    public void everyPartFileHasExactlyOneEntryOfItsKind() throws IOException {
        JsonObject parts = table(resources()).getAsJsonObject("parts");
        Path partRoot = resources().resolve("data/" + JustMoreParts.MOD_ID + "/propwash_parts");
        int files = 0;
        for (String kind : ShareTableCheck.KINDS) {
            Path dir = partRoot.resolve(kind);
            Set<String> expected = new TreeSet<>();
            if (Files.isDirectory(dir)) {
                try (Stream<Path> list = Files.list(dir)) {
                    list.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".json")).forEach(n -> expected.add(n.substring(0, n.length() - 5)));
                }
            }
            files += expected.size();
            List<String> listed = ids(parts, kind);
            assertEquals(expected, new TreeSet<>(listed), kind);
            assertEquals(listed.size(), new TreeSet<>(listed).size(), kind + ": doppelte Einträge");
        }
        assertEquals(PartFiles.parseAll().size() - countChemistry(), files);
    }

    @Test
    public void headerAndEntriesFollowTheFormat() throws IOException {
        JsonObject root = table(resources());
        assertEquals(1, root.get("format").getAsInt());
        assertEquals(JustMoreParts.MOD_ID, root.get("namespace").getAsString());
        assertEquals(2, root.get("short_id").getAsInt());
        for (String kind : root.getAsJsonObject("parts").keySet()) {
            assertTrue(ShareTableCheck.KINDS.contains(kind), kind);
            for (JsonElement element : root.getAsJsonObject("parts").getAsJsonArray(kind)) {
                JsonObject entry = element.getAsJsonObject();
                assertTrue(entry.has("id") && entry.has("since"), entry.toString());
                assertTrue(entry.has("fallback") && entry.get("fallback").getAsString().startsWith("propwash:"), entry.toString());
                assertFalse(entry.has("deprecated") && !entry.has("replaced_by"), entry.toString());
            }
        }
    }

    @Test
    public void checkFindsAMovedEntryAndAnUnlistedPart(@TempDir Path temp) throws IOException {
        copy(resources().resolve("data/" + JustMoreParts.MOD_ID), temp.resolve("data/" + JustMoreParts.MOD_ID));
        Path file = temp.resolve(TABLE);
        JsonObject root = table(temp);
        JsonArray motors = root.getAsJsonObject("parts").getAsJsonArray("motor");
        JsonElement first = motors.get(0);
        motors.set(0, motors.get(1));
        motors.set(1, first);
        Files.writeString(file, ShareTableCheck.write(root), StandardCharsets.UTF_8);
        assertTrue(codes(ShareTableCheck.check(temp, version(), baseline())).contains("ENTRY_MOVED"));
        motors.remove(0);
        Files.writeString(file, ShareTableCheck.write(root), StandardCharsets.UTF_8);
        Set<String> codes = codes(ShareTableCheck.check(temp, version(), baseline()));
        assertTrue(codes.contains("MISSING_ENTRY") && codes.contains("ENTRY_REMOVED"), codes.toString());
    }

    @Test
    public void baselineIsTheFrozenTable(@TempDir Path temp) throws IOException {
        Path out = temp.resolve("baseline.json");
        ShareTableCheck.freeze(resources(), out);
        assertEquals(Files.readString(baseline(), StandardCharsets.UTF_8), Files.readString(out, StandardCharsets.UTF_8),
                "sharecode-baseline.json weicht von freeze ab (nach einem Release neu einfrieren)");
    }

    @Test
    public void metadataDeclaresNoBreaksOrIncompatibleEntries() throws IOException {
        JsonObject fabric = JsonParser.parseString(Files.readString(root().resolve("fabric/src/main/resources/fabric.mod.json"),
                StandardCharsets.UTF_8)).getAsJsonObject();
        assertFalse(fabric.has("breaks") || fabric.has("conflicts"), "fabric.mod.json");
        assertEquals(">=0.6.0", properties().getProperty("propwash_fabric_range"));
        assertEquals("[0.6.0,)", properties().getProperty("propwash_version_range"));
        String toml = Files.readString(root().resolve("neoforge/src/main/resources/META-INF/neoforge.mods.toml"), StandardCharsets.UTF_8);
        assertFalse(toml.contains("\"incompatible\"") || toml.contains("\"discouraged\""), "neoforge.mods.toml");
    }

    static Properties properties() throws IOException {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(root().resolve("gradle.properties"), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    static List<String> ids(JsonObject parts, String kind) {
        List<String> out = new ArrayList<>();
        if (parts.has(kind)) {
            for (JsonElement element : parts.getAsJsonArray(kind)) {
                out.add(element.getAsJsonObject().get("id").getAsString());
            }
        }
        return out;
    }

    private static Set<String> codes(List<ShareTableCheck.Finding> findings) {
        Set<String> out = new TreeSet<>();
        for (ShareTableCheck.Finding finding : findings) {
            out.add(finding.code());
        }
        return out;
    }

    private static long countChemistry() {
        return PartFiles.parseAll().values().stream().filter(v -> v instanceof CellChemistry).count();
    }

    private static void copy(Path from, Path to) throws IOException {
        try (Stream<Path> walk = Files.walk(from)) {
            for (Path source : walk.toList()) {
                Path target = to.resolve(from.relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(source, target);
                }
            }
        }
    }
}
