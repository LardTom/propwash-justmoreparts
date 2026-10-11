package de.lardtom.justmoreparts.export;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.platform.Services;
import de.lardtom.justmoreparts.share.PartAliases;
import de.lardtom.justmoreparts.share.Pw2Tables;
import de.lardtom.justmoreparts.share.ShareCodec;
import de.lardtom.justmoreparts.share.ShareCompression;
import de.lardtom.justmoreparts.share.ShareContent;
import de.lardtom.justmoreparts.share.ShareDecoded;
import de.lardtom.justmoreparts.share.ShareLayer;
import de.lardtom.justmoreparts.share.ShareStatus;
import de.lardtom.justmoreparts.share.ShareTables;
import de.lardtom.justmoreparts.share.ShareVectors;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.BuildCheck;
import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.build.BuildStats;
import de.lardtom.propwash.api.build.CameraView;
import de.lardtom.propwash.api.build.DronePreset;
import de.lardtom.propwash.api.build.ImportLayer;
import de.lardtom.propwash.api.build.ImportStatus;
import de.lardtom.propwash.api.build.PaintSpec;
import de.lardtom.propwash.api.build.TuneGroup;
import de.lardtom.propwash.api.build.TuneSpec;
import de.lardtom.propwash.api.part.AccessoryDefinition;
import de.lardtom.propwash.api.part.AccessorySlot;
import de.lardtom.propwash.api.part.BatteryDefinition;
import de.lardtom.propwash.api.part.CellChemistry;
import de.lardtom.propwash.api.part.FrameDefinition;
import de.lardtom.propwash.api.part.MotorDefinition;
import de.lardtom.propwash.api.part.Mounts;
import de.lardtom.propwash.api.part.PartDefinition;
import de.lardtom.propwash.api.part.PartKind;
import de.lardtom.propwash.api.part.PropDefinition;
import de.lardtom.propwash.api.part.StackDefinition;
import de.lardtom.propwash.api.part.Vec3Mm;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

public final class WebExport {
    public static final int FORMAT = 1;
    public static final List<PartKind> ORDER = List.of(PartKind.FRAME, PartKind.STACK, PartKind.MOTOR, PartKind.PROP, PartKind.VIDEO,
            PartKind.BATTERY, PartKind.ACCESSORY);
    public static final List<String> OUTPUTS = List.of("manifest.json", "parts.json", "mounts.json", "chemistries.json", "rules.json", "paint.json",
            "tune.json", "presets.json", "analysis", "sharecode", "lang", "assets", "problems.json");
    public static final Set<ShareLayer> PRESET_LAYERS = EnumSet.of(ShareLayer.PARTS, ShareLayer.PAINT, ShareLayer.TUNE, ShareLayer.NAME);
    public static final Set<Identifier> CREATIVE_ONLY = Set.of(Identifier.fromNamespaceAndPath("propwash", "creative_fc"));

    public record Result(boolean pass, String summary, int parts, int vectors) {
    }

    private final Path target;
    private final Problems problems = new Problems();
    private final LangTable lang = new LangTable(this.problems);
    private final AssetIndex assets = new AssetIndex(this.problems);
    private final List<String> written = new ArrayList<>();
    private final Map<String, Integer> counts = new LinkedHashMap<>();
    private List<PartDefinition> parts = List.of();
    private int vectors;

    private WebExport(Path target) {
        this.target = target;
    }

    public static Result run(Path target) {
        return new WebExport(target).export();
    }

    private Result export() {
        this.clean();
        this.parts = sortedParts();
        Set<String> namespaces = new TreeSet<>(List.of(PropwashApi.MOD_ID, JustMoreParts.MOD_ID));
        this.parts.forEach(part -> namespaces.add(part.id().getNamespace()));
        PropwashApi.parts().chemistries().forEach(chemistry -> namespaces.add(chemistry.id().getNamespace()));
        for (String namespace : namespaces) {
            this.assets.load(namespace);
        }
        for (String path : this.assets.languageFiles()) {
            this.lang.load(path, this.assets.files().get(path));
        }
        this.assets.readPaint();
        this.write("parts.json", this.partsJson());
        this.write("mounts.json", this.mountsJson());
        this.write("chemistries.json", this.chemistriesJson());
        int mismatches = RuleCheck.verify(this.problems);
        this.write("rules.json", RuleCheck.toJson(this.lang, mismatches));
        this.write("paint.json", this.paintJson());
        List<DronePreset> presets = PropwashApi.builds().presets();
        this.write("tune.json", this.tuneJson(presets));
        this.write("presets.json", this.presetsJson(presets));
        this.analysis(presets);
        this.write("sharecode/spec.json", this.specJson());
        this.write("sharecode/vectors.json", this.vectorsJson(presets));
        this.write("analysis_labels.json", this.analysisLabelsJson());
        this.lang.both("justmoreparts.share.title", "lang", false);
        for (ImportStatus status : ImportStatus.values()) {
            this.lang.both(status.translationKey(), "lang", true);
        }
        for (ImportLayer layer : ImportLayer.values()) {
            this.lang.both(layer.translationKey(), "lang", true);
        }
        for (String language : LangTable.LANGUAGES) {
            this.write("lang/" + language + ".json", this.lang.usedJson(language));
        }
        this.assets.useLayoutFiles();
        int assetFiles = 0;
        for (String path : this.assets.used()) {
            byte[] data = this.assets.files().get(path);
            if (data != null) {
                ExportJson.write(this.target.resolve(path), data);
                assetFiles++;
            }
        }
        this.counts.put("asset_files", assetFiles);
        this.counts.put("vectors", this.vectors);
        this.write("problems.json", this.problems.toJson());
        this.write("manifest.json", this.manifestJson());
        int partCount = this.parts.size();
        if (this.problems.errors() > 0) {
            List<String> first = new ArrayList<>();
            for (Problems.Entry entry : this.problems.firstErrors(3)) {
                first.add(entry.where() + ": " + entry.message());
            }
            return new Result(false, "FAIL " + this.problems.errors() + " Fehler im Web-Export: " + String.join(" | ", first), partCount, this.vectors);
        }
        return new Result(true, "PASS " + partCount + " Teile, " + this.vectors + " Vektoren", partCount, this.vectors);
    }

