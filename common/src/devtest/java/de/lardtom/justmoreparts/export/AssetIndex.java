package de.lardtom.justmoreparts.export;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.lardtom.justmoreparts.platform.Services;
import de.lardtom.propwash.api.part.PartDefinition;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

public final class AssetIndex {
    public static final Map<String, List<String>> CHANNEL_SLOTS = Map.of(
            "frame", List.of("frame"), "tpu", List.of("tpu"), "motor", List.of("motors"),
            "prop", List.of("prop_fl", "prop_fr", "prop_rl", "prop_rr"), "stack", List.of("stack"), "battery", List.of("battery"),
            "camera", List.of("camera"), "antenna", List.of("antenna"), "accessory", List.of("accessories"));
    private static final Pattern VARIANT = Pattern.compile("((_(worn|damaged|broken|ccw|blur|under|paint))*)\\.json");

    private final SortedMap<String, byte[]> files = new TreeMap<>();
    private final Set<String> used = new TreeSet<>();
    private final Map<String, List<Paint>> paint = new LinkedHashMap<>();
    private final Problems problems;

    public record Paint(String channel, int rgb) {
    }

    public record Model(String kind, Optional<String> item, Optional<String> drone, List<String> variants, List<String> textures, boolean paintable,
                        Map<String, Integer> baseColors) {
    }

    public AssetIndex(Problems problems) {
        this.problems = problems;
    }

    public void load(String modId) {
        if (!Services.PLATFORM.isModLoaded(modId)) {
            return;
        }
        for (String suffix : List.of(".json", ".png", ".mcmeta")) {
            SortedMap<String, byte[]> found = Services.PLATFORM.readModResources(modId, "assets", suffix);
            found.forEach(this.files::putIfAbsent);
        }
    }

    public SortedMap<String, byte[]> files() {
        return this.files;
    }

    public List<String> languageFiles() {
        List<String> out = new ArrayList<>();
        for (String path : this.files.keySet()) {
            String[] parts = path.split("/");
            if (parts.length == 4 && parts[0].equals("assets") && parts[2].equals("lang") && parts[3].endsWith(".json")) {
                out.add(path);
            }
        }
        return out;
    }

    public void readPaint() {
        for (Map.Entry<String, byte[]> entry : this.files.entrySet()) {
            String[] parts = entry.getKey().split("/");
            if (parts.length != 3 || !parts[2].equals("drone_paint.json")) {
                continue;
            }
            String namespace = parts[1];
            try {
                JsonObject root = JsonParser.parseString(new String(entry.getValue(), StandardCharsets.UTF_8)).getAsJsonObject();
                JsonObject models = root.has("models") ? root.getAsJsonObject("models") : new JsonObject();
                for (Map.Entry<String, JsonElement> model : models.entrySet()) {
                    String key = model.getKey().contains(":") ? model.getKey() : namespace + ":" + model.getKey();
                    List<Paint> layers = new ArrayList<>();
                    for (JsonElement layer : model.getValue().getAsJsonArray()) {
                        JsonObject object = layer.getAsJsonObject();
                        String base = object.get("base").getAsString();
                        layers.add(new Paint(object.get("channel").getAsString(), Integer.parseInt(base.substring(1), 16)));
                    }
                    this.paint.put(key, layers);
                }
            } catch (RuntimeException e) {
                this.problems.warning(entry.getKey(), "drone_paint.json nicht lesbar: " + e.getMessage());
            }
        }
    }

    public boolean has(String path) {
        return this.files.containsKey(path);
    }

