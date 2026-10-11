package de.lardtom.justmoreparts.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.bench.PartFiles;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.part.PartKind;
import de.lardtom.propwash.api.registry.ExtensionTab;
import de.lardtom.propwash.api.registry.TabSection;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JmpTabTest {
    @Test
    public void presetsComeFirstThenFramesMotorsPropsPowerAndVideo() {
        assertEquals(List.of(TabSection.PRESETS, TabSection.FRAMES, TabSection.MOTORS, TabSection.PROPS, TabSection.STACKS,
                TabSection.BATTERIES, TabSection.VIDEO), JmpTab.ORDER);
        List<TabSection> complete = TabSection.complete(JmpTab.ORDER);
        assertEquals(JmpTab.ORDER, complete.subList(0, JmpTab.ORDER.size()));
        assertEquals(TabSection.values().length, complete.size());
    }

    @Test
    public void everyCategoryWithJmpPartsHasItsPlaceInTheOrder() throws IOException {
        Path root = PartFiles.root();
        assertNotNull(root, "Teile-Ordner fehlt auf dem Klassenpfad");
        try (Stream<Path> dirs = Files.list(root)) {
            for (Path dir : dirs.filter(Files::isDirectory).toList()) {
                String category = dir.getFileName().toString();
                if ("chemistry".equals(category)) {
                    continue;
                }
                PartKind kind = PartKind.byKey(category).orElseThrow(() -> new AssertionError("unbekannte Kategorie " + category));
                TabSection section = TabSection.of(kind).orElseThrow();
                assertTrue(JmpTab.ORDER.contains(section), category + " fehlt in JmpTab.ORDER");
            }
        }
    }

    @Test
    public void iconIsTheDeadcatFrame() {
        assertEquals(JustMoreParts.id("deadcat_5"), JmpTab.ICON);
        assertTrue(Files.isRegularFile(PartFiles.root().resolve("frame/deadcat_5.json")), "deadcat_5.json fehlt");
    }

    @Test
    public void tabNameIsJustMorePartsInBothLanguagesAndPropwashAddsThePrefix() throws IOException {
        assertEquals("itemGroup.justmoreparts", JmpTab.NAME_KEY);
        Path resources = PartFiles.root().getParent().getParent().getParent();
        for (String language : List.of("de_de", "en_us")) {
            JsonObject merged = read(resources.resolve("assets/justmoreparts/lang/" + language + ".json"));
            JsonObject module = read(moduleLang().resolve(language + ".json"));
            assertEquals("Just More Parts", merged.get(JmpTab.NAME_KEY).getAsString(), language);
            assertEquals("Just More Parts", module.get(JmpTab.NAME_KEY).getAsString(), language);
            try (InputStream in = JmpTabTest.class.getResourceAsStream("/assets/propwash/lang/" + language + ".json")) {
                assertNotNull(in, "Propwash-Sprachdatei " + language + " fehlt auf dem Klassenpfad");
                JsonObject propwash = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                assertEquals("Propwash: %s", propwash.get(ExtensionTab.TITLE_KEY).getAsString(), language);
            }
        }
    }

    @Test
    public void jmpNeedsTheApiWithOwnTabs() {
        assertEquals("1.4", JustMoreParts.REQUIRED_API);
        assertTrue(PropwashApi.isCompatible(JustMoreParts.REQUIRED_API), "Propwash-API " + PropwashApi.API_VERSION);
    }

    private static Path moduleLang() {
        for (Path candidate : List.of(Path.of("src/lang/core"), Path.of("common/src/lang/core"))) {
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        throw new AssertionError("src/lang/core nicht gefunden (Arbeitsverzeichnis " + Path.of("").toAbsolutePath() + ")");
    }

    private static JsonObject read(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