    private void clean() {
        for (String name : OUTPUTS) {
            Path path = this.target.resolve(name);
            if (!Files.exists(path)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(path)) {
                for (Path file : walk.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(file);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Web-Export: " + path + " nicht gelöscht", e);
            }
        }
    }

    private void write(String name, JsonElement element) {
        ExportJson.write(this.target.resolve(name), element);
        this.written.add(name);
    }

    static boolean exported(PartDefinition part) {
        return !CREATIVE_ONLY.contains(part.id());
    }

    static List<PartDefinition> sortedParts() {
        List<PartDefinition> out = new ArrayList<>(PropwashApi.parts().all());
        out.removeIf(part -> !exported(part));
        out.sort(Comparator.comparingInt((PartDefinition p) -> ORDER.indexOf(p.kind())).thenComparingInt(p -> p.basics().sortOrder())
                .thenComparing(p -> p.id().toString()));
        return out;
    }

    private String prefix(Identifier id) {
        return PropwashApi.parts().isBuiltin(id) ? PropwashApi.MOD_ID : id.getNamespace();
    }

    private JsonArray partsJson() {
        JsonArray out = new JsonArray();
        EnumSet<PartKind> seen = EnumSet.noneOf(PartKind.class);
        for (PartKind kind : ORDER) {
            this.counts.put(kind.key(), 0);
        }
        for (PartDefinition part : this.parts) {
            Identifier id = part.id();
            boolean builtin = PropwashApi.parts().isBuiltin(id);
            String where = id.toString();
            this.counts.merge(part.kind().key(), 1, Integer::sum);
            seen.add(part.kind());
            JsonObject entry = new JsonObject();
            entry.addProperty("id", id.toString());
            entry.addProperty("category", part.kind().key());
            entry.addProperty("builtin", builtin);
            entry.add("name", this.lang.both("part." + id.getNamespace() + "." + id.getPath(), where, true));
            if (part.kind() == PartKind.FRAME) {
                String key = builtin ? "propwash.content.frame_short." + id.getPath() : id.getNamespace() + ".frame_short." + id.getPath();
                entry.add("short_name", this.lang.has(key) ? this.lang.both(key, where, true) : JsonNull.INSTANCE);
                if (!builtin && !this.lang.has(key)) {
                    this.problems.warning(where, "Kurzname fehlt: " + key);
                }
            } else {
                entry.add("short_name", JsonNull.INSTANCE);
            }
            entry.add("help", this.help(this.prefix(id) + ".help." + id.getPath(), where));
            JsonObject basics = PartData.basics(part.basics());
            entry.add("basics", basics);
            entry.add("data", PartData.data(part));
            entry.add("derived", PartData.derived(part));
            AssetIndex.Model model = this.assets.model(part);
            JsonObject paint = new JsonObject();
            paint.add("slots", ExportJson.strings(PartData.paintSlots(part)));
            paint.addProperty("paintable_model", model.paintable());
            entry.add("paint", paint);
            JsonObject modelJson = new JsonObject();
            modelJson.addProperty("kind", model.kind());
            modelJson.add("item", ExportJson.str(model.item()));
            modelJson.add("drone", ExportJson.str(model.drone()));
            modelJson.add("variants", ExportJson.strings(model.variants()));
            modelJson.add("textures", ExportJson.strings(model.textures()));
            entry.add("model", modelJson);
            out.add(entry);
        }
        return out;
    }

    private JsonObject help(String prefix, String where) {
        JsonObject out = new JsonObject();
        for (String language : LangTable.LANGUAGES) {
            JsonObject help = new JsonObject();
            Optional<String> summary = this.lang.get(language, prefix + ".summary");
            help.add("summary", ExportJson.str(summary));
            if (summary.isEmpty()) {
                this.problems.warning(where, "Hilfe fehlt (" + language + "): " + prefix + ".summary");
            }
            for (String block : List.of("what", "use")) {
                JsonArray lines = new JsonArray();
                for (int i = 1; i <= 8; i++) {
                    this.lang.get(language, prefix + "." + block + "." + i).ifPresent(lines::add);
                }
                help.add(block, lines);
            }
            out.add(language, help);
        }
        return out;
    }

    private JsonElement mountName(String kind, Identifier mount) {
        String key = PropwashApi.MOD_ID.equals(mount.getNamespace()) ? "propwash.content.mount." + kind + "." + mount.getPath()
                : mount.getNamespace() + ".mount." + kind + "." + mount.getPath();
        if (!this.lang.has(key) && Mounts.PROP_UNIVERSAL.equals(mount)) {
            key = "justmoreparts.share.mount.prop.universal";
        }
        if (!this.lang.has(key)) {
            this.problems.warning("mounts.json", "Halterung ohne Namen: " + key);
        }
        return this.lang.both(key, "mounts.json", false);
    }

    private JsonObject mountsJson() {
        Map<Identifier, List<String>> motorFrames = new TreeMap<>();
        Map<Identifier, List<String>> motorParts = new TreeMap<>();
        Map<Identifier, List<String>> stackFrames = new TreeMap<>();
        Map<Identifier, List<String>> stackParts = new TreeMap<>();
        Map<Identifier, List<String>> propMotors = new TreeMap<>();
        Map<Identifier, List<String>> propParts = new TreeMap<>();
        for (PartDefinition part : this.parts) {
            String id = part.id().toString();
            switch (part) {
                case FrameDefinition frame -> {
                    frame.motorMounts().forEach(m -> motorFrames.computeIfAbsent(m, k -> new ArrayList<>()).add(id));
                    frame.stackMounts().forEach(m -> stackFrames.computeIfAbsent(m, k -> new ArrayList<>()).add(id));
                }
                case MotorDefinition motor -> {
                    motorParts.computeIfAbsent(motor.mount(), k -> new ArrayList<>()).add(id);
                    propMotors.computeIfAbsent(motor.propMount(), k -> new ArrayList<>()).add(id);
                }
                case StackDefinition stack -> stackParts.computeIfAbsent(stack.mount(), k -> new ArrayList<>()).add(id);
                case PropDefinition prop -> propParts.computeIfAbsent(prop.mount(), k -> new ArrayList<>()).add(id);
                default -> {
                }
            }
        }
        JsonObject out = new JsonObject();
        out.add("motor", this.mountList("motor", motorFrames, motorParts, "frames"));
        out.add("stack", this.mountList("stack", stackFrames, stackParts, "frames"));
        out.add("prop", this.mountList("prop", propMotors, propParts, "motors"));
        return out;
    }

    private JsonArray mountList(String kind, Map<Identifier, List<String>> holders, Map<Identifier, List<String>> parts, String holderName) {
        TreeSet<Identifier> all = new TreeSet<>(holders.keySet());
        all.addAll(parts.keySet());
        JsonArray out = new JsonArray();
        for (Identifier mount : all) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", mount.toString());
            entry.add("name", this.mountName(kind, mount));
            entry.add(holderName, ExportJson.strings(holders.getOrDefault(mount, List.of())));
            entry.add("parts", ExportJson.strings(parts.getOrDefault(mount, List.of())));
            out.add(entry);
        }
        return out;
    }