    public Model model(PartDefinition definition) {
        Identifier id = definition.id();
        String category = definition.kind().key();
        String itemPath = "assets/" + id.getNamespace() + "/items/" + id.getPath() + ".json";
        Optional<String> item = Optional.empty();
        List<String> textures = new ArrayList<>();
        if (this.has(itemPath)) {
            item = Optional.of(itemPath);
            this.use(itemPath, textures);
        } else {
            this.problems.warning(id.toString(), "Inventar-Symbol fehlt: " + itemPath);
        }
        String renderBase;
        Optional<Identifier> renderModel = definition.basics().visual().renderModel();
        if (renderModel.isPresent()) {
            Identifier model = renderModel.get();
            renderBase = "assets/" + model.getNamespace() + "/items/" + model.getPath();
        } else {
            renderBase = "assets/" + id.getNamespace() + "/items/drone_render/" + category + "/" + id.getPath();
        }
        Optional<String> drone = Optional.empty();
        List<String> variants = new ArrayList<>();
        if (this.has(renderBase + ".json")) {
            drone = Optional.of(renderBase + ".json");
            this.use(renderBase + ".json", textures);
            String prefix = renderBase;
            for (String path : this.files.subMap(prefix + "_", prefix + "_￿").keySet()) {
                if (VARIANT.matcher(path.substring(prefix.length())).matches()) {
                    variants.add(path);
                    this.use(path, textures);
                }
            }
        }
        String paintKey = renderModel.map(m -> m.getNamespace() + ":" + stripDroneRender(m.getPath()))
                .orElse(id.getNamespace() + ":" + category + "/" + id.getPath());
        List<Paint> layers = this.paint.getOrDefault(paintKey, List.of());
        boolean paintable = !layers.isEmpty() && this.has(renderBase + "_paint.json");
        Map<String, Integer> colors = new LinkedHashMap<>();
        for (Paint layer : layers) {
            for (String slot : CHANNEL_SLOTS.getOrDefault(layer.channel(), List.of())) {
                colors.putIfAbsent(slot, layer.rgb());
            }
        }
        return new Model(drone.isPresent() ? "model" : "procedural", item, drone, variants, List.copyOf(new TreeSet<>(textures)), paintable, colors);
    }

    private static String stripDroneRender(String path) {
        return path.startsWith("drone_render/") ? path.substring("drone_render/".length()) : path;
    }

    public void use(String path, List<String> textures) {
        this.follow(path, textures, new TreeSet<>(), 0);
    }

    private void follow(String path, List<String> textures, Set<String> visited, int depth) {
        if (depth > 16 || !visited.add(path)) {
            return;
        }
        this.used.add(path);
        if (path.endsWith(".png")) {
            textures.add(path);
            String meta = path + ".mcmeta";
            if (this.has(meta)) {
                this.used.add(meta);
            }
            return;
        }
        byte[] data = this.files.get(path);
        if (data == null) {
            return;
        }
        JsonElement root;
        try {
            root = JsonParser.parseString(new String(data, StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            this.problems.warning(path, "JSON nicht lesbar: " + e.getMessage());
            return;
        }
        this.walk(root, path.contains("/models/"), textures, visited, depth, path);
    }

    private void walk(JsonElement element, boolean model, List<String> textures, Set<String> visited, int depth, String from) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement child : array) {
                this.walk(child, model, textures, visited, depth, from);
            }
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            JsonElement value = entry.getValue();
            String key = entry.getKey();
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                String text = value.getAsString();
                if (!model && (key.equals("model") || key.equals("base")) || model && key.equals("parent")) {
                    this.reference(text, "models", ".json", textures, visited, depth, from);
                }
            } else if (model && key.equals("textures") && value.isJsonObject()) {
                for (Map.Entry<String, JsonElement> texture : value.getAsJsonObject().entrySet()) {
                    if (texture.getValue().isJsonPrimitive()) {
                        String text = texture.getValue().getAsString();
                        if (!text.startsWith("#")) {
                            this.reference(text, "textures", ".png", textures, visited, depth, from);
                        }
                    }
                }
            } else {
                this.walk(value, model, textures, visited, depth, from);
            }
        }
    }

    private void reference(String text, String folder, String suffix, List<String> textures, Set<String> visited, int depth, String from) {
        Identifier id = Identifier.tryParse(text);
        if (id == null || id.getNamespace().equals("minecraft")) {
            return;
        }
        String path = "assets/" + id.getNamespace() + "/" + folder + "/" + id.getPath() + suffix;
        if (!this.has(path)) {
            if (!id.getPath().startsWith("builtin/")) {
                this.problems.warning(from, "Verweis ohne Datei: " + path);
            }
            return;
        }
        this.follow(path, textures, visited, depth + 1);
    }

    public void useLayoutFiles() {
        for (String path : this.files.keySet()) {
            String[] parts = path.split("/");
            if (parts.length == 3 && (parts[2].equals("drone_layout.json") || parts[2].equals("drone_paint.json"))) {
                this.used.add(path);
            }
        }
    }

    public Set<String> used() {
        return this.used;
    }
}
