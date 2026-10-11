package de.lardtom.justmoreparts.prop;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.lardtom.justmoreparts.bench.Bench;
import de.lardtom.justmoreparts.bench.PartFiles;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.BuildCheck;
import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.build.BuildStats;
import de.lardtom.propwash.api.part.MotorDefinition;
import de.lardtom.propwash.api.part.PropDefinition;
import de.lardtom.propwash.api.part.PropMaterial;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PropBenchTest {
    record Build(String name, String frame, String stack, String motor, String video, String battery) {
        BuildSpec spec(String prop) {
            return Bench.spec(this.frame, this.stack, this.motor, prop, this.video, this.battery);
        }
    }

    record Corridor(String name, double minMass, double maxMass, double minTw, double maxTw, double minHover, double maxHover,
                    double minHoverTime, double maxHoverTime, double minCruise, double maxCruise, double minTop, double maxTop,
                    double maxMotorC, double maxPropsInView, Set<String> allowedWarnings) {
    }

    record Case(String prop, Build build, Corridor corridor, Set<String> exempt) {
    }

    record Claim(String prop, int line, BooleanSupplier check) {
    }

    static final Build STOCK_WHOOP = new Build("Whoop (Propwash)", "propwash:whoop_65", "propwash:aio_whoop", "propwash:m0802_22000",
            "propwash:analog_nano", "propwash:b1s_300_hv");
    static final Build W65 = new Build("65 mm 1S", "whoop_65_lite", "aio_whoop_1s_6a", "m0702_27000", "analog_whoop_25mw", "b1s_380_hv");
    static final Build W75 = new Build("75 mm 1S", "whoop_75_duct", "aio_whoop_1s_6a", "m0802_19000", "analog_whoop_25mw", "b1s_450_hv");
    static final Build W85 = new Build("85 mm 2S", "whoop_85_duct", "aio_whoop_2s_12a", "m1102_13500", "digital_micro", "b2s_450_hv");
    static final Build TP2 = new Build("2″ 2S", "tp_2_x", "aio_whoop_2s_12a", "m1103_11000", "analog_whoop_25mw", "b2s_450_hv");
    static final Build TP25 = new Build("2,5″ 4S", "tp_25_x", "aio_tp_4s_20a", "m12025_5500", "digital_micro", "b4s_450");
    static final Build CW25 = new Build("CW 2,5″ 4S", "cw_25_duct", "aio_tp_4s_20a", "m1404_3800", "digital_micro", "b4s_850");
    static final Build TP3 = new Build("3″ 4S", "tp_3_x", "aio_tp_4s_20a", "m1404_3800", "digital_micro", "propwash:b4s_650");
    static final Build STOCK_CINE = new Build("Cinewhoop (Propwash)", "propwash:cinewhoop_3", "propwash:aio_20", "propwash:m1404_4600",
            "propwash:digital_lite", "propwash:b4s_650");
    static final Build TP35 = new Build("3,5″ 6S", "tp_35_stretched", "aio_tp_6s_20a", "m1505_2500", "digital_micro", "b6s_750");
    static final Build CW35 = new Build("CW 3,5″ 6S", "cw_35_duct", "aio_20_6s_45a", "m2004_1700", "digital_hd_pro", "b6s_850");
    static final Build LR4 = new Build("LR 4″ 6S SS", "lr_4_x", "aio_20_6s_45a", "m2004_1700", "digital_micro", "b6s_3000_ss");
    static final Build FS5 = new Build("5″ 6S", "propwash:freestyle_5", "stack_30_6s_60a", "m2207_1950", "digital_hd_pro", "propwash:b6s_1300");
    static final Build LR6 = new Build("LR 6″ Li-Ion", "lr_6_x", "stack_30_6s_60a", "m2507_1500", "digital_lr", "b6s_3000_lion");
    static final Build LR6_LIPO = new Build("LR 6″ LiPo", "lr_6_x", "stack_30_6s_60a", "m2507_1500", "digital_lr", "b6s_1800");
    static final Build LR7 = new Build("LR 7″ Li-Ion", "lr_7_deadcat", "stack_30_6s_60a", "m28065_1300", "digital_lr", "b6s_4500_lion");
    static final Build LR7_LIPO = new Build("LR 7″ LiPo", "lr_7_deadcat", "stack_30_6s_60a", "m28065_1300", "digital_lr", "b6s_2200");
    static final Build LR8 = new Build("LR 8″ 6S2P", "lr_8_x", "stack_30_8s_65a", "m2807_1100", "digital_lr", "b6s_8400_lion");
    static final Build LR8_1P = new Build("LR 8″ 6S1P", "lr_8_x", "stack_30_8s_65a", "m2807_1100", "digital_lr", "b6s_4500_lion");
    static final Build X10 = new Build("X-Class 10″", "xclass_10", "stack_30_8s_100a", "m3115_900", "digital_hd_pro", "b6s_5000");
    static final Build X12 = new Build("X-Class 12″", "xclass_12", "stack_30_8s_100a", "m4214_400", "digital_lr", "b8s_5000");
    static final Build X13 = new Build("X-Class 13″", "xclass_13", "stack_30_8s_100a", "m4214_400", "digital_lr", "b8s_5000");

    static final Set<String> NONE = Set.of();
    static final Set<String> LION = Set.of("battery_weak", "heavy_sag", "battery_overload");
    static final Corridor WHOOP = new Corridor("Whoop", 20, 60, 3.5, 7, 25, 50, 2.5, 9, 2.3, 9, 35, 85, 60, 20, NONE);
    static final Corridor TOOTHPICK = new Corridor("Toothpick", 50, 230, 5, 9, 20, 35, 6, 16, 4, 13, 70, 110, Double.NaN, 15, NONE);
    static final Corridor TOOTHPICK_35 = new Corridor("Toothpick 3,5″", 50, 230, 5, 9, 20, 35, 6, 16, 4, 13, 100, 130, Double.NaN, 15, NONE);
    static final Corridor CINEWHOOP = new Corridor("Cinewhoop", 190, 420, 4, 7, 23, 35, 4.5, 13, 3.5, 11, 80, 150, 100, 25, NONE);
    static final Corridor FIVE_INCH = new Corridor("5″", 400, 560, 10, 15, 16, 25, 8, 14.5, 5.5, 11, 125, 190, Double.NaN, 15, NONE);
    static final Corridor LONG_RANGE = new Corridor("Long Range", 440, 1400, 3.5, 11, 20, 50, 12, 48, 8, 33, 90, 130, Double.NaN, 20, LION);
    static final Corridor X_CLASS = new Corridor("X-Class", 1600, 3400, 5, 9, 25, 40, 12, 26, 8, 18, 100, 140, Double.NaN, 20, NONE);

    static final List<Case> CASES = List.of(
            new Case("p31_16_2b_pc", W65, WHOOP, NONE),
            new Case("p31_14_3b_bi", W65, WHOOP, NONE),
            new Case("p31_12_4b_hard", W65, WHOOP, NONE),
            new Case("p40_20_2b_pc", W75, WHOOP, NONE),
            new Case("p40_17_3b_pc", W75, WHOOP, NONE),
            new Case("p40_15_4b_pc", W85, WHOOP, NONE),
            new Case("p40_14_5b_soft", W85, WHOOP, NONE),
            new Case("p45_19_3b_pc", W85, WHOOP, NONE),
            new Case("p45_17_4b_bi", W85, WHOOP, NONE),
            new Case("p51_20_3b_pc", TP2, TOOTHPICK, NONE),
            new Case("p51_18_4b_pc", TP2, TOOTHPICK, NONE),
            new Case("p65_25_3b_pc", TP25, TOOTHPICK, NONE),
            new Case("p65_22_4b_bi", CW25, CINEWHOOP, NONE),
            new Case("p76_30_2b_pc", TP3, TOOTHPICK, NONE),
            new Case("p76_25_3b_hard", TP3, TOOTHPICK, NONE),
            new Case("p76_24_5b_pc_duct", TP3, TOOTHPICK, NONE),
            new Case("p89_30_3b_pc", TP35, TOOTHPICK_35, NONE),
            new Case("p89_25_5b_pc_duct", CW35, CINEWHOOP, NONE),
            new Case("p102_25_2b_pc", LR4, LONG_RANGE, NONE),
            new Case("p102_30_3b_gfn", LR4, LONG_RANGE, NONE),
            new Case("p130_36_3b_bi", FS5, FIVE_INCH, NONE),
            new Case("p130_41_3b_soft", FS5, FIVE_INCH, NONE),
            new Case("p130_46_3b_hard", FS5, FIVE_INCH, NONE),
            new Case("p130_50_3b_gfn", FS5, FIVE_INCH, NONE),
            new Case("p130_35_4b_pc", FS5, FIVE_INCH, NONE),
            new Case("p130_40_2b_pc", FS5, FIVE_INCH, NONE),
            new Case("p152_33_3b_pc", LR6, LONG_RANGE, NONE),
            new Case("p152_40_2b_gfn", LR6, LONG_RANGE, NONE),
            new Case("p178_35_3b_pc", LR7, LONG_RANGE, NONE),
            new Case("p178_40_2b_cfn", LR7, LONG_RANGE, NONE),
            new Case("p178_45_3b_gfn", LR7, LONG_RANGE, NONE),
            new Case("p203_40_3b_gfn", LR8, LONG_RANGE, NONE),
            new Case("p203_45_2b_cfn", LR8, LONG_RANGE, NONE),
            new Case("p254_45_3b_cfn", X10, X_CLASS, Set.of("hover_throttle")),
            new Case("p254_50_2b_carbon", X10, X_CLASS, NONE),
            new Case("p305_60_2b_cfn", X12, X_CLASS, NONE),
            new Case("p330_70_2b_carbon", X13, X_CLASS, NONE),
            new Case("p330_60_3b_cfn", X13, X_CLASS, NONE));

    static final double MARGIN = 0.01;

    private static final Map<String, BuildStats> STATS = new HashMap<>();
    private static final Map<String, JsonObject> RECIPES = new HashMap<>();

    static BuildStats stats(Build build, String prop) {
        return STATS.computeIfAbsent(build.name() + "|" + prop, k -> Bench.stats(build.spec(prop)));
    }

    static PropDefinition prop(String id) {
        PartFiles.registerAll();
        return PropwashApi.parts().get(PropDefinition.class, Bench.id(id)).orElseThrow(() -> new IllegalArgumentException("kein Prop " + id));
    }

    static JsonObject recipe(String id) {
        return RECIPES.computeIfAbsent(id, key -> {
            try (InputStream in = PropBenchTest.class.getClassLoader().getResourceAsStream("data/justmoreparts/recipe/" + key + ".json")) {
                if (in == null) {
                    throw new IllegalArgumentException("kein Rezept " + key);
                }
                return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    static boolean recipeUses(String id, String item) {
        return recipe(id).getAsJsonObject("key").entrySet().stream().anyMatch(e -> e.getValue().getAsString().equals(item));
    }

    static boolean all(Build build, String prop, String[] others, ToDoubleFunction<BuildStats> metric, boolean greater) {
        double value = metric.applyAsDouble(stats(build, prop));
        for (String other : others) {
            double o = metric.applyAsDouble(stats(build, other));
            if (greater ? !(value > o + Math.abs(o) * MARGIN) : !(value < o - Math.abs(o) * MARGIN)) {
                return false;
            }
        }
        return true;
    }

    static boolean longer(Build b, String p, String... others) {
        return all(b, p, others, BuildStats::hoverFlightTimeMin, true) && all(b, p, others, BuildStats::cruiseFlightTimeMin, true);
    }

    static boolean hoverLonger(Build b, String p, String... others) {
        return all(b, p, others, BuildStats::hoverFlightTimeMin, true);
    }

    static boolean cruiseLonger(Build b, String p, String... others) {
        return all(b, p, others, BuildStats::cruiseFlightTimeMin, true);
    }

    static boolean cruiseShorter(Build b, String p, String... others) {
        return all(b, p, others, BuildStats::cruiseFlightTimeMin, false);
    }

    static boolean shorter(Build b, String p, String... others) {
        return all(b, p, others, BuildStats::hoverFlightTimeMin, false) && all(b, p, others, BuildStats::cruiseFlightTimeMin, false);
    }

    static boolean flightOrder(Build b, String fewer, String more) {
        BuildStats f = stats(b, fewer);
        BuildStats m = stats(b, more);
        return f.hoverFlightTimeMin() > m.hoverFlightTimeMin() && cruiseLonger(b, fewer, more)
                && all(b, fewer, new String[]{more}, BuildStats::mixedFlightTimeMin, true)
                && all(b, fewer, new String[]{more}, BuildStats::aggressiveFlightTimeMin, true);
    }

    static double thrustRatio(Build b, String p, String other) {
        return stats(b, p).thrustToWeight() / stats(b, other).thrustToWeight();
    }

    static boolean moreThrust(Build b, String p, String... others) {
        return all(b, p, others, BuildStats::thrustToWeight, true);
    }

    static boolean lessThrust(Build b, String p, String... others) {
        return all(b, p, others, BuildStats::thrustToWeight, false);
    }

    static boolean moreCurrent(Build b, String p, String... others) {
        return all(b, p, others, BuildStats::fullThrottleCurrentA, true);
    }

    static boolean lessCurrent(Build b, String p, String... others) {
        return all(b, p, others, BuildStats::fullThrottleCurrentA, false);
    }

    static boolean faster(Build b, String p, String... others) {
        return all(b, p, others, BuildStats::topSpeedKmh, true);
    }

    static boolean slower(Build b, String p, String... others) {
        return all(b, p, others, BuildStats::topSpeedKmh, false);
    }

    static boolean hotter(Build b, String p, String... others) {
        return all(b, p, others, BuildStats::sustainedMotorTempC, true);
    }

    static boolean cooler(Build b, String p, String... others) {
        return all(b, p, others, BuildStats::sustainedMotorTempC, false);
    }

    static boolean quicker(Build b, String p, String... others) {
        return all(b, p, others, BuildStats::motorResponseMs, false);
    }

    static boolean sluggisher(Build b, String p, String... others) {
        return all(b, p, others, BuildStats::motorResponseMs, true);
    }

    static boolean heavier(String p, String... others) {
        return Arrays.stream(others).allMatch(o -> prop(p).basics().massGrams() > prop(o).basics().massGrams());
    }

    static boolean lighter(String p, String... others) {
        return Arrays.stream(others).allMatch(o -> prop(p).basics().massGrams() < prop(o).basics().massGrams());
    }

    static boolean warns(Build b, String p, String warning) {
        return stats(b, p).warnings().contains(warning);
    }

    static boolean clean(Build b, String p) {
        return stats(b, p).warnings().isEmpty() && Bench.check(b.spec(p)).warnings().isEmpty();
    }

    static boolean stiff(String p, double min) {
        return prop(p).stiffness() >= min && prop(p).stiffness() > 1.0;
    }

    static boolean tough(String p) {
        return prop(p).basics().durability() > 1.0;
    }

    static boolean brittle(String p) {
        return prop(p).basics().durability() < 1.0;
    }

    static boolean current(Build b, String p, String other, double maxRatio) {
        return stats(b, p).fullThrottleCurrentA() <= maxRatio * stats(b, other).fullThrottleCurrentA();
    }

    static boolean flexLoss(Build b, String p) {
        PropDefinition soft = prop(p);
        Identifier twinId = Identifier.fromNamespaceAndPath("jmp_test", p + "_stiff");
        if (PropwashApi.parts().get(twinId).isEmpty()) {
            PropwashApi.parts().register(PropDefinition.builder(twinId).mass(soft.basics().massGrams())
                    .size(soft.diameterMm(), soft.pitchInch(), soft.blades())
                    .coefficients(soft.thrustCoefficient(), soft.powerCoefficient()).material(soft.material()).stiffness(1.0)
                    .build());
        }
        return stats(b, p).thrustToWeight() < stats(b, twinId.toString()).thrustToWeight();
    }

    static final String[] P31 = {"p31_16_2b_pc", "p31_14_3b_bi", "p31_12_4b_hard"};
    static final String[] P40 = {"p40_20_2b_pc", "p40_17_3b_pc", "p40_15_4b_pc", "p40_14_5b_soft"};
    static final String[] P76 = {"p76_30_2b_pc", "p76_25_3b_hard", "p76_24_5b_pc_duct", "propwash:p3_3b"};
    static final String[] P130 = {"p130_36_3b_bi", "p130_41_3b_soft", "p130_46_3b_hard", "p130_50_3b_gfn", "p130_35_4b_pc",
            "p130_40_2b_pc", "propwash:p51_3b"};
    static final String[] P178 = {"p178_35_3b_pc", "p178_40_2b_cfn", "p178_45_3b_gfn", "propwash:p7_2b"};

    static String[] except(String[] group, String p) {
        return Arrays.stream(group).filter(o -> !o.equals(p)).toArray(String[]::new);
    }

    static List<Claim> claims() {
        List<Claim> c = new ArrayList<>();
        String p31a = "p31_16_2b_pc";
        c.add(new Claim(p31a, 2, () -> longer(W65, p31a, "p31_14_3b_bi", "p31_12_4b_hard", "propwash:p31_4b", "propwash:p31_3b")
                && longer(STOCK_WHOOP, p31a, "propwash:p31_4b", "propwash:p31_3b")));
        c.add(new Claim(p31a, 3, () -> lessCurrent(W65, p31a, except(P31, p31a)) && cooler(W65, p31a, except(P31, p31a))));
        c.add(new Claim(p31a, 4, () -> lighter(p31a, except(P31, p31a)) && quicker(W65, p31a, except(P31, p31a))));
        c.add(new Claim(p31a, 5, () -> lessThrust(W65, p31a, except(P31, p31a))));
        String p31b = "p31_14_3b_bi";
        c.add(new Claim(p31b, 2, () -> stiff(p31b, 1.1) && tough(p31b)));
        c.add(new Claim(p31b, 3, () -> moreThrust(W65, p31b, "p31_16_2b_pc") && longer(W65, p31b, "p31_12_4b_hard")));
        c.add(new Claim(p31b, 4, () -> heavier(p31b, "propwash:p31_3b")));
        String p31c = "p31_12_4b_hard";
        c.add(new Claim(p31c, 2, () -> moreThrust(W65, p31c, except(P31, p31c))));
        c.add(new Claim(p31c, 3, () -> stiff(p31c, 1.1)));
        c.add(new Claim(p31c, 4, () -> brittle(p31c) && prop(p31c).basics().durability() < prop("p40_14_5b_soft").basics().durability()));
        c.add(new Claim(p31c, 5, () -> moreCurrent(W65, p31c, except(P31, p31c)) && shorter(W65, p31c, except(P31, p31c))));
        String p40a = "p40_20_2b_pc";
        c.add(new Claim(p40a, 2, () -> longer(W75, p40a, except(P40, p40a))));
        c.add(new Claim(p40a, 3, () -> lessCurrent(W75, p40a, except(P40, p40a)) && quicker(W75, p40a, except(P40, p40a))));
        c.add(new Claim(p40a, 4, () -> lessThrust(W85, p40a, except(P40, p40a))));
        String p40b = "p40_17_3b_pc";
        c.add(new Claim(p40b, 2, () -> longer(W75, p40b, "p40_15_4b_pc", "p40_14_5b_soft")
                && longer(W85, p40b, "p40_15_4b_pc", "p40_14_5b_soft")));
        c.add(new Claim(p40b, 3, () -> moreThrust(W85, p40b, "p40_20_2b_pc")));
        c.add(new Claim(p40b, 4, () -> longer(W75, "p40_20_2b_pc", p40b)));
        String p40c = "p40_15_4b_pc";
        c.add(new Claim(p40c, 2, () -> moreThrust(W85, p40c, "p40_20_2b_pc")));
        c.add(new Claim(p40c, 3, () -> cruiseShorter(W85, p40c, "p40_20_2b_pc", "p40_17_3b_pc")
                && cruiseShorter(W75, p40c, "p40_20_2b_pc", "p40_17_3b_pc")));
        c.add(new Claim(p40c, 4, () -> moreCurrent(W85, p40c, "p40_20_2b_pc")));
        c.add(new Claim(p40c, 5, () -> stats(W75, p40c).thrustToWeight() < 1.05 * stats(W75, "p40_17_3b_pc").thrustToWeight() && clean(W85, p40c)));
        String p40d = "p40_14_5b_soft";
        c.add(new Claim(p40d, 2, () -> moreThrust(W85, p40d, "p40_20_2b_pc", "p40_15_4b_pc")));
        c.add(new Claim(p40d, 3, () -> prop(p40d).basics().durability() >= 1.3));
        c.add(new Claim(p40d, 4, () -> prop(p40d).stiffness() < 1.0 && flexLoss(W85, p40d)));
        c.add(new Claim(p40d, 5, () -> moreCurrent(W85, p40d, "p40_20_2b_pc", "p40_15_4b_pc") && warns(W75, p40d, "esc_current_limited")));
        String p45a = "p45_19_3b_pc";
        String p45b = "p45_17_4b_bi";
        c.add(new Claim(p45a, 2, () -> quicker(W85, p45a, p45b)));
        c.add(new Claim(p45a, 3, () -> lessCurrent(W85, p45a, p45b)));
        c.add(new Claim(p45a, 4, () -> lessThrust(W85, p45a, p45b) && shorter(W85, p45a, p45b)));
        c.add(new Claim(p45b, 2, () -> moreThrust(W85, p45b, p45a) && longer(W85, p45b, p45a)));
        c.add(new Claim(p45b, 3, () -> stiff(p45b, 1.1) && tough(p45b)));
        c.add(new Claim(p45b, 4, () -> moreCurrent(W85, p45b, p45a) && hotter(W85, p45b, p45a)));
        c.add(new Claim(p45b, 5, () -> heavier(p45b, p45a) && sluggisher(W85, p45b, p45a)));
        String p51a = "p51_20_3b_pc";
        String p51b = "p51_18_4b_pc";
        c.add(new Claim(p51a, 2, () -> quicker(TP2, p51a, p51b)));
        c.add(new Claim(p51a, 3, () -> lessCurrent(TP2, p51a, p51b) && cooler(TP2, p51a, p51b)));
        c.add(new Claim(p51a, 4, () -> lessThrust(TP2, p51a, p51b)));
        c.add(new Claim(p51b, 2, () -> moreThrust(TP2, p51b, p51a)));
        c.add(new Claim(p51b, 3, () -> moreThrust(TP2, p51b, p51a)));
        c.add(new Claim(p51b, 4, () -> moreCurrent(TP2, p51b, p51a) && hotter(TP2, p51b, p51a)));
        c.add(new Claim(p51b, 5, () -> heavier(p51b, p51a) && sluggisher(TP2, p51b, p51a)));
        String p65a = "p65_25_3b_pc";
        String p65b = "p65_22_4b_bi";
        c.add(new Claim(p65a, 2, () -> lighter(p65a, p65b) && longer(TP25, p65a, p65b)));
        c.add(new Claim(p65a, 3, () -> cooler(TP25, p65a, p65b)));
        c.add(new Claim(p65a, 4, () -> lessThrust(CW25, p65a, p65b)));
        c.add(new Claim(p65b, 2, () -> stats(CW25, p65b).thrustToWeight() > 1.2 * stats(CW25, p65a).thrustToWeight()));
        c.add(new Claim(p65b, 3, () -> stiff(p65b, 1.1) && tough(p65b)));
        c.add(new Claim(p65b, 4, () -> moreCurrent(CW25, p65b, p65a) && hotter(CW25, p65b, p65a)));
        c.add(new Claim(p65b, 5, () -> warns(TP25, p65b, "motor_thermal") && warns(TP25, p65b, "heavy_sag")));
        String p76a = "p76_30_2b_pc";
        c.add(new Claim(p76a, 2, () -> longer(TP3, p76a, except(P76, p76a)) && longer(STOCK_CINE, p76a, except(P76, p76a))));
        c.add(new Claim(p76a, 3, () -> lessCurrent(TP3, p76a, except(P76, p76a)) && cooler(TP3, p76a, except(P76, p76a))));
        c.add(new Claim(p76a, 4, () -> lessThrust(TP3, p76a, "p76_25_3b_hard", "p76_24_5b_pc_duct")));
        String p76b = "p76_25_3b_hard";
        c.add(new Claim(p76b, 2, () -> longer(TP3, p76b, "propwash:p3_3b") && longer(STOCK_CINE, p76b, "propwash:p3_3b")));
        c.add(new Claim(p76b, 3, () -> stiff(p76b, 1.1)));
        c.add(new Claim(p76b, 4, () -> brittle(p76b)));
        String p76c = "p76_24_5b_pc_duct";
        c.add(new Claim(p76c, 2, () -> moreThrust(STOCK_CINE, p76c, except(P76, p76c)) && moreThrust(TP3, p76c, except(P76, p76c))));
        c.add(new Claim(p76c, 3, () -> longer(STOCK_CINE, p76c, "propwash:p3_3b")));
        c.add(new Claim(p76c, 4, () -> moreCurrent(STOCK_CINE, p76c, except(P76, p76c)) && hotter(STOCK_CINE, p76c, except(P76, p76c))));
        c.add(new Claim(p76c, 5, () -> heavier(p76c, except(P76, p76c)) && sluggisher(STOCK_CINE, p76c, except(P76, p76c))));
        String p89a = "p89_30_3b_pc";
        String p89b = "p89_25_5b_pc_duct";
        c.add(new Claim(p89a, 2, () -> stats(TP35, p89a).thrustToWeight() >= 8.5 && stats(TP35, p89a).thrustToWeight() < 9.0
                && Math.round(stats(TP35, p89a).topSpeedKmh()) == 115 && clean(TP35, p89a)));
        c.add(new Claim(p89a, 3, () -> lighter(p89a, p89b) && quicker(TP35, p89a, p89b) && quicker(CW35, p89a, p89b)));
        c.add(new Claim(p89a, 4, () -> current(CW35, p89a, p89b, 0.5) && flightOrder(CW35, p89a, p89b)));
        c.add(new Claim(p89a, 5, () -> thrustRatio(CW35, p89a, p89b) > 0.68 && thrustRatio(CW35, p89a, p89b) < 0.72));
        c.add(new Claim(p89b, 2, () -> thrustRatio(CW35, p89b, p89a) > 1.4 && thrustRatio(CW35, p89b, p89a) < 1.5));
        c.add(new Claim(p89b, 3, () -> stats(CW35, p89b).fullThrottleCurrentA() >= 2.0 * stats(CW35, p89a).fullThrottleCurrentA()
                && stats(CW35, p89b).fullThrottleCurrentA() < 2.25 * stats(CW35, p89a).fullThrottleCurrentA() && hotter(CW35, p89b, p89a)));
        c.add(new Claim(p89b, 4, () -> heavier(p89b, p89a) && sluggisher(CW35, p89b, p89a)));
        c.add(new Claim(p89b, 5, () -> flightOrder(CW35, p89a, p89b)));
        String p102a = "p102_25_2b_pc";
        String p102b = "p102_30_3b_gfn";
        c.add(new Claim(p102a, 2, () -> longer(LR4, p102a, p102b)
                && stats(LR4, p102a).mixedFlightTimeMin() > stats(LR4, p102b).mixedFlightTimeMin()));
        c.add(new Claim(p102a, 3, () -> current(LR4, p102a, p102b, 0.8) && cooler(LR4, p102a, p102b)));
        c.add(new Claim(p102a, 4, () -> lessThrust(LR4, p102a, p102b) && slower(LR4, p102a, p102b)));
        c.add(new Claim(p102b, 2, () -> moreThrust(LR4, p102b, p102a) && faster(LR4, p102b, p102a)));
        c.add(new Claim(p102b, 3, () -> stiff(p102b, 1.2)));
        c.add(new Claim(p102b, 4, () -> brittle(p102b)));
        c.add(new Claim(p102b, 5, () -> moreCurrent(LR4, p102b, p102a) && shorter(LR4, p102b, p102a)));
        String stock5 = "propwash:p51_3b";
        String p130a = "p130_36_3b_bi";
        c.add(new Claim(p130a, 2, () -> longer(FS5, p130a, stock5) && lessCurrent(FS5, p130a, stock5)));
        c.add(new Claim(p130a, 3, () -> stiff(p130a, 1.1) && tough(p130a) && cooler(FS5, p130a, stock5)));
        c.add(new Claim(p130a, 4, () -> slower(FS5, p130a, stock5)));
        String p130b = "p130_41_3b_soft";
        c.add(new Claim(p130b, 2, () -> prop(p130b).basics().durability() >= 1.3));
        c.add(new Claim(p130b, 3, () -> longer(FS5, p130b, stock5)));
        c.add(new Claim(p130b, 4, () -> prop(p130b).stiffness() < 1.0 && stats(FS5, p130b).propwashNoise() > 1.0));
        c.add(new Claim(p130b, 5, () -> flexLoss(FS5, p130b)));
        String p130c = "p130_46_3b_hard";
        c.add(new Claim(p130c, 2, () -> stiff(p130c, 1.1) && lighter(p130c, stock5)));
        c.add(new Claim(p130c, 3, () -> faster(FS5, p130c, "p130_36_3b_bi", "p130_41_3b_soft")));
        c.add(new Claim(p130c, 4, () -> brittle(p130c)));
        c.add(new Claim(p130c, 5, () -> moreCurrent(FS5, p130c, "p130_36_3b_bi", "p130_41_3b_soft")));
        String p130d = "p130_50_3b_gfn";
        c.add(new Claim(p130d, 2, () -> faster(FS5, p130d, except(P130, p130d))));
        c.add(new Claim(p130d, 3, () -> stiff(p130d, 1.2)));
        c.add(new Claim(p130d, 4, () -> moreCurrent(FS5, p130d, except(P130, p130d)) && hotter(FS5, p130d, except(P130, p130d))));
        c.add(new Claim(p130d, 5, () -> shorter(FS5, p130d, except(P130, p130d)) && brittle(p130d)));
        String p130e = "p130_35_4b_pc";
        c.add(new Claim(p130e, 2, () -> moreThrust(FS5, p130e, except(P130, p130e))));
        c.add(new Claim(p130e, 3, () -> longer(FS5, p130e, stock5)));
        c.add(new Claim(p130e, 4, () -> slower(FS5, p130e, stock5)));
        c.add(new Claim(p130e, 5, () -> heavier(p130e, except(P130, p130e)) && sluggisher(FS5, p130e, stock5)));
        String p130f = "p130_40_2b_pc";
        c.add(new Claim(p130f, 2, () -> longer(FS5, p130f, except(P130, p130f))));
        c.add(new Claim(p130f, 3, () -> current(FS5, p130f, stock5, 0.75)));
        c.add(new Claim(p130f, 4, () -> lighter(p130f, except(P130, p130f)) && quicker(FS5, p130f, except(P130, p130f))));
        c.add(new Claim(p130f, 5, () -> lessThrust(FS5, p130f, except(P130, p130f))));
        String p152a = "p152_33_3b_pc";
        String p152b = "p152_40_2b_gfn";
        c.add(new Claim(p152a, 2, () -> prop(p152a).basics().durability() > prop(p152b).basics().durability()));
        c.add(new Claim(p152a, 3, () -> moreThrust(LR6_LIPO, p152a, p152b)));
        c.add(new Claim(p152a, 4, () -> shorter(LR6, p152a, p152b) && moreCurrent(LR6, p152a, p152b)));
        c.add(new Claim(p152b, 2, () -> longer(LR6, p152b, p152a) && lessCurrent(LR6, p152b, p152a) && longer(LR6_LIPO, p152b, p152a)));
        c.add(new Claim(p152b, 3, () -> faster(LR6, p152b, p152a) && faster(LR6_LIPO, p152b, p152a)));
        c.add(new Claim(p152b, 4, () -> stiff(p152b, 1.2)));
        c.add(new Claim(p152b, 5, () -> brittle(p152b)));
        String p178a = "p178_35_3b_pc";
        String p178b = "p178_40_2b_cfn";
        String p178c = "p178_45_3b_gfn";
        c.add(new Claim(p178a, 2, () -> prop(p178a).basics().durability() > prop(p178b).basics().durability()
                && prop(p178a).basics().durability() > prop(p178c).basics().durability()));
        c.add(new Claim(p178a, 3, () -> moreCurrent(LR7, p178a, p178b) && shorter(LR7, p178a, p178b)));
        c.add(new Claim(p178a, 4, () -> heavier(p178a, p178b) && sluggisher(LR7, p178a, p178b)));
        c.add(new Claim(p178b, 2, () -> hoverLonger(LR7, p178b, except(P178, p178b))));
        c.add(new Claim(p178b, 3, () -> faster(LR7, p178b, p178a, p178c)));
        c.add(new Claim(p178b, 4, () -> stiff(p178b, 1.4)));
        c.add(new Claim(p178b, 5, () -> brittle(p178b) && prop(p178b).basics().durability() < prop(p178a).basics().durability()));
        c.add(new Claim(p178c, 2, () -> moreThrust(LR7_LIPO, p178c, except(P178, p178c)) && faster(LR7_LIPO, p178c, except(P178, p178c))));
        c.add(new Claim(p178c, 3, () -> stiff(p178c, 1.2)));
        c.add(new Claim(p178c, 4, () -> moreCurrent(LR7_LIPO, p178c, except(P178, p178c)) && warns(LR7_LIPO, p178c, "motor_thermal")
                && hotter(LR7_LIPO, p178c, except(P178, p178c))));
        c.add(new Claim(p178c, 5, () -> shorter(LR7_LIPO, p178c, p178b) && shorter(LR7, p178c, p178b)));
        String p203a = "p203_40_3b_gfn";
        String p203b = "p203_45_2b_cfn";
        c.add(new Claim(p203a, 2, () -> moreThrust(LR8, p203a, p203b) && hoverLonger(LR8, p203a, p203b)
                && moreThrust(LR8_1P, p203a, p203b) && hoverLonger(LR8_1P, p203a, p203b)));
        c.add(new Claim(p203a, 3, () -> stiff(p203a, 1.2)));
        c.add(new Claim(p203a, 4, () -> slower(LR8, p203a, p203b) && moreCurrent(LR8, p203a, p203b) && slower(LR8_1P, p203a, p203b)));
        c.add(new Claim(p203a, 5, () -> heavier(p203a, p203b) && hotter(LR8, p203a, p203b)));
        c.add(new Claim(p203b, 2, () -> faster(LR8, p203b, p203a) && cooler(LR8, p203b, p203a) && lessCurrent(LR8, p203b, p203a)
                && faster(LR8_1P, p203b, p203a)));
        c.add(new Claim(p203b, 3, () -> lighter(p203b, p203a) && quicker(LR8, p203b, p203a)));
        c.add(new Claim(p203b, 4, () -> stiff(p203b, 1.4)));
        c.add(new Claim(p203b, 5, () -> lessThrust(LR8, p203b, p203a) && hoverLonger(LR8, p203a, p203b)));
        String p254a = "p254_45_3b_cfn";
        String p254b = "p254_50_2b_carbon";
        c.add(new Claim(p254a, 2, () -> !recipeUses(p254a, "minecraft:quartz") && recipeUses(p254b, "minecraft:quartz")));
        c.add(new Claim(p254a, 3, () -> stiff(p254a, 1.4)));
        c.add(new Claim(p254a, 4, () -> moreCurrent(X10, p254a, p254b) && hotter(X10, p254a, p254b)));
        c.add(new Claim(p254a, 5, () -> heavier(p254a, p254b) && sluggisher(X10, p254a, p254b)));
        c.add(new Claim(p254b, 2, () -> stiff(p254b, 2.0) && tough(p254b)));
        c.add(new Claim(p254b, 3, () -> longer(X10, p254b, p254a) && lessCurrent(X10, p254b, p254a)));
        c.add(new Claim(p254b, 4, () -> faster(X10, p254b, p254a) && cooler(X10, p254b, p254a)));
        c.add(new Claim(p254b, 5, () -> recipeUses(p254b, "minecraft:quartz") && recipeUses(p254b, "minecraft:gold_ingot")));
        String p305 = "p305_60_2b_cfn";
        String p330a = "p330_70_2b_carbon";
        String p330b = "p330_60_3b_cfn";
        c.add(new Claim(p305, 2, () -> stiff(p305, 1.4)));
        c.add(new Claim(p305, 3, () -> lighter(p305, p330a, p330b) && quicker(X13, p305, p330a, p330b)));
        c.add(new Claim(p305, 4, () -> lessThrust(X13, p305, p330a, p330b)));
        c.add(new Claim(p330a, 2, () -> stiff(p330a, 2.0) && tough(p330a)));
        c.add(new Claim(p330a, 3, () -> longer(X13, p330a, p330b) && faster(X13, p330a, p330b)));
        c.add(new Claim(p330a, 4, () -> recipeUses(p330a, "minecraft:quartz") && recipeUses(p330a, "minecraft:gold_ingot")));
        c.add(new Claim(p330a, 5, () -> lessThrust(X13, p330a, p330b)));
        c.add(new Claim(p330b, 2, () -> moreThrust(X13, p330b, p330a, p305)));
        c.add(new Claim(p330b, 3, () -> stiff(p330b, 1.4)));
        c.add(new Claim(p330b, 4, () -> heavier(p330b, p330a, p305) && sluggisher(X13, p330b, p330a, p305)));
        c.add(new Claim(p330b, 5, () -> moreCurrent(X13, p330b, p330a) && hotter(X13, p330b, p330a)));
        return c;
    }

    static List<String> propFiles() {
        Path dir = PartFiles.root().resolve("prop");
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(f -> f.getFileName().toString()).filter(n -> n.endsWith(".json")).map(n -> n.substring(0, n.length() - 5))
                    .sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Map<String, String> lang(String code) {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("settings.gradle"))) {
            dir = dir.getParent();
        }
        if (dir == null) {
            throw new IllegalStateException("Projektwurzel nicht gefunden");
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(dir.resolve("common/src/lang/prop/" + code + ".json"), StandardCharsets.UTF_8))
                    .getAsJsonObject();
            Map<String, String> out = new LinkedHashMap<>();
            json.entrySet().forEach(e -> out.put(e.getKey(), e.getValue().getAsString()));
            return out;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String violation(Case c) {
        BuildStats s = stats(c.build(), c.prop());
        BuildCheck check = Bench.check(c.build().spec(c.prop()));
        Corridor k = c.corridor();
        List<String> out = new ArrayList<>();
        if (!check.problems().isEmpty()) {
            out.add("Probleme " + check.problems());
        }
        for (String warning : check.warnings()) {
            if (!k.allowedWarnings().contains(warning)) {
                out.add("Warnung " + warning);
            }
        }
        if (!c.exempt().contains("mass")) {
            range(out, "Masse", s.massGrams(), k.minMass(), k.maxMass());
        }
        range(out, "T/W", s.thrustToWeight(), k.minTw(), k.maxTw());
        if (!c.exempt().contains("hover_throttle")) {
            range(out, "Schwebegas", s.hoverThrottlePercent(), k.minHover(), k.maxHover());
        }
        range(out, "Schwebezeit", s.hoverFlightTimeMin(), k.minHoverTime(), k.maxHoverTime());
        range(out, "Reiseflugzeit", s.cruiseFlightTimeMin(), k.minCruise(), k.maxCruise());
        range(out, "Topspeed", s.topSpeedKmh(), k.minTop(), k.maxTop());
        double derate = PropwashApi.parts().get(MotorDefinition.class, Bench.id(c.build().motor())).orElseThrow().thermal().derateStartC();
        double maxMotor = Double.isNaN(k.maxMotorC()) ? derate : Math.min(derate, k.maxMotorC());
        if (!c.exempt().contains("motor_temp") && !(s.sustainedMotorTempC() < maxMotor)) {
            out.add(String.format(Locale.ROOT, "Motor %.1f °C ≥ %.0f", s.sustainedMotorTempC(), maxMotor));
        }
        if (!c.exempt().contains("props_in_view") && s.propsInViewPercent() > k.maxPropsInView()) {
            out.add(String.format(Locale.ROOT, "Props im Bild %.1f %% > %.0f", s.propsInViewPercent(), k.maxPropsInView()));
        }
        for (String warning : s.warnings()) {
            if (!k.allowedWarnings().contains(warning) && !(warning.equals("motor_thermal") && c.exempt().contains("motor_temp"))) {
                out.add("Warnung " + warning);
            }
        }
        return out.isEmpty() ? "" : c.prop() + " in " + c.build().name() + " (" + k.name() + "): " + String.join(", ", out);
    }

    static void range(List<String> out, String what, double value, double min, double max) {
        if (value < min || value > max) {
            out.add(String.format(Locale.ROOT, "%s %.1f außerhalb %.1f–%.1f", what, value, min, max));
        }
    }

    @Test
    public void everyPropHasAReferenceBuildInsideItsCorridor() {
        List<String> files = propFiles();
        assertEquals(38, files.size(), "38 Propeller laut Katalog");
        assertEquals(new TreeSet<>(files), CASES.stream().map(Case::prop).collect(Collectors.toCollection(TreeSet::new)),
                "jeder Prop braucht genau einen Referenzaufbau");
        System.out.println(Bench.header());
        List<String> violations = new ArrayList<>();
        for (Case c : CASES) {
            System.out.println(Bench.row(c.prop() + " @ " + c.build().name(), c.build().spec(c.prop())));
            String v = violation(c);
            if (!v.isEmpty()) {
                violations.add(v);
            }
        }
        assertTrue(violations.isEmpty(), "außerhalb der Zielkorridore:\n" + String.join("\n", violations));
    }

    @Test
    public void everyProAndConLineIsBackedByTheBench() {
        Map<String, String> de = lang("de_de");
        Map<String, String> en = lang("en_us");
        List<Claim> claims = claims();
        Map<String, List<Integer>> byProp = new HashMap<>();
        for (Claim claim : claims) {
            byProp.computeIfAbsent(claim.prop(), k -> new ArrayList<>()).add(claim.line());
        }
        List<String> failures = new ArrayList<>();
        for (String prop : propFiles()) {
            List<Integer> signed = new ArrayList<>();
            for (int i = 1; i <= 8; i++) {
                String line = de.get("justmoreparts.help." + prop + ".what." + i);
                if (line != null && (line.startsWith("+ ") || line.startsWith("− "))) {
                    signed.add(i);
                    String english = en.get("justmoreparts.help." + prop + ".what." + i);
                    if (english == null || english.charAt(0) != line.charAt(0)) {
                        failures.add(prop + " what." + i + ": englische Zeile fehlt oder hat ein anderes Vorzeichen");
                    }
                }
            }
            List<Integer> backed = byProp.getOrDefault(prop, List.of()).stream().sorted().toList();
            if (!signed.equals(backed)) {
                failures.add(prop + ": Pro/Contra-Zeilen " + signed + ", Prüfungen " + backed);
            }
        }
        for (Claim claim : claims) {
            if (!claim.check().getAsBoolean()) {
                failures.add(claim.prop() + " what." + claim.line() + " „" + de.get("justmoreparts.help." + claim.prop() + ".what." + claim.line())
                        + "“ stimmt im Prüfstand nicht");
            }
        }
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }

    @Test
    public void twoBladesFlyLongerThanThreeBladesOfTheSameSize() {
        List<String[]> pairs = List.of(
                new String[]{"W65", "p31_16_2b_pc", "p31_14_3b_bi"},
                new String[]{"W75", "p40_20_2b_pc", "p40_17_3b_pc"},
                new String[]{"TP3", "p76_30_2b_pc", "p76_25_3b_hard"},
                new String[]{"LR4", "p102_25_2b_pc", "p102_30_3b_gfn"},
                new String[]{"FS5", "p130_40_2b_pc", "p130_36_3b_bi"},
                new String[]{"FS5", "p130_40_2b_pc", "p130_41_3b_soft"},
                new String[]{"LR6", "p152_40_2b_gfn", "p152_33_3b_pc"},
                new String[]{"LR7", "p178_40_2b_cfn", "p178_35_3b_pc"},
                new String[]{"LR7", "p178_40_2b_cfn", "p178_45_3b_gfn"},
                new String[]{"X10", "p254_50_2b_carbon", "p254_45_3b_cfn"},
                new String[]{"X13", "p330_70_2b_carbon", "p330_60_3b_cfn"});
        Map<String, Build> builds = Map.of("W65", W65, "W75", W75, "TP3", TP3, "LR4", LR4, "FS5", FS5, "LR6", LR6, "LR7", LR7,
                "X10", X10, "X13", X13);
        for (String[] pair : pairs) {
            Build b = builds.get(pair[0]);
            assertTrue(longer(b, pair[1], pair[2]), pair[1] + " fliegt nicht länger als " + pair[2] + " in " + b.name());
            assertTrue(lessCurrent(b, pair[1], pair[2]), pair[1] + " braucht nicht weniger Strom als " + pair[2]);
        }
    }

    @Test
    public void fourAndFiveBladesGiveMoreThrustAndDrawMoreCurrent() {
        List<Object[]> pairs = List.of(
                new Object[]{W65, "p31_12_4b_hard", "p31_14_3b_bi"},
                new Object[]{W85, "p40_15_4b_pc", "p40_20_2b_pc"},
                new Object[]{W85, "p40_14_5b_soft", "p40_20_2b_pc"},
                new Object[]{W85, "p45_17_4b_bi", "p45_19_3b_pc"},
                new Object[]{TP2, "p51_18_4b_pc", "p51_20_3b_pc"},
                new Object[]{CW25, "p65_22_4b_bi", "p65_25_3b_pc"},
                new Object[]{TP3, "p76_24_5b_pc_duct", "p76_25_3b_hard"},
                new Object[]{STOCK_CINE, "p76_24_5b_pc_duct", "propwash:p3_3b"},
                new Object[]{CW35, "p89_25_5b_pc_duct", "p89_30_3b_pc"},
                new Object[]{FS5, "p130_35_4b_pc", "p130_36_3b_bi"});
        for (Object[] pair : pairs) {
            Build b = (Build) pair[0];
            String many = (String) pair[1];
            String three = (String) pair[2];
            assertTrue(moreThrust(b, many, three), many + " hat nicht mehr Schub als " + three + " in " + b.name());
            assertTrue(moreCurrent(b, many, three), many + " zieht nicht mehr Strom als " + three + " in " + b.name());
        }
    }

    @Test
    public void higherPitchGivesMoreTopSpeedAndMoreCurrent() {
        List<String> byPitch = List.of("p130_36_3b_bi", "p130_41_3b_soft", "p130_46_3b_hard", "p130_50_3b_gfn");
        for (Build b : List.of(FS5, new Build("Race (Propwash)", "propwash:race_5", "propwash:stack_30", "propwash:m2306_1860",
                "propwash:analog_race", "propwash:b6s_1100"))) {
            for (int i = 1; i < byPitch.size(); i++) {
                assertTrue(faster(b, byPitch.get(i), byPitch.get(i - 1)), byPitch.get(i) + " nicht schneller als " + byPitch.get(i - 1));
                assertTrue(moreCurrent(b, byPitch.get(i), byPitch.get(i - 1)), byPitch.get(i) + " zieht nicht mehr Strom");
            }
        }
    }

    @Test
    public void softPolycarbonateSurvivesCrashesButFlexesAndIsNoisier() {
        for (String soft : List.of("p40_14_5b_soft", "p130_41_3b_soft")) {
            PropDefinition d = prop(soft);
            assertEquals(PropMaterial.POLYCARBONATE, d.material());
            assertTrue(d.basics().durability() > 1.3, soft + ": Haltbarkeit");
            assertTrue(d.stiffness() < 1.0, soft + ": Steifigkeit < 1 heißt mehr Propwash-Rauschen (÷√s)");
        }
        assertTrue(stats(FS5, "p130_41_3b_soft").propwashNoise() > 1.0, "weiches PC: mehr Propwash-Rauschen laut Propwash-Analyse");
        assertTrue(stats(W85, "p40_14_5b_soft").propwashNoise() > 1.0, "weiches PC: mehr Propwash-Rauschen laut Propwash-Analyse");
        assertTrue(stats(FS5, "p130_50_3b_gfn").propwashNoise() < 1.0, "Glasfaser-Nylon: weniger Propwash-Rauschen");
        assertTrue(flexLoss(FS5, "p130_41_3b_soft"), "weiches PC verliert unter Last Schub");
        assertTrue(flexLoss(W85, "p40_14_5b_soft"), "weiches PC verliert unter Last Schub");
    }

    @Test
    public void materialsFollowTheCatalogTable() {
        Map<String, double[]> expected = Map.of(
                "pc", new double[]{1.0, 1.0}, "soft", new double[]{0.85, 1.35}, "hard", new double[]{1.12, 0.8},
                "bi", new double[]{1.1, 1.15}, "gfn", new double[]{1.2, 0.8}, "cfn", new double[]{1.4, 0.9},
                "carbon", new double[]{2.2, 1.4});
        Map<String, PropMaterial> materials = Map.of("pc", PropMaterial.POLYCARBONATE, "soft", PropMaterial.POLYCARBONATE,
                "hard", PropMaterial.POLYCARBONATE, "bi", PropMaterial.POLYCARBONATE, "gfn", PropMaterial.GLASS_FIBER_NYLON,
                "cfn", PropMaterial.CARBON_FIBER_NYLON, "carbon", PropMaterial.CARBON);
        for (String id : propFiles()) {
            String[] parts = id.split("_");
            String material = parts[3];
            PropDefinition d = prop(id);
            assertEquals(materials.get(material), d.material(), id);
            assertEquals(expected.get(material)[0], d.stiffness(), 1e-9, id + " Steifigkeit");
            assertEquals(expected.get(material)[1], d.basics().durability(), 1e-9, id + " Haltbarkeit");
            assertEquals(Integer.parseInt(parts[0].substring(1)), d.diameterMm(), id + " Durchmesser");
            assertEquals(Integer.parseInt(parts[1]) / 10.0, d.pitchInch(), 1e-9, id + " Steigung");
            assertEquals(Integer.parseInt(parts[2].substring(0, 1)), d.blades(), id + " Blätter");
            assertTrue(d.basics().visual().accentRgb().isPresent(), id + " Akzentfarbe");
            assertTrue(d.basics().sortOrder() >= 200 && d.basics().sortOrder() < 300, id + " Sortierung");
        }
    }

    @Test
    public void noPropCausesABuildProblemInItsReferenceBuild() {
        for (Case c : CASES) {
            BuildCheck check = Bench.check(c.build().spec(c.prop()));
            assertTrue(check.known() && check.buildable(), c.prop() + " in " + c.build().name() + ": " + check.problems());
        }
    }
}