    private JsonArray chemistriesJson() {
        List<CellChemistry> chemistries = new ArrayList<>(PropwashApi.parts().chemistries());
        chemistries.sort(Comparator.comparing(c -> c.id().toString()));
        this.counts.put("chemistry", chemistries.size());
        JsonArray out = new JsonArray();
        for (CellChemistry chemistry : chemistries) {
            Identifier id = chemistry.id();
            JsonObject entry = new JsonObject();
            entry.addProperty("id", id.toString());
            String key = PropwashApi.MOD_ID.equals(id.getNamespace()) ? "propwash.content.tooltip.chemistry." + id.getPath()
                    : id.getNamespace() + ".chemistry." + id.getPath();
            entry.add("name", this.lang.both(key, "chemistries.json#" + id, true));
            JsonObject values = PartData.chemistry(chemistry);
            values.entrySet().forEach(e -> entry.add(e.getKey(), e.getValue()));
            List<String> users = new ArrayList<>();
            for (PartDefinition part : this.parts) {
                if (part instanceof BatteryDefinition battery && battery.chemistry().equals(id)) {
                    users.add(part.id().toString());
                }
            }
            entry.add("used_by", ExportJson.strings(users));
            out.add(entry);
        }
        return out;
    }

    private JsonObject paintJson() {
        JsonObject out = new JsonObject();
        JsonArray slots = new JsonArray();
        for (String slot : PaintSpec.SLOTS) {
            JsonObject entry = new JsonObject();
            entry.addProperty("key", slot);
            entry.addProperty("bit", ShareTables.paintBit(slot));
            entry.add("name", this.lang.both("propwash.paint.slot." + slot, "paint.json", true));
            entry.add("hint", this.lang.both("propwash.paint.slot_hint." + slot, "paint.json", false));
            JsonArray categories = new JsonArray();
            for (PartKind kind : ORDER) {
                if (slotsOf(kind).contains(slot)) {
                    categories.add(kind.key());
                }
            }
            entry.add("categories", categories);
            slots.add(entry);
        }
        out.add("slots", slots);
        JsonObject defaults = new JsonObject();
        for (PartDefinition part : this.parts) {
            AssetIndex.Model model = this.assets.model(part);
            JsonObject colors = new JsonObject();
            if (!model.baseColors().isEmpty()) {
                for (String slot : PartData.paintSlots(part)) {
                    Integer rgb = model.baseColors().get(slot);
                    if (rgb != null) {
                        colors.addProperty(slot, ExportJson.hex(rgb));
                    }
                }
            } else if (part.basics().visual().accentRgb().isPresent()) {
                for (String slot : PartData.paintSlots(part)) {
                    colors.addProperty(slot, ExportJson.hex(part.basics().visual().accentRgb().getAsInt()));
                }
            }
            if (!colors.isEmpty()) {
                defaults.add(part.id().toString(), colors);
            }
        }
        out.add("default_colors", defaults);
        return out;
    }

