package de.lardtom.justmoreparts.power;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.lardtom.justmoreparts.bench.PartFiles;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

final class PowerFiles {
    static final String NS = PartFiles.NAMESPACE;

    private PowerFiles() {
    }

    static Path resources() {
        Path parts = PartFiles.root();
        if (parts == null) {
            throw new IllegalStateException("data/justmoreparts/propwash_parts nicht auf dem Klassenpfad");
        }
        return parts.getParent().getParent().getParent();
    }

    static Path project() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("settings.gradle"))) {
            dir = dir.getParent();
        }
        if (dir == null) {
            Path res = resources();
            dir = res.getParent().getParent().getParent().getParent();
        }
        return dir;
    }

    static List<String> ids(String category) {
        List<String> out = new ArrayList<>();
        try (Stream<Path> files = Files.list(resources().resolve("data/" + NS + "/propwash_parts/" + category))) {
            files.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".json")).sorted()
                    .forEach(n -> out.add(n.substring(0, n.length() - 5)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out;
    }

    static List<String> powerIds() {
        List<String> out = new ArrayList<>(ids("battery"));
        out.addAll(ids("stack"));
        return out;
    }

    static JsonObject json(Path path) {
        try {
            return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static JsonObject part(String category, String id) {
        return json(resources().resolve("data/" + NS + "/propwash_parts/" + category + "/" + id + ".json"));
    }

    static Map<String, String> lang(String language) {
        Map<String, String> out = new LinkedHashMap<>();
        JsonObject json = json(project().resolve("common/src/lang/power/" + language + ".json"));
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            out.put(entry.getKey(), entry.getValue().getAsString());
        }
        return out;
    }

    static List<Path> walk(Path root) {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Map<String, JsonObject> classpathDirectory(String directory, String probe) {
        URL url = PowerFiles.class.getClassLoader().getResource(directory + "/" + probe);
        if (url == null) {
            return Map.of();
        }
        Map<String, JsonObject> out = new LinkedHashMap<>();
        try {
            URI uri = url.toURI();
            if ("jar".equals(uri.getScheme())) {
                FileSystem fs;
                try {
                    fs = FileSystems.getFileSystem(uri);
                } catch (FileSystemNotFoundException e) {
                    fs = FileSystems.newFileSystem(uri, Map.of());
                }
                for (Path path : walk(fs.getPath(directory))) {
                    out.put(path.toString(), json(path));
                }
            } else {
                Path dir = Path.of(uri).getParent();
                for (Path path : walk(dir)) {
                    out.put(path.toString(), json(path));
                }
            }
        } catch (URISyntaxException | IOException e) {
            throw new IllegalStateException(e);
        }
        return out;
    }
}
