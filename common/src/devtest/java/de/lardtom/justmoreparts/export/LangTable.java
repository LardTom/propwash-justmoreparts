package de.lardtom.justmoreparts.export;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public final class LangTable {
    public static final List<String> LANGUAGES = List.of("de_de", "en_us");

    private final Map<String, Map<String, String>> texts = new TreeMap<>();
    private final Map<String, Map<String, String>> used = new TreeMap<>();
    private final Problems problems;

    public LangTable(Problems problems) {
        this.problems = problems;
        for (String language : LANGUAGES) {
            this.texts.put(language, new TreeMap<>());
            this.used.put(language, new TreeMap<>());
        }
    }

    public void load(String where, byte[] json) {
        String name = where.substring(where.lastIndexOf('/') + 1);
        String language = name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
        Map<String, String> target = this.texts.get(language);
        if (target == null) {
            return;
        }
        try {
            JsonObject object = JsonParser.parseString(new String(json, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    target.putIfAbsent(entry.getKey(), entry.getValue().getAsString());
                }
            }
        } catch (RuntimeException e) {
            this.problems.warning(where, "Sprachdatei nicht lesbar: " + e.getMessage());
        }
    }

    public Optional<String> get(String language, String key) {
        String value = this.texts.getOrDefault(language, Map.of()).get(key);
        if (value != null) {
            this.used.get(language).put(key, value);
        }
        return Optional.ofNullable(value);
    }

    public boolean has(String key) {
        return this.texts.get(LANGUAGES.getFirst()).containsKey(key) || this.texts.get(LANGUAGES.getLast()).containsKey(key);
    }

    public JsonObject both(String key, String where, boolean required) {
        JsonObject out = new JsonObject();
        for (String language : LANGUAGES) {
            Optional<String> value = this.get(language, key);
            out.add(language, value.<JsonElement>map(JsonPrimitive::new).orElse(JsonNull.INSTANCE));
            if (value.isEmpty() && required) {
                this.problems.warning(where, "Text fehlt (" + language + "): " + key);
            }
        }
        return out;
    }

    public JsonElement bothOrNull(String key, String where) {
        if (!this.has(key)) {
            return JsonNull.INSTANCE;
        }
        return this.both(key, where, true);
    }

    public JsonObject usedJson(String language) {
        JsonObject out = new JsonObject();
        this.used.getOrDefault(language, Map.of()).forEach(out::addProperty);
        return out;
    }
}