    private static List<String> slotsOf(PartKind kind) {
        return switch (kind) {
            case FRAME -> List.of("frame", "tpu");
            case MOTOR -> List.of("motors");
            case PROP -> List.of("prop_fl", "prop_fr", "prop_rl", "prop_rr");
            case STACK -> List.of("stack");
            case BATTERY -> List.of("battery");
            case VIDEO -> List.of("camera", "antenna");
            case ACCESSORY -> List.of("accessories");
            default -> List.of();
        };
    }

    static JsonElement tuneValue(float value) {
        return Float.isFinite(value) ? new JsonPrimitive(Double.parseDouble(Float.toString(value))) : JsonNull.INSTANCE;
    }

    static JsonObject tune(TuneSpec tune) {
        JsonObject out = new JsonObject();
        new TreeMap<>(tune.values()).forEach((key, value) -> out.add(key, tuneValue(value)));
        return out;
    }

    private JsonObject tuneJson(List<DronePreset> presets) {
        JsonObject out = new JsonObject();
        JsonObject groups = new JsonObject();
        JsonArray flight = new JsonArray();
        JsonArray shared = new JsonArray();
        JsonArray never = new JsonArray();
        JsonObject names = new JsonObject();
        for (TuneGroup group : TuneGroup.values()) {
            String key = group.name().toLowerCase(Locale.ROOT);
            if (TuneGroup.FLIGHT.contains(group)) {
                flight.add(key);
            }
            (ShareTables.TUNE_GROUPS.contains(group) ? shared : never).add(key);
            names.add(key, this.lang.both(group.translationKey(), "tune.json", true));
        }
        groups.add("flight", flight);
        groups.add("shared", shared);
        groups.add("never", never);
        out.add("groups", groups);
        out.add("group_names", names);
        JsonArray keys = new JsonArray();
        for (int i = 0; i < ShareTables.TUNE_KEYS.size(); i++) {
            String key = ShareTables.TUNE_KEYS.get(i);
            JsonObject entry = new JsonObject();
            entry.addProperty("index", i + 1);
            entry.addProperty("key", key);
            entry.addProperty("group", TuneGroup.of(key).name().toLowerCase(Locale.ROOT));
            keys.add(entry);
        }
        out.add("pw1_keys", keys);
        JsonObject defaults = new JsonObject();
        for (DronePreset preset : presets) {
            try {
                defaults.add(preset.id().toString(), tune(PropwashApi.builds().defaultTune(preset.data().build())));
            } catch (IllegalArgumentException e) {
                this.problems.error("tune.json#" + preset.id(), "Standard-Tuning nicht berechenbar: " + e.getMessage());
            }
        }
        out.add("defaults", defaults);
        return out;
    }

    static JsonObject build(BuildSpec build) {
        JsonObject out = new JsonObject();
        out.addProperty("frame", ShareTables.display(build.frame()));
        out.addProperty("stack", ShareTables.display(build.stack()));
        out.addProperty("motor", ShareTables.display(build.motor()));
        out.addProperty("prop", ShareTables.display(build.prop()));
        out.addProperty("video", ShareTables.display(build.video()));
        out.addProperty("battery", ShareTables.display(build.battery()));
        JsonObject accessories = new JsonObject();
        for (AccessorySlot slot : ShareTables.ACCESSORY_SLOTS) {
            build.accessory(slot).ifPresent(id -> accessories.addProperty(slot.key(), ShareTables.display(id)));
        }
        out.add("accessories", accessories);
        return out;
    }

    static JsonObject paint(PaintSpec paint) {
        JsonObject out = new JsonObject();
        for (String slot : PaintSpec.SLOTS) {
            paint.color(slot).ifPresent(rgb -> out.addProperty(slot, ExportJson.hex(rgb)));
        }
        return out;
    }

    static JsonObject check(BuildCheck check) {
        JsonObject out = new JsonObject();
        out.add("problems", ExportJson.strings(check.problems()));
        out.add("warnings", ExportJson.strings(check.warnings()));
        return out;
    }

    static final String[][] ANALYSIS_LABELS = {
            {"mixed_flight_time_min", "propwash.content.workbench.stat.flight_time"},
            {"hover_flight_time_min", "propwash.content.workbench.details.hover_time"},
            {"cruise_flight_time_min", "propwash.content.workbench.details.cruise_time"},
            {"mixed_flight_time_detail", "propwash.content.workbench.details.mixed_time"},
            {"aggressive_flight_time_min", "propwash.content.workbench.details.aggressive_time"},
            {"hover_mixed_current_a", "propwash.content.workbench.details.currents"},
            {"landing_cell_voltage", "propwash.content.workbench.details.landing_voltage"},
            {"avionics_power_w", "propwash.content.workbench.details.avionics_power"},
            {"motor_temp_minute_c", "propwash.content.workbench.details.motor_temp"},
            {"mixed_motor_temp_c", "propwash.content.workbench.details.motor_temp_flight"},
            {"mass_grams", "propwash.content.workbench.stat.weight"},
            {"thrust_to_weight", "propwash.content.workbench.stat.thrust_to_weight"},
            {"hover_throttle_percent", "propwash.content.workbench.stat.hover_throttle"},
            {"top_speed_kmh", "propwash.content.workbench.stat.top_speed"},
            {"full_throttle_current_a", "propwash.content.workbench.stat.max_current"}};

