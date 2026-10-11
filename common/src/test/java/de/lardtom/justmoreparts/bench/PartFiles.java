package de.lardtom.justmoreparts.bench;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.part.CellChemistry;
import de.lardtom.propwash.api.part.PartDefinition;
import de.lardtom.propwash.api.part.PartKind;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class PartFiles {
    public static final String NAMESPACE = "justmoreparts";
    public static final String ROOT = "data/" + NAMESPACE + "/propwash_parts";
    public static final List<String> ORDER = List.of("chemistry", "frame", "stack", "motor", "prop", "video", "battery", "accessory");

    private static Map<Identifier, Object> registered;

    private PartFiles() {
    }

    public static synchronized Map<Identifier, Object> registerAll() {
        if (registered != null) {
            return registered;
        }
        Map<Identifier, Object> parsed = parseAll();
        Map<Identifier, Object> out = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Object> entry : parsed.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof CellChemistry chemistry) {
                out.put(entry.getKey(), PropwashApi.parts().registerChemistry(chemistry));
            } else if (value instanceof PartDefinition definition) {
                out.put(entry.getKey(), PropwashApi.parts().register(definition));
            }
        }
        registered = out;
        return out;
    }

    public static Map<Identifier, Object> parseAll() {
        Path resources = root();
        Map<Identifier, Object> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        for (String category : ORDER) {
            Path dir = resources == null ? null : resources.resolve(category);
            if (dir == null || !Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> files = Files.list(dir)) {
                for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList()) {
                    String name = file.getFileName().toString();
                    Identifier id = Identifier.fromNamespaceAndPath(NAMESPACE, name.substring(0, name.length() - 5));
                    try {
                        JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                        out.put(id, parse(category, id, json));
                    } catch (RuntimeException e) {
                        errors.add(category + "/" + name + ": " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        if (resources != null) {
            try (Stream<Path> dirs = Files.list(resources)) {
                for (Path dir : dirs.toList()) {
                    if (!ORDER.contains(dir.getFileName().toString())) {
                        errors.add("unbekannter Kategorie-Ordner: " + dir.getFileName());
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Fehlerhafte Bauteil-Dateien:\n" + String.join("\n", errors));
        }
        return out;
    }

    public static Path root() {
        URL url = PartFiles.class.getClassLoader().getResource(ROOT);
        if (url == null) {
            return null;
        }
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Object parse(String category, Identifier id, JsonObject json) {
        if ("chemistry".equals(category)) {
            return PropwashApi.parts().chemistryFromJson(id, json);
        }
        PartKind kind = PartKind.byKey(category).orElseThrow(() -> new IllegalArgumentException("unbekannte Kategorie " + category));
        return PropwashApi.parts().fromJson(kind, id, json);
    }

    public static Optional<Object> get(Identifier id) {
        return Optional.ofNullable(registerAll().get(id));
    }
}
