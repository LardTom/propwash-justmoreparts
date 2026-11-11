package de.lardtom.justmoreparts.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.lardtom.justmoreparts.bench.PartFiles;
import de.lardtom.justmoreparts.preset.JmpPresets;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReleaseTextsTest {
    private static final List<String> EN = List.of("README.md", "docs/modrinth/description.md");
    private static final List<String> DE = List.of("README.de.md", "docs/modrinth/description_de.md");
    private static final String LISTING = "docs/modrinth/listing.md";
    private static final List<String> PART_KINDS = List.of("motor", "prop", "battery", "stack", "video", "frame");
    private static final List<String> ROW_KINDS = List.of("motor", "prop", "battery", "chemistry", "stack", "video", "frame", "preset");
    private static final Map<String, String> ROWS = Map.ofEntries(
            Map.entry("motors", "motor"), Map.entry("motoren", "motor"),
            Map.entry("propellers", "prop"), Map.entry("props", "prop"), Map.entry("propeller", "prop"),
            Map.entry("batteries", "battery"), Map.entry("akkus", "battery"),
            Map.entry("cellchemistry", "chemistry"), Map.entry("zellchemie", "chemistry"),
            Map.entry("stacks/aios", "stack"),
            Map.entry("videosystems", "video"), Map.entry("videosysteme", "video"),
            Map.entry("frames", "frame"), Map.entry("rahmen", "frame"),
            Map.entry("presets", "preset"));
    private static final String NEW_PRESET = "whoop65_race_1s";
    private static final Pattern TOTAL = Pattern.compile("\\b(\\d{2,})\\s+(?:more|weitere)\\b");
    private static final Pattern REQUIRES = Pattern.compile("Propwash(?: FPV)?\\s+(\\d+\\.\\d+\\.\\d+)\\s+(?:or newer|oder neuer)");
    private static final Pattern REQUIRES_LISTING = Pattern.compile("Propwash FPV\\s*≥\\s*(\\d+\\.\\d+\\.\\d+)");
    private static final Pattern OLD_COMMANDS = Pattern.compile("/(?:jmp|justmoreparts) code (?:export|info|import|give)");
    private static final Pattern VERSION_LINE = Pattern.compile("Version \\*\\*([^*]+)\\*\\*");
    private static final Pattern LISTING_VERSION = Pattern.compile("Just More Parts (\\d+\\.\\d+\\.\\d+)");
    private static final Pattern JAR = Pattern.compile("justmoreparts-(?:fabric|neoforge)-[\\d.]+-(\\d+\\.\\d+\\.\\d+)\\.jar");

    private static Path root() {
        return LicenseFilesTest.root();
    }

    private static String read(String name) {
        try {
            return Files.readString(root().resolve(name), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String flat(String name) {
        return read(name).replaceAll("\\s+", " ");
    }

    private static Properties gradle() {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(root().resolve("gradle.properties"), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return properties;
    }

    private static String release() {
        String version = gradle().getProperty("version");
        assertNotNull(version, "gradle.properties ohne version");
        int dash = version.indexOf('-');
        return dash < 0 ? version : version.substring(0, dash);
    }

    private static String minimumPropwash() {
        String range = gradle().getProperty("propwash_version_range");
        assertNotNull(range, "gradle.properties ohne propwash_version_range");
        Matcher matcher = Pattern.compile("^\\[(\\d+\\.\\d+\\.\\d+)").matcher(range);
        assertTrue(matcher.find(), "propwash_version_range ohne Untergrenze: " + range);
        return matcher.group(1);
    }

    private static List<Path> files(String kind) {
        Path dir = PartFiles.root();
        assertNotNull(dir, PartFiles.ROOT + " nicht auf dem Klassenpfad");
        try (Stream<Path> all = Files.list(dir.resolve(kind))) {
            return all.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Map<String, Integer> counts() {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (String kind : ROW_KINDS) {
            out.put(kind, "preset".equals(kind) ? JmpPresets.ALL.size() : files(kind).size());
        }
        return out;
    }

    private static int total() {
        int sum = 0;
        for (String kind : PART_KINDS) {
            sum += files(kind).size();
        }
        return sum;
    }

    private static int[] kvRange() {
        int min = Integer.MAX_VALUE;
        int max = 0;
        for (Path file : files("motor")) {
            try {
                JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                int kv = json.get("kv").getAsInt();
                min = Math.min(min, kv);
                max = Math.max(max, kv);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return new int[] {min, max};
    }

    private static String lang(String language, String key) {
        JsonObject json = JsonParser.parseString(read("common/src/lang/preset/" + language + ".json")).getAsJsonObject();
        assertTrue(json.has(key), "Sprachschlüssel fehlt: " + language + " " + key);
        return json.get(key).getAsString();
    }

    private static List<String> playerTexts() {
        List<String> out = new ArrayList<>(EN);
        out.addAll(DE);
        return out;
    }

    private static List<String> all(Pattern pattern, String text) {
        List<String> out = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            out.add(matcher.group(1));
        }
        return out;
    }

    private static Map<String, String> rows(String name) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String line : read(name).lines().toList()) {
            if (!line.startsWith("|")) {
                continue;
            }
            String[] cells = line.split("\\|");
            if (cells.length < 3) {
                continue;
            }
            String label = cells[1].replace(" ", "").toLowerCase(Locale.ROOT);
            String kind = ROWS.get(label);
            if (kind != null && cells[2].trim().matches("\\d+")) {
                out.put(kind, line);
            }
        }
        return out;
    }

    private static String compact(String text) {
        return text.replaceAll("[\\s\\u00a0\\u202f\\u2009]+", "");
    }

    @Test
    public void versionAndPropwashRequirementFollowTheBuild() {
        String release = release();
        String minimum = minimumPropwash();
        List<String> errors = new ArrayList<>();
        for (String name : List.of("README.md", "README.de.md")) {
            List<String> versions = all(VERSION_LINE, read(name));
            if (!versions.equals(List.of(release))) {
                errors.add(name + ": Versionszeile " + versions + " statt " + release);
            }
        }
        for (String name : playerTexts()) {
            List<String> required = all(REQUIRES, flat(name));
            if (required.isEmpty()) {
                errors.add(name + ": nennt keine Mindestversion von Propwash („Propwash FPV " + minimum + " or newer/oder neuer“)");
            }
            for (String version : required) {
                if (!version.equals(minimum)) {
                    errors.add(name + ": verlangt Propwash " + version + " statt " + minimum);
                }
            }
        }
        String listing = flat(LISTING);
        List<String> listed = all(REQUIRES_LISTING, listing);
        if (listed.isEmpty()) {
            errors.add(LISTING + ": nennt keine Abhängigkeit „Propwash FPV ≥ " + minimum + "“");
        }
        for (String version : listed) {
            if (!version.equals(minimum)) {
                errors.add(LISTING + ": Abhängigkeit Propwash FPV ≥ " + version + " statt " + minimum);
            }
        }
        for (Pattern pattern : List.of(LISTING_VERSION, JAR)) {
            for (String version : all(pattern, listing)) {
                if (!version.equals(release)) {
                    errors.add(LISTING + ": Version " + version + " statt " + release + " (" + pattern.pattern() + ")");
                }
            }
        }
        assertTrue(errors.isEmpty(), "Texte passen nicht zu Version " + release + " und Propwash ab " + minimum
                + " aus gradle.properties:\n" + String.join("\n", errors));
    }

    @Test
    public void countsFollowThePartData() {
        Map<String, Integer> counts = counts();
        int total = total();
        int[] kv = kvRange();
        List<String> errors = new ArrayList<>();
        for (String name : playerTexts()) {
            List<String> totals = all(TOTAL, flat(name));
            if (totals.isEmpty()) {
                errors.add(name + ": nennt keine Gesamtzahl der Teile (" + total + ")");
            }
            for (String value : totals) {
                if (Integer.parseInt(value) != total) {
                    errors.add(name + ": " + value + " Teile statt " + total);
                }
            }
            Map<String, String> rows = rows(name);
            for (String kind : ROW_KINDS) {
                String row = rows.get(kind);
                if (row == null) {
                    errors.add(name + ": keine Tabellenzeile für " + kind);
                    continue;
                }
                int stated = Integer.parseInt(row.split("\\|")[2].trim());
                if (stated != counts.get(kind)) {
                    errors.add(name + ": " + kind + " " + stated + " statt " + counts.get(kind));
                }
            }
            String motors = rows.get("motor");
            if (motors != null) {
                for (int value : kv) {
                    if (!compact(motors).contains(value + "KV")) {
                        errors.add(name + ": Motor-Zeile ohne " + value + " KV (Spanne " + kv[0] + "–" + kv[1] + " KV)");
                    }
                }
            }
        }
        List<String> listed = all(TOTAL, flat(LISTING));
        if (listed.size() < 2) {
            errors.add(LISTING + ": Kurzbeschreibungen ohne Gesamtzahl der Teile");
        }
        for (String value : listed) {
            if (Integer.parseInt(value) != total) {
                errors.add(LISTING + ": " + value + " Teile statt " + total);
            }
        }
        assertTrue(errors.isEmpty(), "Zahlen in den Texten passen nicht zu den Teile-Daten (" + total + " Teile, " + counts
                + "):\n" + String.join("\n", errors));
    }

    @Test
    public void shareCodesComeFromPropwash() {
        List<String> errors = new ArrayList<>();
        for (String name : playerTexts()) {
            String text = flat(name);
            for (String phrase : List.of("/propwash code", "PW3")) {
                if (!text.contains(phrase)) {
                    errors.add(name + ": fehlt „" + phrase + "“");
                }
            }
        }
        List<String> names = new ArrayList<>(playerTexts());
        names.add(LISTING);
        for (String name : names) {
            Matcher matcher = OLD_COMMANDS.matcher(flat(name));
            while (matcher.find()) {
                errors.add(name + ": alter Befehl „" + matcher.group() + "“");
            }
        }
        assertTrue(errors.isEmpty(), "Sharecodes gehören ab 1.2.0 zu Propwash (PW3, /propwash code), /jmp code ist nur noch "
                + "ein Hinweis:\n" + String.join("\n", errors));
    }

    @Test
    public void newContentOfTheReleaseIsNamed() {
        String key = "justmoreparts.preset." + NEW_PRESET;
        JmpPresets.byPath(NEW_PRESET);
        List<String> errors = new ArrayList<>();
        Map<String, List<String>> expected = new LinkedHashMap<>();
        for (String name : EN) {
            expected.put(name, List.of(lang("en_us", key), "no plugs or cables"));
        }
        for (String name : DE) {
            expected.put(name, List.of(lang("de_de", key), "keine Stecker und keine Kabel"));
        }
        for (Map.Entry<String, List<String>> entry : expected.entrySet()) {
            String text = flat(entry.getKey());
            for (String phrase : entry.getValue()) {
                if (!text.contains(phrase)) {
                    errors.add(entry.getKey() + ": fehlt „" + phrase + "“");
                }
            }
        }
        assertTrue(errors.isEmpty(), "Neues aus 1.2.0 fehlt in den Texten (Preset, Akku ohne Stecker und Kabel):\n"
                + String.join("\n", errors));
    }
}