    private JsonObject analysisLabelsJson() {
        JsonObject out = new JsonObject();
        for (String[] entry : ANALYSIS_LABELS) {
            JsonObject label = new JsonObject();
            label.addProperty("key", entry[1]);
            label.add("text", this.lang.both(entry[1], "analysis_labels.json", true));
            out.add(entry[0], label);
        }
        return out;
    }

    static JsonObject stats(BuildStats stats) {
        JsonObject out = new JsonObject();
        out.add("mass_grams", ExportJson.num(stats.massGrams()));
        out.add("thrust_to_weight", ExportJson.num(stats.thrustToWeight()));
        out.add("hover_throttle_percent", ExportJson.num(stats.hoverThrottlePercent()));
        out.add("hover_flight_time_min", ExportJson.num(stats.hoverFlightTimeMin()));
        out.add("cruise_flight_time_min", ExportJson.num(stats.cruiseFlightTimeMin()));
        out.add("mixed_flight_time_min", ExportJson.num(stats.mixedFlightTimeMin()));
        out.add("aggressive_flight_time_min", ExportJson.num(stats.aggressiveFlightTimeMin()));
        out.add("cruise_speed_kmh", ExportJson.num(stats.cruiseSpeedKmh()));
        out.add("hover_current_a", ExportJson.num(stats.hoverCurrentA()));
        out.add("cruise_current_a", ExportJson.num(stats.cruiseCurrentA()));
        out.add("mixed_current_a", ExportJson.num(stats.mixedCurrentA()));
        out.add("landing_cell_voltage", ExportJson.num(stats.landingCellVoltage()));
        out.add("avionics_power_w", ExportJson.num(stats.avionicsPowerW()));
        out.add("top_speed_kmh", ExportJson.num(stats.topSpeedKmh()));
        out.add("full_throttle_current_a", ExportJson.num(stats.fullThrottleCurrentA()));
        out.add("esc_load_percent", ExportJson.num(stats.escLoadPercent()));
        out.add("battery_load_percent", ExportJson.num(stats.batteryLoadPercent()));
        out.add("motor_response_ms", ExportJson.num(stats.motorResponseMs()));
        out.add("crash_speed_ms", ExportJson.num(stats.crashSpeedMs()));
        out.add("sustained_motor_temp_c", ExportJson.num(stats.sustainedMotorTempC()));
        out.add("motor_temp_minute_c", ExportJson.num(stats.motorTempMinuteC()));
        out.add("mixed_motor_temp_c", ExportJson.num(stats.mixedMotorTempC()));
        out.add("props_in_view_percent", ExportJson.num(stats.propsInViewPercent()));
        out.add("warnings", ExportJson.strings(stats.warnings()));
        return out;
    }

    static JsonObject cameraView(CameraView view) {
        JsonObject out = new JsonObject();
        out.add("lens_mm", vec(view.lensMm()));
        out.add("uptilt_deg", ExportJson.num(view.uptiltDeg()));
        out.add("uptilt_min_deg", ExportJson.num(view.uptiltMinDeg()));
        out.add("uptilt_max_deg", ExportJson.num(view.uptiltMaxDeg()));
        out.add("horizontal_fov_deg", ExportJson.num(view.horizontalFovDeg()));
        out.add("vertical_fov_deg", ExportJson.num(view.verticalFovDeg()));
        out.add("aspect", ExportJson.num(view.aspect()));
        out.addProperty("projection", "rectilinear");
        JsonArray props = new JsonArray();
        for (CameraView.PropDisc disc : view.props()) {
            JsonObject entry = new JsonObject();
            entry.add("center_mm", vec(disc.centerMm()));
            entry.add("radius_mm", ExportJson.num(disc.radiusMm()));
            entry.add("hub_radius_mm", ExportJson.num(disc.hubRadiusMm()));
            props.add(entry);
        }
        out.add("props", props);
        out.add("props_in_view_percent", ExportJson.num(view.propsInViewPercent()));
        return out;
    }

    private static JsonArray vec(Vec3Mm v) {
        return ExportJson.nums(v.x(), v.y(), v.z());
    }

    private static JsonElement cameraView(BuildSpec build) {
        return PropwashApi.builds().cameraView(build).<JsonElement>map(WebExport::cameraView).orElse(JsonNull.INSTANCE);
    }

    private JsonElement presetName(DronePreset preset) {
        if (preset.data().name().isPresent()) {
            JsonObject out = new JsonObject();
            for (String language : LangTable.LANGUAGES) {
                out.addProperty(language, preset.data().name().get());
            }
            return out;
        }
        return this.lang.both(preset.translationKey(), "presets.json#" + preset.id(), true);
    }

