package de.lardtom.justmoreparts.frame;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.lardtom.justmoreparts.bench.Bench;
import de.lardtom.justmoreparts.bench.PartFiles;
import de.lardtom.justmoreparts.preset.JmpPresets;
import de.lardtom.justmoreparts.video.VideoModule;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.BuildCheck;
import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.build.BuildStats;
import de.lardtom.propwash.api.part.AccessorySlot;
import de.lardtom.propwash.api.part.BatteryDefinition;
import de.lardtom.propwash.api.part.FrameDefinition;
import de.lardtom.propwash.api.part.FrameRole;
import de.lardtom.propwash.api.part.MotorDefinition;
import de.lardtom.propwash.api.part.PartDefinition;
import de.lardtom.propwash.api.part.PartKind;
import de.lardtom.propwash.api.part.VideoDefinition;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FrameBenchTest {
    public static final String STACK = "propwash:stack_30";
    public static final String MOTOR = "propwash:m2207_1750";
    public static final String PROP = "propwash:p51_3b";
    public static final String VIDEO = "propwash:digital_hd";
    public static final String BATTERY = "propwash:b6s_1300";

    private static boolean twins;

    @BeforeAll
    public static void register() {
        PartFiles.registerAll();
        if (!twins) {
            twins = true;
            twin("tp_35_stretched", "tp_35_truex");
            twin("fs_5_squashed", "fs_5_squashed_truex");
            twin("race_5_stretched", "race_5_stretched_truex");
            twin("hframe_5", "hframe_5_truex");
            twin("deadcat_5", "deadcat_5_truex");
        }
    }

    record Corridor(double massMin, double massMax, double twMin, double twMax, double hoverMin, double hoverMax, double hoverTimeMin,
                    double hoverTimeMax, double cruiseMin, double cruiseMax, double topMin, double topMax, double motorMax, double propsMax) {
    }

    static Corridor corridor(JmpPresets.SizeClass size) {
        return switch (size) {
            case WHOOP -> new Corridor(20, 60, 3.5, 7, 25, 50, 2.5, 9, 2.3, 9, 35, 85, 60, 20);
            case TOOTHPICK -> new Corridor(50, 230, 5, 9, 20, 35, 6, 16, 4, 13, 70, 110, Double.NaN, 15);
            case CINEWHOOP -> new Corridor(190, 420, 4, 7, 23, 35, 4.5, 13, 3.5, 11, 80, 150, 100, 25);
            case FIVE_INCH -> new Corridor(400, 560, 10, 15, 16, 25, 8, 14.5, 5.5, 11, 125, 190, Double.NaN, 15);
            case LONG_RANGE -> new Corridor(440, 1400, 3.5, 11, 20, 50, 12, 48, 8, 33, 90, 130, Double.NaN, 20);
            case X_CLASS -> new Corridor(1600, 3400, 5, 9, 25, 40, 12, 26, 8, 18, 100, 140, Double.NaN, 20);
        };
    }

    static final Map<String, Corridor> OWN_CORRIDORS = Map.of(
            "toothpick35_6s", new Corridor(50, 230, 5, 9, 20, 35, 6, 16, 4, 13, 100, 130, Double.NaN, 15),
            "lr7_8s", new Corridor(800, 950, 9, 13, 18, 25, 15, 23, 9, 14, 130, 170, Double.NaN, 10));

    static Corridor corridor(JmpPresets.Entry entry) {
        return OWN_CORRIDORS.getOrDefault(entry.path(), corridor(entry.sizeClass()));
    }

    static Set<String> allowedWarnings(JmpPresets.Entry entry) {
        if (entry.path().equals("race5_6s")) {
            return Set.of("heavy_sag");
        }
        if (entry.battery().endsWith("_lion")) {
            return Set.of("battery_weak", "heavy_sag", "battery_overload");
        }
        return Set.of();
    }

    @Test
    public void presetsStayInsideTheTargetCorridors() {
        List<String> failures = new ArrayList<>();
        System.out.println(Bench.header());
        for (JmpPresets.Entry entry : JmpPresets.ALL) {
            BuildSpec spec = entry.build();
            System.out.println(Bench.row(entry.path(), spec));
            List<String> own = violations(entry, spec);
            if (!own.isEmpty()) {
                failures.add(entry.path() + " " + own);
            }
        }
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }

    static List<String> violations(JmpPresets.Entry entry, BuildSpec spec) {
        List<String> out = new ArrayList<>();
        BuildCheck check = Bench.check(spec);
        if (!check.known() || !check.buildable()) {
            out.add("nicht baubar " + check.unknownParts() + check.problems());
            return out;
        }
        BuildStats s = Bench.stats(spec);
        Corridor c = corridor(entry);
        range(out, "Masse", s.massGrams(), c.massMin(), c.massMax());
        range(out, "T/W", s.thrustToWeight(), c.twMin(), c.twMax());
        range(out, "Schwebegas", s.hoverThrottlePercent(), c.hoverMin(), c.hoverMax());
        range(out, "Schweben", s.hoverFlightTimeMin(), c.hoverTimeMin(), c.hoverTimeMax());
        range(out, "Reise", s.cruiseFlightTimeMin(), c.cruiseMin(), c.cruiseMax());
        range(out, "Topspeed", s.topSpeedKmh(), c.topMin(), c.topMax());
        double motorMax = Double.isNaN(c.motorMax()) ? motor(spec).thermal().derateStartC() : c.motorMax();
        if (!(s.sustainedMotorTempC() < motorMax)) {
            out.add(String.format(Locale.ROOT, "Motor %.1f °C ≥ %.0f", s.sustainedMotorTempC(), motorMax));
        }
        double propsMax = entry.frame().equals("deadcat_5") || entry.frame().equals("hframe_5") ? 1.0 : c.propsMax();
        if (!(s.propsInViewPercent() < propsMax)) {
            out.add(String.format(Locale.ROOT, "Props im Bild %.1f %% ≥ %.0f", s.propsInViewPercent(), propsMax));
        }
        Set<String> warnings = new LinkedHashSet<>(check.warnings());
        warnings.addAll(s.warnings());
        warnings.removeAll(allowedWarnings(entry));
        if (!warnings.isEmpty()) {
            out.add("Warnungen " + warnings);
        }
        return out;
    }

    @Test
    public void everyFrameAndVideoHasABuildableReference() {
        Set<String> frames = new LinkedHashSet<>(FrameModule.FRAMES);
        Set<String> videos = new LinkedHashSet<>(VideoModule.VIDEOS);
        for (JmpPresets.Entry entry : JmpPresets.ALL) {
            frames.remove(entry.frame());
            videos.remove(entry.video());
        }
        for (BuildSpec spec : List.of(whoop65Lite(), toothpick25(), fs("fs_5_bash"), lr6("analog_lr_1600"))) {
            BuildCheck check = Bench.check(spec);
            assertTrue(check.known() && check.buildable(), spec + " " + check.problems());
            frames.remove(spec.frame().getPath());
            videos.remove(spec.video().getPath());
        }
        assertTrue(frames.isEmpty(), "Rahmen ohne Referenzaufbau: " + frames);
        assertTrue(videos.isEmpty(), "Videosysteme ohne Referenzaufbau: " + videos);
        System.out.println(Bench.row("tp_25_x Referenz 4S", toothpick25()));
        System.out.println(Bench.row("whoop_65_lite Referenz", whoop65Lite()));
        assertTrue(Bench.check(toothpick25()).warnings().isEmpty() && Bench.stats(toothpick25()).warnings().isEmpty());
        BuildStats tp25 = Bench.stats(toothpick25());
        Corridor c = corridor(JmpPresets.SizeClass.TOOTHPICK);
        List<String> out = new ArrayList<>();
        range(out, "Masse", tp25.massGrams(), c.massMin(), c.massMax());
        range(out, "T/W", tp25.thrustToWeight(), c.twMin(), c.twMax());
        range(out, "Schwebegas", tp25.hoverThrottlePercent(), c.hoverMin(), c.hoverMax());
        range(out, "Topspeed", tp25.topSpeedKmh(), 95, 105);
        assertTrue(out.isEmpty(), "tp_25_x " + out);
    }

    @Test
    public void shapeClaimsHoldWithEqualParts() {
        Map<String, BuildStats> s = new LinkedHashMap<>();
        for (String frame : List.of("propwash:freestyle_5", "fs_5_bash", "fs_5_squashed", "race_5_stretched", "deadcat_5", "hframe_5")) {
            s.put(frame, Bench.stats(fs(frame)));
            System.out.println(Bench.row("5″ " + frame, fs(frame)) + String.format(Locale.ROOT, "  Aufprall %.1f  Nick/Roll %.2f",
                    Bench.stats(fs(frame)).crashSpeedMs(), pitchToRoll(fs(frame))));
        }
        BuildStats standard = s.get("propwash:freestyle_5");
        assertEquals(142.0, standard.topSpeedKmh(), 1.0);

        BuildStats bash = s.get("fs_5_bash");
        assertTrue(bash.crashSpeedMs() >= 10.0 && standard.crashSpeedMs() <= 8.0, "Bash: 6-mm-Arme brechen erst ab 10 m/s");
        assertEquals(13.0, bash.massGrams() - standard.massGrams(), 0.05);
        assertTrue(lt(bash.topSpeedKmh(), standard.topSpeedKmh()), "Bash: mehr Luftwiderstand");
        assertEquals(1.4, frame("fs_5_bash").basics().durability(), 1e-9);

        BuildStats squashed = s.get("fs_5_squashed");
        assertTrue(ratio("fs_5_squashed") > ratio("jmp_test:fs_5_squashed_truex") + 0.15, "Squashed: Flips spritziger als Rolls");
        assertTrue(lt(squashed.propsInViewPercent(), standard.propsInViewPercent()), "Squashed: weniger Props im Bild");
        assertEquals(3.0, squashed.propsInViewPercent(), 0.6, "Squashed: 3 statt 8 % Props");
        assertEquals(8.4, standard.propsInViewPercent(), 0.6, "True-X: 8 % Props");

        BuildStats race = s.get("race_5_stretched");
        assertEquals(171.0, race.topSpeedKmh(), 3.0);
        assertTrue(ratio("race_5_stretched") < ratio("jmp_test:race_5_stretched_truex") - 0.15, "Stretched: Rolls spritziger, Nick träger");
        assertTrue(lt(race.massGrams(), standard.massGrams()));
        assertTrue(race.crashSpeedMs() <= 6.5 && lt(race.crashSpeedMs(), standard.crashSpeedMs()), "Race: 4-mm-Arme brechen früher");
        assertTrue(race.propsInViewPercent() > standard.propsInViewPercent() + 3.0, "Stretched: Vorderprops näher an der Bildmitte, "
                + race.propsInViewPercent() + " gegen " + standard.propsInViewPercent() + " %");

        BuildStats deadcat = s.get("deadcat_5");
        assertTrue(deadcat.propsInViewPercent() < 1.0 && gt(standard.propsInViewPercent(), deadcat.propsInViewPercent()));
        assertEquals(7.0, deadcat.massGrams() - standard.massGrams(), 0.05);
        assertTrue(lt(deadcat.topSpeedKmh(), standard.topSpeedKmh()), "Deadcat: etwas langsamer");
        assertTrue(Math.abs(ratio("deadcat_5") - ratio("jmp_test:deadcat_5_truex")) > 0.1, "Deadcat: Roll und Nick unterschiedlich");

        BuildStats hframe = s.get("hframe_5");
        assertTrue(hframe.propsInViewPercent() < 1.0);
        assertEquals(124.0, hframe.topSpeedKmh(), 3.0);
        assertTrue(gt(hframe.massGrams(), standard.massGrams()));
        assertTrue(ratio("hframe_5") > ratio("jmp_test:hframe_5_truex") + 0.15, "H-Frame: Flips spritziger, Rolls träger");

        double stretchedToothpick = pitchToRoll(Bench.spec("tp_35_stretched", "aio_tp_6s_20a", "m1505_2500", "p89_30_3b_pc", "digital_micro",
                "b6s_750"));
        double trueToothpick = pitchToRoll(Bench.spec("jmp_test:tp_35_truex", "aio_tp_6s_20a", "m1505_2500", "p89_30_3b_pc",
                "digital_micro", "b6s_750"));
        assertTrue(stretchedToothpick < trueToothpick - 0.08, "Toothpick 3,5″: mehr Roll-Autorität, Nick träger");
    }

    @Test
    public void classClaimsHold() {
        BuildStats whoop = Bench.stats(builtinWhoop("propwash:whoop_65"));
        BuildStats lite = Bench.stats(builtinWhoop("whoop_65_lite"));
        assertEquals(0.8, whoop.massGrams() - lite.massGrams(), 0.05);
        assertTrue(gt(lite.hoverFlightTimeMin(), whoop.hoverFlightTimeMin()));
        assertTrue(lite.crashSpeedMs() <= 10.0 && whoop.crashSpeedMs() >= 14.0);
        assertEquals(0.7, frame("whoop_65_lite").basics().durability(), 1e-9);

        BuildStats w75 = preset("whoop75_1s");
        BuildStats w85 = preset("whoop85_2s");
        assertTrue(gt(w75.thrustToWeight(), whoop.thrustToWeight()), "Whoop 75: mehr Schub als 65 mm");
        assertTrue(lt(w75.propsInViewPercent(), whoop.propsInViewPercent()), "Whoop 75: weniger Ducts im Bild");
        assertEquals(2, frame("whoop_75_duct").batteryFit().maxCells());
        assertTrue(gt(w85.topSpeedKmh(), w75.topSpeedKmh()), "Whoop 85: schneller");

        BuildStats tp2 = preset("toothpick2_2s");
        assertTrue(tp2.propsInViewPercent() > 3.0 && tp2.propsInViewPercent() < 5.0, "Toothpick 2″: rund 4 % Props");
        assertTrue(tp2.crashSpeedMs() <= 9.0);
        assertTrue(Math.abs(tp2.topSpeedKmh() / w85.topSpeedKmh() - 1.0) < 0.1, "Toothpick 2″: etwa so schnell wie der 85-mm-Whoop");
        BuildStats tp3 = preset("toothpick3_4s");
        assertTrue(tp3.massGrams() < 150.0);
        assertTrue(tp3.propsInViewPercent() > 4.5 && tp3.propsInViewPercent() < 6.5, "Toothpick 3″: rund 5 % Props");
        assertTrue(gt(preset("toothpick35_6s").propsInViewPercent(), tp3.propsInViewPercent()), "Toothpick 3,5″ Stretched: mehr Props im Bild");

        BuildStats cw25 = preset("cinewhoop25_4s");
        BuildStats cw25cam = Bench.stats(Bench.withAccessory(JmpPresets.byPath("cinewhoop25_4s").build(), AccessorySlot.TOP, "propwash:action_cam_light"));
        assertTrue(cw25.crashSpeedMs() >= 12.0, "Ducts prallen ab");
        assertTrue(cw25.topSpeedKmh() >= 80.0 && cw25.topSpeedKmh() <= 90.0, "Cinewhoop 2,5″: rund 85 km/h");
        assertTrue(cw25.hoverFlightTimeMin() >= 10.5 && cw25.hoverFlightTimeMin() <= 11.5);
        assertTrue(cw25cam.thrustToWeight() > 3.0 && cw25cam.hoverThrottlePercent() < 60.0, "trägt eine leichte Action-Cam");
        BuildStats cw35cam = Bench.stats(Bench.withAccessory(JmpPresets.byPath("cinewhoop35_6s").build(), AccessorySlot.TOP, "propwash:action_cam_heavy"));
        assertTrue(cw35cam.thrustToWeight() > 2.5);
        BuildStats cw35 = preset("cinewhoop35_6s");
        double camLoss = 1.0 - cw35cam.hoverFlightTimeMin() / cw35.hoverFlightTimeMin();
        assertTrue(camLoss >= 0.25 && camLoss <= 0.35, "schwere Cam kostet rund 30 % Schwebezeit: " + camLoss);
        assertTrue(cw35.topSpeedKmh() >= 85.0 && cw35.topSpeedKmh() <= 95.0, "Cinewhoop 3,5″: rund 90 km/h");

        BuildStats lr4 = preset("lr4_6s_lion");
        double lr4Battery = PropwashApi.parts().get(BatteryDefinition.class, JmpPresets.byPath("lr4_6s_lion").build().battery()).orElseThrow().massGrams();
        assertTrue(lr4.massGrams() - lr4Battery < 250.0, "LR 4″: unter 250 g ohne Akku");
        assertTrue(lr4.hoverFlightTimeMin() > 32.0 && lr4.hoverFlightTimeMin() < 33.5);
        assertTrue(lr4.thrustToWeight() > 4.3 && lr4.thrustToWeight() < 4.8);
        BuildStats lr6 = preset("lr6_6s");
        BuildStats lr7 = preset("lr7_deadcat_lion");
        assertTrue(lr6.hoverFlightTimeMin() > 16.0 && lr6.thrustToWeight() > 9.5 && lr6.thrustToWeight() < 11.0);
        assertTrue(gt(lr6.hoverFlightTimeMin(), preset("bash5_6s").hoverFlightTimeMin()), "LR 6″ effizienter als 5″");
        assertTrue(lt(lr6.hoverFlightTimeMin(), lr7.hoverFlightTimeMin()), "LR 6″ weniger Reichweite als 7″");
        BuildStats lr7standard = Bench.stats(JmpPresets.byPath("lr7_deadcat_lion").build().withPart(PartKind.FRAME,
                Identifier.parse("propwash:long_range_7")));
        assertTrue(lr7.propsInViewPercent() > 1.0 && lr7.propsInViewPercent() < 3.0 && lr7standard.propsInViewPercent() > 7.5
                && lr7standard.propsInViewPercent() < 9.5, "Deadcat LR 7″: 2 statt 8 % Props (Propspitzen an den Bildrändern), Deadcat "
                + lr7.propsInViewPercent() + ", Standard " + lr7standard.propsInViewPercent());
        assertTrue(lr7.hoverFlightTimeMin() > 30.0);
        assertEquals(15.0, lr7.massGrams() - lr7standard.massGrams(), 0.05);
        BuildStats lr78 = preset("lr7_8s");
        assertEquals(8, frame("lr_7_deadcat").batteryFit().maxCells());
        assertTrue(Bench.check(JmpPresets.byPath("lr7_8s").build()).warnings().isEmpty() && lr78.warnings().isEmpty(), "7″ auf 8S ohne Warnung");
        assertTrue(lr78.thrustToWeight() > 1.8 * lr7.thrustToWeight() && lr78.topSpeedKmh() > lr7.topSpeedKmh() + 30.0, "7″ 8S: Punch statt Reichweite");
        assertTrue(lt(lr78.hoverFlightTimeMin(), lr7.hoverFlightTimeMin()));
        assertTrue(lr78.propsInViewPercent() > 1.0 && lr78.propsInViewPercent() < 3.0, "Deadcat LR 7″ · 8S: Propspitzen an den Bildrändern, "
                + lr78.propsInViewPercent() + " %");
        BuildStats lr8 = preset("lr8_6s_lion");
        assertTrue(lr8.hoverFlightTimeMin() > 40.0);
        assertTrue(gt(lr8.motorResponseMs(), lr6.motorResponseMs()), "LR 8″ träge");

        BuildStats x10 = preset("xclass10_6s");
        BuildStats x12 = preset("xclass12_8s");
        BuildStats x13 = preset("xclass13_8s");
        assertTrue(x10.thrustToWeight() > 8.0);
        assertEquals(1.0 / 3.0, frame("xclass_10").massGrams() / frame("xclass_13").massGrams(), 0.02);
        assertEquals(470.0, frame("xclass_13").massGrams() - frame("xclass_12").massGrams(), 1e-9);
        assertTrue(x12.thrustToWeight() * x12.massGrams() < x13.thrustToWeight() * x13.massGrams(), "12″ weniger Schub als 13″");
        assertTrue(frame("xclass_12").wheelbaseMm() < 800 && frame("xclass_10").wheelbaseMm() < 800 && frame("xclass_13").wheelbaseMm() >= 800);
        assertTrue(frame("xclass_12").motorMounts().size() == 2);
        assertTrue(x13.crashSpeedMs() <= 6.0);
        BuildStats x13h = Bench.stats(JmpPresets.byPath("xclass13_8s").build().withPart(PartKind.FRAME,
                Identifier.parse("justmoreparts:xclass_13_h")));
        assertTrue(x13h.propsInViewPercent() < 1.0 && gt(x13.propsInViewPercent(), x13h.propsInViewPercent()));
        assertEquals(200.0, x13h.massGrams() - x13.massGrams(), 0.05);
        assertTrue(lt(x13h.topSpeedKmh(), x13.topSpeedKmh()));
        assertTrue(gt(x13.topSpeedKmh(), lr7.topSpeedKmh()) && gt(x12.topSpeedKmh(), lr7.topSpeedKmh()), "X-Class schneller als 7″-Li-Ion");
        assertEquals(1.3, frame("xclass_13_h").basics().durability(), 1e-9);
        for (String x : FrameModule.X_CLASS) {
            assertEquals(FrameRole.X_CLASS, frame(x).role());
        }
    }

    @Test
    public void videoClaimsHold() {
        Map<String, VideoDefinition> v = new LinkedHashMap<>();
        for (String path : VideoModule.VIDEOS) {
            v.put(path, PropwashApi.parts().get(VideoDefinition.class, Bench.id(path)).orElseThrow());
        }
        VideoDefinition nano = PropwashApi.parts().get(VideoDefinition.class, Bench.id("propwash:analog_nano")).orElseThrow();
        VideoDefinition race = PropwashApi.parts().get(VideoDefinition.class, Bench.id("propwash:analog_race")).orElseThrow();
        VideoDefinition hd = PropwashApi.parts().get(VideoDefinition.class, Bench.id("propwash:digital_hd")).orElseThrow();
        assertEquals(1.8, nano.massGrams() - v.get("analog_whoop_25mw").massGrams(), 1e-9);
        assertTrue(v.values().stream().allMatch(d -> d.massGrams() >= v.get("analog_whoop_25mw").massGrams()));
        assertTrue(gt(v.get("analog_race_800").rangeBlocks(), race.rangeBlocks()) && v.get("analog_race_800").latencyMs() == 11);
        assertTrue(v.get("analog_lr_1600").rangeBlocks() > v.values().stream().filter(d -> d.link() == v.get("analog_race_800").link()
                && d != v.get("analog_lr_1600")).mapToInt(VideoDefinition::rangeBlocks).max().orElse(0));
        assertEquals(20, v.get("digital_lr").latencyMs() - v.get("analog_lr_1600").latencyMs());
        assertTrue(v.get("digital_micro").massGrams() < 5.0);
        assertTrue(v.get("digital_low_latency").latencyMs() - race.latencyMs() <= 2 && v.get("digital_low_latency").imageQuality() < 1.0);
        assertEquals(6, hd.latencyMs() - v.get("digital_hd_pro").latencyMs());
        assertTrue(gt(v.get("digital_hd_pro").rangeBlocks(), hd.rangeBlocks()) && v.get("digital_hd_pro").imageQuality() >= hd.imageQuality());
        assertTrue(v.get("digital_hd_pro").massGrams() >= 20.0, "Clarity HD Pro: realistische Masse");
        assertEquals(0.8, v.get("digital_hd_pro").basics().durability(), 1e-9);
        assertTrue(v.values().stream().allMatch(d -> d.rangeBlocks() <= v.get("digital_lr").rangeBlocks()));
        BuildStats withNano = Bench.stats(builtinWhoop("propwash:whoop_65"));
        BuildStats withFirefly = Bench.stats(builtinWhoop("propwash:whoop_65").withPart(PartKind.VIDEO,
                Bench.id("analog_whoop_25mw")));
        assertEquals(withNano.propsInViewPercent(), withFirefly.propsInViewPercent(), 1.5, "Firefly: Sichtfeld stellt die Brille, nicht die Kamera");
        BuildStats standard = Bench.stats(fs("propwash:freestyle_5"));
        for (String wide : List.of("digital_micro", "digital_hd_pro")) {
            BuildStats stats = Bench.stats(fs("propwash:freestyle_5").withPart(PartKind.VIDEO, Bench.id(wide)));
            assertEquals(standard.propsInViewPercent(), stats.propsInViewPercent(), 1.5, wide + ": Props im Bild wie mit der Standard-Kamera");
            System.out.println(Bench.row("5″ mit " + wide, fs("propwash:freestyle_5").withPart(PartKind.VIDEO,
                    Bench.id(wide))));
        }
    }

    static boolean gt(double a, double b) {
        return a > b + Math.abs(b) * 0.01;
    }

    static boolean lt(double a, double b) {
        return a < b - Math.abs(b) * 0.01;
    }

    static BuildSpec fs(String frame) {
        return Bench.spec(frame, STACK, MOTOR, PROP, VIDEO, BATTERY);
    }

    static BuildSpec builtinWhoop(String frame) {
        return Bench.spec(frame, "propwash:aio_whoop", "propwash:m0802_22000", "propwash:p31_4b", "propwash:analog_nano", "propwash:b1s_300_hv");
    }

    static BuildSpec whoop65Lite() {
        return Bench.spec("whoop_65_lite", "aio_whoop_1s_6a", "m0702_23000", "p31_16_2b_pc", "analog_whoop_25mw", "b1s_250_hv");
    }

    static BuildSpec toothpick25() {
        return Bench.spec("tp_25_x", "aio_tp_4s_20a", "m12025_5500", "p65_25_3b_pc", "digital_micro", "b4s_450");
    }

    static BuildSpec lr6(String video) {
        return Bench.spec("lr_6_x", "stack_30_6s_60a", "m2507_1500", "p152_33_3b_pc", video, "b6s_1800");
    }

    static BuildStats preset(String path) {
        return Bench.stats(JmpPresets.byPath(path).build());
    }

    static FrameDefinition frame(String path) {
        return PropwashApi.parts().get(FrameDefinition.class, Bench.id(path)).orElseThrow();
    }

    static MotorDefinition motor(BuildSpec spec) {
        return PropwashApi.parts().get(MotorDefinition.class, spec.motor()).orElseThrow();
    }

    static double ratio(String frame) {
        return pitchToRoll(fs(frame));
    }

    static double pitchToRoll(BuildSpec spec) {
        BuildStats stats = Bench.stats(spec);
        return stats.pitchAuthorityRadS2() / stats.rollAuthorityRadS2();
    }

    static void range(List<String> out, String label, double value, double min, double max) {
        if (!(value >= min && value <= max)) {
            out.add(String.format(Locale.ROOT, "%s %.1f nicht in %.1f–%.1f", label, value, min, max));
        }
    }

    private static void twin(String source, String name) {
        Path file = PartFiles.root().resolve("frame").resolve(source + ".json");
        try {
            JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            json.remove("arms");
            json.remove("center_of_mass_mm");
            json.remove("mass_split");
            json.addProperty("shape", "true_x");
            Object parsed = PartFiles.parse("frame", Identifier.fromNamespaceAndPath("jmp_test", name), json);
            PropwashApi.parts().register((PartDefinition) parsed);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