    private JsonArray presetsJson(List<DronePreset> presets) {
        this.counts.put("preset", presets.size());
        JsonArray out = new JsonArray();
        for (DronePreset preset : presets) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", preset.id().toString());
            entry.add("name", this.presetName(preset));
            entry.addProperty("creative", preset.creativeListed());
            entry.add("build", build(preset.data().build()));
            entry.add("paint", paint(preset.data().paint()));
            entry.add("tune", tune(preset.data().tune()));
            try {
                entry.addProperty("sharecode", ShareCodec.encode(ShareContent.of(preset.data(), PRESET_LAYERS, Optional.empty())));
            } catch (IllegalArgumentException e) {
                entry.add("sharecode", JsonNull.INSTANCE);
                this.problems.error("presets.json#" + preset.id(), "kein Sharecode: " + e.getMessage());
            }
            BuildCheck check = PropwashApi.builds().check(preset.data().build());
            entry.add("check", check(check));
            Optional<BuildStats> stats = PropwashApi.builds().analyze(preset.data().build());
            entry.add("analysis", stats.<JsonElement>map(WebExport::stats).orElse(JsonNull.INSTANCE));
            entry.add("camera_view", cameraView(preset.data().build()));
            if (!check.buildable()) {
                this.problems.warning("presets.json#" + preset.id(), "Preset nicht baubar: " + check.unknownParts() + " " + check.problems());
            }
            out.add(entry);
        }
        return out;
    }

    private void analysis(List<DronePreset> presets) {
        for (DronePreset preset : presets) {
            BuildSpec base = preset.data().build();
            JsonObject out = new JsonObject();
            out.addProperty("preset", preset.id().toString());
            out.add("camera_view", cameraView(base));
            JsonArray variants = new JsonArray();
            for (PartDefinition part : this.parts) {
                BuildSpec variant;
                if (part instanceof AccessoryDefinition accessory) {
                    variant = base.withAccessory(accessory.slot(), part.id());
                } else {
                    variant = base.withPart(part.kind(), part.id());
                }
                BuildCheck check = PropwashApi.builds().check(variant);
                if (!check.buildable()) {
                    continue;
                }
                Optional<BuildStats> stats = PropwashApi.builds().analyze(variant);
                if (stats.isEmpty()) {
                    continue;
                }
                JsonObject entry = new JsonObject();
                entry.addProperty("category", part.kind().key());
                entry.addProperty("id", part.id().toString());
                entry.add("check", check(check));
                entry.add("analysis", stats(stats.get()));
                entry.add("camera_view", cameraView(variant));
                variants.add(entry);
            }
            out.add("variants", variants);
            this.write("analysis/" + preset.id().getNamespace() + "/" + preset.id().getPath() + ".json", out);
        }
    }

    private JsonObject specJson() {
        JsonObject out = new JsonObject();
        out.addProperty("format", ShareTables.FORMAT);
        out.addProperty("prefix", ShareTables.PREFIX);
        out.addProperty("revision", ShareTables.REVISION);
        out.addProperty("alphabet", ShareTables.ALPHABET);
        out.addProperty("group_length", ShareTables.GROUP_LENGTH);
        out.addProperty("frame_version", ShareTables.FRAME_VERSION);
        out.addProperty("flag_deflate", ShareTables.FLAG_DEFLATE);
        out.addProperty("checksum", "crc32-ieee, big-endian");
        JsonObject tolerant = new JsonObject();
        tolerant.addProperty("O", "0");
        tolerant.addProperty("I", "1");
        tolerant.addProperty("L", "1");
        out.add("tolerant", tolerant);
        JsonArray layers = new JsonArray();
        for (ShareLayer layer : ShareLayer.values()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("bit", layer.bit());
            entry.addProperty("key", layer.key());
            entry.addProperty("default", ShareLayer.DEFAULT_EXPORT.contains(layer));
            entry.add("name", this.lang.both(layer.translationKey(), "sharecode/spec.json", true));
            layers.add(entry);
        }
        out.add("layers", layers);
        JsonArray namespaces = new JsonArray();
        for (int i = 0; i < ShareTables.NAMESPACES.size(); i++) {
            JsonObject entry = new JsonObject();
            entry.addProperty("ns", i);
            entry.add("namespace", i == 0 ? JsonNull.INSTANCE : new JsonPrimitive(ShareTables.NAMESPACES.get(i)));
            namespaces.add(entry);
        }
        out.add("namespaces", namespaces);
        JsonArray accessorySlots = new JsonArray();
        for (int i = 0; i < ShareTables.ACCESSORY_SLOTS.size(); i++) {
            JsonObject entry = new JsonObject();
            entry.addProperty("index", i);
            entry.addProperty("key", ShareTables.ACCESSORY_SLOTS.get(i).key());
            accessorySlots.add(entry);
        }
        out.add("accessory_slots", accessorySlots);
        JsonArray paintBits = new JsonArray();
        for (int i = 0; i < ShareTables.PAINT_SLOTS.size(); i++) {
            JsonObject entry = new JsonObject();
            entry.addProperty("bit", i);
            entry.addProperty("slot", ShareTables.PAINT_SLOTS.get(i));
            paintBits.add(entry);
        }
        out.add("paint_bits", paintBits);
        JsonArray tuneKeys = new JsonArray();
        for (int i = 0; i < ShareTables.TUNE_KEYS.size(); i++) {
            JsonObject entry = new JsonObject();
            entry.addProperty("index", i + 1);
            entry.addProperty("key", ShareTables.TUNE_KEYS.get(i));
            tuneKeys.add(entry);
        }
        out.add("tune_keys", tuneKeys);
        JsonObject limits = new JsonObject();
        limits.addProperty("input_chars", ShareTables.MAX_INPUT_CHARS);
        limits.addProperty("frame_bytes", ShareTables.MAX_FRAME_BYTES);
        limits.addProperty("min_frame_bytes", ShareTables.MIN_FRAME_BYTES);
        limits.addProperty("payload_bytes", ShareTables.MAX_PAYLOAD_BYTES);
        limits.addProperty("tune_entries", ShareTables.MAX_TUNE_ENTRIES);
        limits.addProperty("tune_key_chars", ShareTables.MAX_TUNE_KEY_CHARS);
        limits.addProperty("osd_chars", ShareTables.MAX_OSD_CHARS);
        limits.addProperty("name_chars", ShareTables.MAX_NAME_CHARS);
        limits.addProperty("path_chars", ShareTables.MAX_PATH_CHARS);
        limits.addProperty("namespace_chars", ShareTables.MAX_NAMESPACE_CHARS);
        limits.addProperty("id_chars", "[a-z0-9_.-]");
        out.add("limits", limits);
        JsonArray statuses = new JsonArray();
        for (ShareStatus status : ShareStatus.values()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("key", status.name());
            entry.add("text", this.lang.both(status.translationKey(), "sharecode/spec.json", true));
            statuses.add(entry);
        }
        out.add("statuses", statuses);
        JsonArray aliases = new JsonArray();
        new TreeMap<>(PartAliases.all()).forEach((from, to) -> {
            JsonObject entry = new JsonObject();
            entry.addProperty("from", from.toString());
            entry.addProperty("to", to.toString());
            aliases.add(entry);
        });
        out.add("aliases", aliases);
        out.add("pw2", pw2Spec());
        return out;
    }

    private static JsonObject pw2Spec() {
        JsonObject out = new JsonObject();
        out.addProperty("format", Pw2Tables.FORMAT);
        out.addProperty("prefix", Pw2Tables.PREFIX);
        out.addProperty("revision", Pw2Tables.REVISION);
        out.addProperty("alphabet", Pw2Tables.ALPHABET);
        out.addProperty("text_alphabet", Pw2Tables.TEXT_ALPHABET);
        out.addProperty("checksum", "crc32-ieee, low 16 bits, big-endian");
        out.addProperty("unknown_part", "?<kind>.<number>");
        JsonObject params = new JsonObject();
        params.addProperty("part", Pw2Tables.K_PART);
        params.addProperty("color", Pw2Tables.K_COLOR);
        params.addProperty("paint_slots", Pw2Tables.K_PAINT_SLOTS);
        params.addProperty("tune_count", Pw2Tables.K_TUNE_COUNT);
        params.addProperty("tune_gap", Pw2Tables.K_TUNE_GAP);
        params.addProperty("value", Pw2Tables.K_VALUE);
        params.addProperty("text", Pw2Tables.K_TEXT);
        params.addProperty("osd_bytes", Pw2Tables.K_OSD_BYTES);
        params.addProperty("extension_bits", Pw2Tables.K_EXTENSION_BITS);
        out.add("params", params);
        JsonObject parts = new JsonObject();
        for (PartKind kind : Pw2Tables.KINDS) {
            parts.add(Pw2Tables.kindKey(kind), ExportJson.strings(Pw2Tables.parts(kind)));
        }
        out.add("parts", parts);
        JsonArray colors = new JsonArray();
        for (int rgb : Pw2Tables.COLORS) {
            colors.add(String.format(Locale.ROOT, "#%06x", rgb));
        }
        out.add("colors", colors);
        out.add("tune_keys", ExportJson.strings(Pw2Tables.TUNE_KEYS));
        out.addProperty("osd_dictionary", new String(Pw2Tables.OSD_DICTIONARY, StandardCharsets.UTF_8));
        return out;
    }

    private JsonArray vectorsJson(List<DronePreset> presets) {
        JsonArray out = new JsonArray();
        for (ShareVectors.Vector vector : ShareVectors.all()) {
            ShareDecoded decoded = ShareCodec.decode(vector.code());
            for (String error : ShareVectors.check(vector, decoded)) {
                this.problems.error("sharecode/vectors.json#" + vector.name(), error);
            }
            out.add(this.vector(vector.name(), vector.code(), vector.compression().key(), vector.format(), decoded));
        }
        for (DronePreset preset : presets) {
            ShareContent full = ShareContent.of(preset.data(), PRESET_LAYERS, Optional.empty());
            ShareContent partsOnly = ShareContent.of(preset.data().build());
            for (Map.Entry<String, Map.Entry<ShareContent, ShareCompression>> entry : Map.of(
                    "preset:" + preset.id(), Map.entry(full, ShareCompression.AUTO),
                    "preset_parts:" + preset.id(), Map.entry(partsOnly, ShareCompression.NEVER)).entrySet().stream()
                    .sorted(Map.Entry.comparingByKey()).toList()) {
                ShareContent content = entry.getValue().getKey();
                String code;
                try {
                    code = ShareCodec.encodePw1(content, entry.getValue().getValue());
                } catch (IllegalArgumentException e) {
                    this.problems.error("sharecode/vectors.json#" + entry.getKey(), "nicht kodierbar: " + e.getMessage());
                    continue;
                }
                ShareDecoded decoded = ShareCodec.decode(code);
                if (!decoded.ok() || !decoded.require().equals(content)) {
                    this.problems.error("sharecode/vectors.json#" + entry.getKey(), "Rundreise weicht ab: " + decoded);
                }
                if (!decoded.unknownParts().isEmpty() || !decoded.wrongKind().isEmpty()) {
                    this.problems.error("sharecode/vectors.json#" + entry.getKey(), "Preset mit unbekannten Teilen: " + decoded.unknownParts());
                }
                out.add(this.vector(entry.getKey(), code, entry.getValue().getValue().key(), ShareTables.FORMAT, decoded));
            }
            for (Map.Entry<String, ShareContent> entry : List.of(Map.entry("pw2_preset:" + preset.id(), full),
                    Map.entry("pw2_preset_parts:" + preset.id(), partsOnly))) {
                String code = ShareCodec.encode(entry.getValue());
                ShareDecoded decoded = ShareCodec.decode(code);
                if (!decoded.ok() || !decoded.require().equals(entry.getValue())) {
                    this.problems.error("sharecode/vectors.json#" + entry.getKey(), "Rundreise weicht ab: " + decoded);
                }
                if (!decoded.unknownParts().isEmpty() || !decoded.wrongKind().isEmpty()) {
                    this.problems.error("sharecode/vectors.json#" + entry.getKey(), "Preset mit unbekannten Teilen: " + decoded.unknownParts());
                }
                out.add(this.vector(entry.getKey(), code, ShareCompression.AUTO.key(), Pw2Tables.FORMAT, decoded));
            }
        }
        this.vectors = out.size();
        return out;
    }

    private JsonObject vector(String name, String code, String compression, String format, ShareDecoded decoded) {
        JsonObject out = new JsonObject();
        out.addProperty("name", name);
        out.addProperty("format", format);
        out.addProperty("code", code);
        out.addProperty("compression", compression);
        out.addProperty("status", decoded.status().name());
        JsonArray layers = new JsonArray();
        decoded.layers().forEach(layer -> layers.add(layer.key()));
        out.add("layers", layers);
        if (decoded.ok()) {
            ShareContent content = decoded.require();
            JsonObject body = new JsonObject();
            body.add("build", build(content.build()));
            body.add("paint", content.paint().<JsonElement>map(WebExport::paint).orElse(JsonNull.INSTANCE));
            body.add("tune", content.tune().<JsonElement>map(WebExport::tune).orElse(JsonNull.INSTANCE));
            body.add("osd", content.osd().<JsonElement>map(osd -> {
                JsonObject object = new JsonObject();
                object.addProperty("version", osd.version());
                object.addProperty("json", osd.json());
                return object;
            }).orElse(JsonNull.INSTANCE));
            body.add("name", ExportJson.str(content.name()));
            out.add("decoded", body);
        } else {
            out.add("decoded", JsonNull.INSTANCE);
        }
        out.add("skipped", ExportJson.strings(decoded.skipped()));
        JsonArray unknown = new JsonArray();
        decoded.unknownParts().forEach(id -> unknown.add(ShareTables.display(id)));
        out.add("unknown_parts", unknown);
        out.add("wrong_kind", ExportJson.ids(decoded.wrongKind()));
        JsonArray renamed = new JsonArray();
        for (PartAliases.Rename rename : decoded.renamed()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("from", rename.from().toString());
            entry.addProperty("to", rename.to().toString());
            renamed.add(entry);
        }
        out.add("renamed", renamed);
        if (decoded.partsKnown()) {
            BuildSpec build = decoded.require().build();
            out.add("check", check(PropwashApi.builds().check(build)));
            out.add("analysis", PropwashApi.builds().analyze(build).<JsonElement>map(WebExport::stats).orElse(JsonNull.INSTANCE));
        } else {
            out.add("check", JsonNull.INSTANCE);
            out.add("analysis", JsonNull.INSTANCE);
        }
        return out;
    }

    private JsonObject manifestJson() {
        JsonObject out = new JsonObject();
        out.addProperty("format", FORMAT);
        out.addProperty("generated_at", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
        out.addProperty("generator", JustMoreParts.MOD_ID + " " + Services.PLATFORM.modVersion(JustMoreParts.MOD_ID).orElse("?"));
        out.addProperty("minecraft", Services.PLATFORM.modVersion("minecraft").orElse("26.3"));
        JsonObject mods = new JsonObject();
        mods.addProperty(PropwashApi.MOD_ID, Services.PLATFORM.modVersion(PropwashApi.MOD_ID).orElse("?"));
        mods.addProperty(JustMoreParts.MOD_ID, Services.PLATFORM.modVersion(JustMoreParts.MOD_ID).orElse("?"));
        out.add("mods", mods);
        out.addProperty("propwash_api", PropwashApi.API_VERSION);
        JsonObject share = new JsonObject();
        share.addProperty("prefix", ShareTables.PREFIX);
        share.addProperty("revision", ShareTables.REVISION);
        share.addProperty("default_format", Pw2Tables.FORMAT);
        share.addProperty("pw2_revision", Pw2Tables.REVISION);
        out.add("sharecode", share);
        out.add("languages", ExportJson.strings(LangTable.LANGUAGES));
        JsonObject countJson = new JsonObject();
        this.counts.forEach(countJson::addProperty);
        out.add("counts", countJson);
        out.addProperty("fingerprint", PropwashApi.parts().fingerprint());
        List<String> files = new ArrayList<>(this.written);
        files.add("manifest.json");
        files.sort(String::compareTo);
        out.add("files", ExportJson.strings(files));
        return out;
    }
}
