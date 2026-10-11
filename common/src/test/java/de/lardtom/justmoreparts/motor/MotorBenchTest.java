package de.lardtom.justmoreparts.motor;

import de.lardtom.justmoreparts.bench.Bench;
import de.lardtom.justmoreparts.bench.PartFiles;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.BuildCheck;
import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.build.BuildStats;
import de.lardtom.propwash.api.part.MotorDefinition;
import de.lardtom.propwash.api.part.PartDefinition;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MotorBenchTest {
    static final double TOLERANCE = 0.05;

    enum Corridor {
        WHOOP(20, 60, 3.5, 7, 25, 50, 2.5, 9, 2.3, 9, 35, 85, 60, 20),
        TOOTHPICK(50, 230, 5, 9, 20, 35, 6, 16, 4, 13, 70, 110, Double.NaN, 15),
        TOOTHPICK_35(50, 230, 5, 9, 20, 35, 6, 16, 4, 13, 100, 130, Double.NaN, 15),
        CINEWHOOP(190, 420, 4, 7, 23, 35, 4.5, 13, 3.5, 11, 80, 150, 100, 25),
        FIVE(400, 560, 10, 15, 16, 25, 8, 14.5, 5.5, 11, 125, 190, Double.NaN, 15),
        FREESTYLE_6_7(550, 900, 9, 14, 15, 27, 10, 22, 6, 15, 120, 160, Double.NaN, 20),
        LONG_RANGE(440, 1400, 3.5, 11, 20, 50, 12, 48, 8, 33, 90, 130, Double.NaN, 20),
        XCLASS(1600, 3400, 5, 9, 25, 40, 12, 26, 8, 18, 100, 140, Double.NaN, 20);

        final double[] limits;

        Corridor(double... limits) {
            this.limits = limits;
        }
    }

    record Build(String frame, String stack, String prop, String video, String battery) {
        BuildSpec with(String motor) {
            return Bench.spec(frame, stack, motor, prop, video, battery);
        }
    }

    record Ref(String motor, String label, BuildSpec spec, Corridor corridor, Set<String> allowed, Set<String> skip) {
    }

    static final Build W65 = new Build("whoop_65_lite", "aio_whoop_1s_6a", "p31_14_3b_bi", "analog_whoop_25mw", "b1s_380_hv");
    static final Build W65_250 = new Build("whoop_65_lite", "aio_whoop_1s_6a", "p31_14_3b_bi", "analog_whoop_25mw", "b1s_250_hv");
    static final Build W75_1S = new Build("whoop_75_duct", "aio_whoop_2s_12a", "p40_17_3b_pc", "analog_whoop_25mw", "b1s_550_hv");
    static final Build W75_2S = new Build("whoop_75_duct", "aio_whoop_2s_12a", "p40_20_2b_pc", "analog_whoop_25mw", "b2s_450_hv");
    static final Build W85_2S = new Build("whoop_85_duct", "aio_whoop_2s_12a", "p45_17_4b_bi", "digital_micro", "b2s_450_hv");
    static final Build TP2 = new Build("tp_2_x", "aio_whoop_2s_12a", "p51_20_3b_pc", "analog_whoop_25mw", "b2s_450_hv");
    static final Build TP25_2S = new Build("tp_25_x", "aio_tp_4s_20a", "p65_25_3b_pc", "digital_micro", "b2s_650");
    static final Build TP25_3S = new Build("tp_25_x", "aio_tp_4s_20a", "p65_25_3b_pc", "digital_micro", "b3s_650");
    static final Build TP25_4S = new Build("tp_25_x", "aio_tp_4s_20a", "p65_25_3b_pc", "digital_micro", "b4s_450");
    static final Build TP3_4S = new Build("tp_3_x", "aio_tp_4s_20a", "p76_25_3b_hard", "digital_micro", "propwash:b4s_650");
    static final Build TP3_4S_450 = new Build("tp_3_x", "aio_tp_4s_20a", "p76_25_3b_hard", "digital_micro", "b4s_450");
    static final Build TP3_6S = new Build("tp_3_x", "aio_tp_6s_20a", "p76_25_3b_hard", "digital_micro", "b6s_550");
    static final Build TP35_6S = new Build("tp_35_stretched", "aio_tp_6s_20a", "p89_30_3b_pc", "digital_micro", "b6s_750");
    static final Build TP35_4S = new Build("tp_35_stretched", "aio_20_4s_35a", "p89_30_3b_pc", "digital_micro", "b4s_850");
    static final Build TP35_4S_AIO20 = new Build("tp_35_stretched", "aio_tp_4s_20a", "p89_30_3b_pc", "digital_micro", "b4s_850");
    static final Build CW35 = new Build("cw_35_duct", "aio_20_6s_45a", "p89_25_5b_pc_duct", "digital_hd_pro", "b6s_850");
    static final Build BASH6 = new Build("fs_5_bash", "stack_30_6s_60a", "p130_41_3b_soft", "digital_hd_pro", "propwash:b6s_1300");
    static final Build DEADCAT6 = new Build("deadcat_5", "stack_30_6s_60a", "p130_36_3b_bi", "digital_hd_pro", "propwash:b6s_1300");
    static final Build RACE4 = new Build("race_5_stretched", "stack_30_4s_55a", "p130_46_3b_hard", "digital_low_latency", "b4s_1500");
    static final Build RACE4_SOFT = new Build("race_5_stretched", "stack_30_4s_55a", "p130_41_3b_soft", "digital_hd_pro", "b4s_1500");
    static final Build RACE4_BI = new Build("race_5_stretched", "stack_30_4s_55a", "p130_36_3b_bi", "digital_hd_pro", "b4s_1500");
    static final Build RACE6 = new Build("race_5_stretched", "stack_30_6s_60a", "p130_46_3b_hard", "digital_low_latency", "propwash:b6s_1300");
    static final Build LR6 = new Build("lr_6_x", "stack_30_6s_60a", "p152_33_3b_pc", "digital_lr", "b6s_1800");
    static final Build DC7 = new Build("lr_7_deadcat", "stack_30_6s_60a", "p178_40_2b_cfn", "digital_lr", "b6s_4500_lion");
    static final Build DC7_8S = new Build("lr_7_deadcat", "stack_30_8s_65a", "p178_40_2b_cfn", "digital_lr", "b8s_2200");
    static final Build FS7 = new Build("lr_7_deadcat", "stack_30_6s_60a", "p178_35_3b_pc", "digital_lr", "b6s_1800");
    static final Build LR8_6S = new Build("lr_8_x", "stack_30_8s_65a", "p203_45_2b_cfn", "digital_lr", "b6s_8400_lion");
    static final Build LR8_8S = new Build("lr_8_x", "stack_30_8s_65a", "p203_45_2b_cfn", "digital_lr", "b8s_4500_lion");
    static final Build XC10_6S = new Build("xclass_10", "stack_30_8s_100a", "p254_50_2b_carbon", "digital_hd_pro", "b6s_5000");
    static final Build XC10_8S = new Build("xclass_10", "stack_30_8s_100a", "p254_50_2b_carbon", "digital_lr", "b8s_5000");
    static final Build XC12_6S = new Build("xclass_12", "stack_30_8s_100a", "p305_60_2b_cfn", "digital_lr", "b6s_5000");
    static final Build XC13 = new Build("xclass_13", "stack_30_8s_100a", "p330_70_2b_carbon", "digital_lr", "b8s_5000");
    static final Build XC13_3B = new Build("xclass_13", "stack_30_8s_100a", "p330_60_3b_cfn", "digital_lr", "b8s_5000");

    static final String[] LION = {"battery_weak", "heavy_sag", "battery_overload"};

    static final List<Ref> REFS = new ArrayList<>();

    static {
        ref("m0702_23000", "65 mm 1S 250", Corridor.WHOOP, W65_250);
        ref("m0702_27000", "65 mm 1S 380", Corridor.WHOOP, W65);
        ref("m0802_15500", "75 mm 2S 450 digital, 75 °C nach 1 min Vollgas", Corridor.WHOOP,
                new Build("whoop_75_duct", "aio_whoop_2s_12a", "p40_20_2b_pc", "digital_micro", "b2s_450_hv"), "~temp");
        ref("m0802_19000", "75 mm 1S 450 (Preset whoop75_1s)", Corridor.WHOOP,
                new Build("whoop_75_duct", "aio_whoop_1s_6a", "p40_17_3b_pc", "analog_whoop_25mw", "b1s_450_hv"));
        ref("m1102_13500", "85 mm 2S 450 (Preset whoop85_2s)", Corridor.WHOOP, W85_2S);
        ref("m1102_18000", "75 mm 1S 750, 4 Blatt", Corridor.WHOOP,
                new Build("whoop_75_duct", "aio_whoop_2s_12a", "p40_15_4b_pc", "analog_whoop_25mw", "b1s_750_hv"));
        ref("m1103_8000", "2,5\" 3S 650", Corridor.TOOTHPICK, TP25_3S, "~hover");
        ref("m1103_11000", "2\" 2S 450 (Preset toothpick2_2s)", Corridor.TOOTHPICK, TP2);
        ref("m12025_5500", "2,5\" 4S 450", Corridor.TOOTHPICK, TP25_4S);
        ref("m12025_11500", "2\" 2S 450", Corridor.TOOTHPICK, TP2, "battery_weak", "~hoverTime");
        ref("m1404_2750", "3\" 6S 550", Corridor.TOOTHPICK, TP3_6S);
        ref("m1404_3800", "3\" 4S 650 (Preset toothpick3_4s)", Corridor.TOOTHPICK, TP3_4S);
        ref("m1505_2500", "3,5\" 6S 750 (Preset toothpick35_6s)", Corridor.TOOTHPICK_35, TP35_6S);
        ref("m1505_3600", "3,5\" 4S 850", Corridor.TOOTHPICK_35, TP35_4S);
        ref("m2004_1700", "Cinewhoop 3,5\" 6S (Preset cinewhoop35_6s)", Corridor.CINEWHOOP, CW35);
        ref("m2207_1500", "6\" LR 6S 1800", Corridor.LONG_RANGE, LR6);
        ref("m2207_1950", "5\" Bash 6S 1300 (Preset bash5_6s)", Corridor.FIVE, BASH6);
        ref("m2207_2550", "5\" Race 4S 1500", Corridor.FIVE, RACE4_SOFT);
        ref("m22075_1850", "5\" Deadcat 6S 1300 (Preset deadcat5_6s)", Corridor.FIVE, DEADCAT6);
        ref("m22075_2700", "5\" Race 4S 1500", Corridor.FIVE, RACE4_BI);
        ref("m23065_2020", "5\" Race 6S 1300", Corridor.FIVE, RACE6);
        ref("m23065_2450", "5\" Race 4S 1500 (Preset race5_4s)", Corridor.FIVE, RACE4);
        ref("m2507_1500", "6\" LR 6S 1800 (Preset lr6_6s)", Corridor.LONG_RANGE, LR6);
        ref("m2507_1800", "6\" Freestyle 6S 1800", Corridor.FREESTYLE_6_7, LR6);
        ref("m28065_1050", "8\" LR 8S Li-Ion", Corridor.LONG_RANGE, LR8_8S, LION);
        ref("m28065_1300", "7\" Deadcat 6S Li-Ion (Preset lr7_deadcat_lion)", Corridor.LONG_RANGE, DC7, LION);
        ref("m2807_1100", "8\" LR 6S2P Li-Ion (Preset lr8_6s_lion)", Corridor.LONG_RANGE, LR8_6S, LION);
        ref("m2807_1500", "7\" Freestyle 6S 1800", Corridor.FREESTYLE_6_7, FS7, "heavy_sag");
        ref("m3115_640", "10\" 8S 5000", Corridor.XCLASS, XC10_8S, "~hoverTime", "~cruise");
        ref("m3115_900", "10\" 6S 5000 (Preset xclass10_6s)", Corridor.XCLASS, XC10_6S);
        ref("m3115_1050", "10\" 6S 5000", Corridor.XCLASS, XC10_6S, "heavy_sag", "~speed", "~tw", "~hover");
        ref("m4214_330", "13\" 3 Blatt 8S 5000", Corridor.XCLASS, XC13_3B, "~tw", "~hover");
        ref("m4214_400", "13\" 8S 5000 (Preset xclass13_8s)", Corridor.XCLASS, XC13);
        ref("m4214_530", "13\" 8S 5000", Corridor.XCLASS, XC13, "heavy_sag", "~cruise");
    }

    static void ref(String motor, String label, Corridor corridor, Build build, String... exceptions) {
        Set<String> allowed = new TreeSet<>();
        Set<String> skip = new TreeSet<>();
        for (String e : exceptions) {
            if (e.startsWith("~")) {
                skip.add(e.substring(1));
            } else {
                allowed.add(e);
            }
        }
        REFS.add(new Ref(motor, label, build.with(motor), corridor, allowed, skip));
    }

    static BuildStats s(Build build, String motor) {
        return Bench.stats(build.with(motor));
    }

    static List<String> warnings(BuildSpec spec) {
        List<String> all = new ArrayList<>(Bench.check(spec).warnings());
        all.addAll(Bench.stats(spec).warnings());
        return all;
    }

    static MotorDefinition motor(String path) {
        PartFiles.registerAll();
        PartDefinition definition = PropwashApi.parts().get(Bench.id(path)).orElseThrow();
        return (MotorDefinition) definition;
    }

    static List<String> motorFiles() {
        List<String> out = new ArrayList<>();
        for (Identifier id : PartFiles.registerAll().keySet()) {
            if (PropwashApi.parts().get(id).orElse(null) instanceof MotorDefinition) {
                out.add(id.getPath());
            }
        }
        return out;
    }

    static String line(String label, BuildSpec spec) {
        return Bench.row(label, spec) + String.format(Locale.ROOT, " %.1fms", Bench.stats(spec).motorResponseMs());
    }

    @Test
    public void printsReferenceTable() {
        System.out.println(Bench.header());
        for (Ref ref : REFS) {
            System.out.println(line(ref.motor() + " " + ref.label(), ref.spec()));
        }
    }

    @Test
    public void thirtyFourMotorsWithReferenceBuilds() {
        List<String> files = motorFiles();
        assertEquals(34, files.size(), "Motoren in resources");
        Set<String> covered = new TreeSet<>();
        for (Ref ref : REFS) {
            covered.add(ref.motor());
        }
        assertEquals(new TreeSet<>(files), covered, "jeder Motor braucht einen Referenzaufbau");
    }

    @Test
    public void referenceBuildsStayInsideTheirCorridors() {
        List<String> failures = new ArrayList<>();
        for (Ref ref : REFS) {
            BuildCheck check = Bench.check(ref.spec());
            BuildStats st = Bench.stats(ref.spec());
            String who = ref.motor() + " (" + ref.label() + ")";
            if (!check.problems().isEmpty()) {
                failures.add(who + ": Probleme " + check.problems());
            }
            for (String warning : warnings(ref.spec())) {
                if (!ref.allowed().contains(warning)) {
                    failures.add(who + ": Warnung " + warning);
                }
            }
            double[] l = ref.corridor().limits;
            double derate = motor(ref.motor()).thermal().derateStartC();
            double maxTemp = Double.isNaN(l[12]) ? derate : Math.min(l[12], derate);
            range(failures, who, ref.skip(), "mass", st.massGrams(), l[0], l[1]);
            range(failures, who, ref.skip(), "tw", st.thrustToWeight(), l[2], l[3]);
            range(failures, who, ref.skip(), "hover", st.hoverThrottlePercent(), l[4], l[5]);
            range(failures, who, ref.skip(), "hoverTime", st.hoverFlightTimeMin(), l[6], l[7]);
            range(failures, who, ref.skip(), "cruise", st.cruiseFlightTimeMin(), l[8], l[9]);
            range(failures, who, ref.skip(), "speed", st.topSpeedKmh(), l[10], l[11]);
            if (!ref.skip().contains("temp") && !(st.sustainedMotorTempC() < maxTemp)) {
                failures.add(who + ": Motor " + fmt(st.sustainedMotorTempC()) + " °C, Grenze " + fmt(maxTemp) + " °C");
            }
            if (!ref.skip().contains("view") && st.propsInViewPercent() > l[13] * (1 + TOLERANCE)) {
                failures.add(who + ": Props im Bild " + fmt(st.propsInViewPercent()) + " %");
            }
        }
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }

    private static void range(List<String> failures, String who, Set<String> skip, String key, double value, double min, double max) {
        if (skip.contains(key)) {
            return;
        }
        if (!(value >= min * (1 - TOLERANCE) && value <= max * (1 + TOLERANCE))) {
            failures.add(who + ": " + key + " " + fmt(value) + " außerhalb " + fmt(min) + "–" + fmt(max));
        }
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    @Test
    public void presetsWithJmpMotorsAreNeitherSluggishNorOverheated() {
        String[][] presets = {
                {"whoop75_1s", "whoop_75_duct", "aio_whoop_1s_6a", "m0802_19000", "p40_17_3b_pc", "analog_whoop_25mw", "b1s_450_hv"},
                {"whoop85_2s", "whoop_85_duct", "aio_whoop_2s_12a", "m1102_13500", "p45_17_4b_bi", "digital_micro", "b2s_450_hv"},
                {"toothpick2_2s", "tp_2_x", "aio_whoop_2s_12a", "m1103_11000", "p51_20_3b_pc", "analog_whoop_25mw", "b2s_450_hv"},
                {"toothpick3_4s", "tp_3_x", "aio_tp_4s_20a", "m1404_3800", "p76_25_3b_hard", "digital_micro", "propwash:b4s_650"},
                {"toothpick35_6s", "tp_35_stretched", "aio_tp_6s_20a", "m1505_2500", "p89_30_3b_pc", "digital_micro", "b6s_750"},
                {"cinewhoop25_4s", "cw_25_duct", "aio_tp_4s_20a", "m1404_3800", "p65_22_4b_bi", "digital_micro", "b4s_850"},
                {"cinewhoop35_6s", "cw_35_duct", "aio_20_6s_45a", "m2004_1700", "p89_25_5b_pc_duct", "digital_hd_pro", "b6s_850"},
                {"bash5_6s", "fs_5_bash", "stack_30_6s_60a", "m2207_1950", "p130_41_3b_soft", "digital_hd_pro", "propwash:b6s_1300"},
                {"squashed5_6s", "fs_5_squashed", "stack_30_6s_60a", "m2207_1950", "p130_36_3b_bi", "digital_hd_pro", "propwash:b6s_1300"},
                {"deadcat5_6s", "deadcat_5", "stack_30_6s_60a", "m22075_1850", "p130_36_3b_bi", "digital_hd_pro", "propwash:b6s_1300"},
                {"race5_4s", "race_5_stretched", "stack_30_4s_55a", "m23065_2450", "p130_46_3b_hard", "digital_low_latency", "b4s_1500"},
                {"race5_6s", "race_5_stretched", "stack_30_6s_60a", "m23065_2020", "p130_50_3b_gfn", "analog_race_800", "propwash:b6s_1300"},
                {"hframe5_6s", "hframe_5", "stack_30_6s_60a", "m2207_1950", "p130_35_4b_pc", "digital_hd_pro", "propwash:b6s_1300"},
                {"lr4_6s_lion", "lr_4_x", "aio_20_6s_45a", "m2004_1700", "p102_30_3b_gfn", "digital_micro", "b6s_3000_lion"},
                {"lr6_6s", "lr_6_x", "stack_30_6s_60a", "m2507_1500", "p152_33_3b_pc", "digital_lr", "b6s_1800"},
                {"lr7_deadcat_lion", "lr_7_deadcat", "stack_30_6s_60a", "m28065_1300", "p178_40_2b_cfn", "digital_lr", "b6s_4500_lion"},
                {"lr7_8s", "lr_7_deadcat", "stack_30_8s_65a", "m2807_1100", "p178_40_2b_cfn", "digital_lr", "b8s_2200"},
                {"lr8_6s_lion", "lr_8_x", "stack_30_8s_65a", "m2807_1100", "p203_45_2b_cfn", "digital_lr", "b6s_8400_lion"},
                {"xclass10_6s", "xclass_10", "stack_30_8s_100a", "m3115_900", "p254_50_2b_carbon", "digital_hd_pro", "b6s_5000"},
                {"xclass12_8s", "xclass_12", "stack_30_8s_100a", "m4214_400", "p305_60_2b_cfn", "digital_lr", "b8s_5000"},
                {"xclass13_8s", "xclass_13", "stack_30_8s_100a", "m4214_400", "p330_70_2b_carbon", "digital_lr", "b8s_5000"},
                {"xclass13h_8s", "xclass_13_h", "stack_30_8s_100a", "m4214_400", "p330_60_3b_cfn", "digital_lr", "b8s_5000"}};
        List<String> failures = new ArrayList<>();
        System.out.println(Bench.header());
        for (String[] p : presets) {
            BuildSpec spec = Bench.spec(p[1], p[2], p[3], p[4], p[5], p[6]);
            System.out.println(line(p[0], spec));
            List<String> w = warnings(spec);
            if (w.contains("sluggish_motors") || w.contains("motor_thermal")) {
                failures.add(p[0] + ": " + w);
            }
            if (!(Bench.stats(spec).sustainedMotorTempC() < motor(p[3]).thermal().derateStartC())) {
                failures.add(p[0] + ": Motor über der Drosselgrenze");
            }
        }
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }

    @Test
    public void selfTestBuildsWithBuiltinPartsStayClean() {
        String[][] builds = {
                {"propwash:whoop_65", "propwash:aio_whoop", "m0702_23000", "propwash:p31_3b", "propwash:analog_nano", "propwash:b1s_300_hv"},
                {"propwash:cinewhoop_3", "propwash:aio_20", "m1404_3800", "propwash:p3_3b", "propwash:digital_lite", "propwash:b4s_650"},
                {"propwash:freestyle_5", "propwash:stack_30", "m2207_1950", "propwash:p51_3b", "propwash:digital_hd", "propwash:b6s_1300"},
                {"propwash:race_5", "propwash:stack_30", "m23065_2020", "propwash:p51_3b", "propwash:analog_race", "propwash:b6s_1300"},
                {"propwash:long_range_7", "propwash:stack_30", "m28065_1300", "propwash:p7_2b", "propwash:digital_hd", "propwash:b6s_4000_lion"},
                {"xclass_13", "stack_30_8s_100a", "m4214_400", "p330_70_2b_carbon", "digital_lr", "b8s_5000"}};
        for (String[] b : builds) {
            BuildSpec spec = Bench.spec(b[0], b[1], b[2], b[3], b[4], b[5]);
            BuildCheck check = Bench.check(spec);
            assertTrue(check.known() && check.buildable() && check.problems().isEmpty(), b[2] + " in " + b[0] + ": " + check.problems());
            List<String> w = warnings(spec);
            assertTrue(!w.contains("sluggish_motors") && !w.contains("motor_thermal"), b[2] + " in " + b[0] + ": " + w);
        }
        BuildSpec six = Bench.spec("propwash:freestyle_5", "propwash:stack_30", "m2207_2550", "propwash:p51_3b", "propwash:digital_hd", "propwash:b6s_1300");
        BuildSpec four = Bench.spec("propwash:freestyle_5", "propwash:stack_30", "m2207_2550", "propwash:p51_3b", "propwash:digital_hd", "propwash:b4s_650");
        assertTrue(Bench.check(six).warnings().contains("motor_overvoltage"));
        assertTrue(!Bench.check(four).warnings().contains("motor_overvoltage"));
    }

    @Test
    public void fourCellMotorsWarnAndSagOnSixCells() {
        String[][] pairs = {
                {"m2207_2550", "fs_5_bash", "p130_41_3b_soft"},
                {"m22075_2700", "fs_5_bash", "p130_36_3b_bi"},
                {"m23065_2450", "race_5_stretched", "p130_46_3b_hard"}};
        for (String[] p : pairs) {
            MotorDefinition definition = motor(p[0]);
            assertEquals(4, definition.maxCells(), p[0] + " ist ein 4S-Motor");
            BuildSpec four = Bench.spec(p[1], "stack_30_4s_55a", p[0], p[2], "digital_hd_pro", "b4s_1500");
            BuildSpec six = Bench.spec(p[1], "stack_30_6s_60a", p[0], p[2], "digital_hd_pro", "propwash:b6s_1300");
            assertTrue(Bench.check(six).warnings().contains("motor_overvoltage"), p[0] + " auf 6S ohne motor_overvoltage");
            assertTrue(!Bench.check(four).warnings().contains("motor_overvoltage"), p[0] + " auf 4S mit motor_overvoltage");
            List<String> w = warnings(six);
            assertTrue(w.contains("heavy_sag") || w.contains("esc_current_limited"), p[0] + " auf 6S ohne Einbruch: " + w);
            assertTrue(Bench.stats(six).topSpeedKmh() > Bench.stats(four).topSpeedKmh(), p[0] + " auf 6S nicht schneller");
            assertTrue(Bench.stats(six).fullThrottleCurrentA() > Bench.stats(four).fullThrottleCurrentA(), p[0] + " auf 6S nicht mehr Strom");
        }
    }

    @Test
    public void higherKvMeansMoreTopSpeedAndMoreCurrent() {
        Object[][] rows = {
                {W65, new String[]{"m0702_23000", "m0702_27000"}},
                {W75_1S, new String[]{"m0802_15500", "m0802_19000"}},
                {W75_1S, new String[]{"m1102_13500", "m1102_18000"}},
                {TP2, new String[]{"m1103_8000", "m1103_11000"}},
                {TP25_3S, new String[]{"m12025_5500", "m12025_11500"}},
                {TP3_4S, new String[]{"m1404_2750", "m1404_3800", "propwash:m1404_4600"}},
                {TP35_4S, new String[]{"m1505_2500", "m1505_3600"}},
                {BASH6, new String[]{"m2207_1500", "propwash:m2207_1750", "m2207_1950", "m2207_2550"}},
                {BASH6, new String[]{"m22075_1850", "m22075_2700"}},
                {RACE6, new String[]{"m23065_2020", "m23065_2450"}},
                {LR6, new String[]{"m2507_1500", "m2507_1800"}},
                {DC7, new String[]{"m28065_1050", "m28065_1300"}},
                {DC7, new String[]{"m2807_1100", "propwash:m2807_1300", "m2807_1500"}},
                {XC10_6S, new String[]{"m3115_640", "m3115_900", "m3115_1050"}},
                {XC13, new String[]{"m4214_330", "m4214_400", "m4214_530"}}};
        for (Object[] row : rows) {
            Build build = (Build) row[0];
            String[] motors = (String[]) row[1];
            for (int i = 1; i < motors.length; i++) {
                BuildStats low = s(build, motors[i - 1]);
                BuildStats high = s(build, motors[i]);
                assertTrue(motor(motors[i]).kv() > motor(motors[i - 1]).kv(), "KV-Reihe sortiert: " + motors[i]);
                assertTrue(high.topSpeedKmh() > low.topSpeedKmh(), motors[i] + " nicht schneller als " + motors[i - 1]);
                assertTrue(high.fullThrottleCurrentA() > low.fullThrottleCurrentA(), motors[i] + " zieht nicht mehr Strom als " + motors[i - 1]);
            }
        }
    }

    @Test
    public void claimedProsAndConsAreVisibleOnTheBench() {
        List<String> failed = new ArrayList<>();
        Claims c = new Claims(failed);

        BuildStats a23 = s(W65, "m0702_23000");
        BuildStats a27 = s(W65, "m0702_27000");
        c.that("0702/23000: längere Flugzeit als 27000 KV", gt(a23.mixedFlightTimeMin(), a27.mixedFlightTimeMin()));
        c.that("0702/23000: weniger Strom und kühler als 27000 KV",
                lt(a23.fullThrottleCurrentA(), a27.fullThrottleCurrentA()) && lt(a23.sustainedMotorTempC(), a27.sustainedMotorTempC()));
        c.that("0702/23000: weniger Schub (T/W 3,5 statt 4,3)", Math.abs(a23.thrustToWeight() - 3.5) < 0.1 && Math.abs(a27.thrustToWeight() - 4.3) < 0.1);
        c.that("0702/27000: mehr Topspeed (57 statt 51 km/h)", Math.round(a27.topSpeedKmh()) == 57 && Math.round(a23.topSpeedKmh()) == 51);
        c.that("0702/27000: 250-mAh-Akku am Limit", s(W65_250, "m0702_27000").batteryLoadPercent() >= 80.0);
        c.that("0702/27000: niedrigere Hitzegrenze", motor("m0702_27000").thermal().derateStartC() < 130.0);
        c.that("0702: leichtester Motor im Katalog", motorFiles().stream().allMatch(m -> motor(m).basics().massGrams() >= motor("m0702_23000").basics().massGrams())
                && lt(motor("m0702_23000").basics().massGrams(), motor("propwash:m0802_22000").basics().massGrams()));

        BuildStats b155_1 = s(W75_1S, "m0802_15500");
        BuildStats b190_1 = s(W75_1S, "m0802_19000");
        BuildStats b155_2 = s(W75_2S, "m0802_15500");
        BuildStats c135_2 = s(W75_2S, "m1102_13500");
        c.that("0802/15500: auf 1S kraftlos", b155_1.thrustToWeight() < b190_1.thrustToWeight() * 0.8);
        c.that("0802/15500: auf 2S kräftig (T/W 6)", b155_2.thrustToWeight() > 5.5);
        c.that("0802/15500: auf 2S längere Flugzeit als 1102/13500",
                gt(b155_2.hoverFlightTimeMin(), c135_2.hoverFlightTimeMin()) && gt(b155_2.mixedFlightTimeMin(), c135_2.mixedFlightTimeMin()));
        c.that("0802/15500: 45-mm-4-Blatt macht ihn träge", warnings(W85_2S.with("m0802_15500")).contains("sluggish_motors"));

        BuildStats c180_1 = s(W75_1S, "m1102_18000");
        c.that("0802/19000: längere Schwebezeit als 1102/18000 mit 40 mm", gt(b190_1.hoverFlightTimeMin(), c180_1.hoverFlightTimeMin()));
        BuildStats b190_preset = s(new Build("whoop_75_duct", "aio_whoop_1s_6a", "p40_17_3b_pc", "analog_whoop_25mw", "b1s_450_hv"), "m0802_19000");
        c.that("0802/19000: 75 mm auf 1S T/W 4, 7 min Schweben", b190_preset.thrustToWeight() > 3.8 && b190_preset.hoverFlightTimeMin() > 7.0);
        c.that("0802/19000: mit 31-mm-Props schwächer als 0702/27000", lt(s(W65, "m0802_19000").thrustToWeight(), a27.thrustToWeight()));
        c.that("1102/18000: 6-A-AIO zu klein", Bench.check(Bench.spec("whoop_75_duct", "aio_whoop_1s_6a", "m1102_18000", "p40_17_3b_pc",
                "analog_whoop_25mw", "b1s_550_hv")).warnings().contains("esc_undersized"));
        c.that("1102/18000: mehr Standschub als 0802/19000",
                c180_1.thrustToWeight() * c180_1.massGrams() > b190_1.thrustToWeight() * b190_1.massGrams());
        c.that("1102/18000: reagiert schneller (34 statt 46 ms)", Math.round(c180_1.motorResponseMs()) == 34 && Math.round(b190_1.motorResponseMs()) == 46);
        c.that("1102/18000: mehr Strom als 0802/19000", gt(c180_1.fullThrottleCurrentA(), b190_1.fullThrottleCurrentA()));

        BuildStats c135_85 = s(W85_2S, "m1102_13500");
        c.that("1102/13500: dreht 45-mm-4-Blatt ohne Trägheit", !c135_85.warnings().contains("sluggish_motors"));
        c.that("1102/13500: auf 1S im 65-mm-Rahmen zu schwach", warnings(W65.with("m1102_13500")).contains("cannot_hover"));

        BuildStats d80_2 = s(TP2, "m1103_8000");
        BuildStats d110_2 = s(TP2, "m1103_11000");
        BuildStats e115_2 = s(TP2, "m12025_11500");
        BuildStats d80_3 = s(TP25_3S, "m1103_8000");
        BuildStats e55_4 = s(TP25_4S, "m12025_5500");
        BuildStats f38_25 = s(TP25_4S, "m1404_3800");
        c.that("1103/8000: auf 2S lahm", d80_2.thrustToWeight() < d110_2.thrustToWeight() * 0.8);
        c.that("1103/8000: 2,5″ auf 3S schwebt länger als 1202,5 auf 4S", gt(d80_3.hoverFlightTimeMin(), e55_4.hoverFlightTimeMin()));
        c.that("1103/8000: auf 3S sehr kräftig (T/W über 7)", d80_3.thrustToWeight() > 7.0);
        c.that("1103/11000: leichter und länger in der Luft als 1202,5/11500",
                lt(d110_2.massGrams(), e115_2.massGrams()) && gt(d110_2.hoverFlightTimeMin(), e115_2.hoverFlightTimeMin()));
        c.that("1103/11000: wenig Reserve mit 2,5″ (ESC am Limit)", s(TP25_2S, "m1103_11000").escLoadPercent() > 65.0);
        c.that("1202,5/11500: reagiert schneller als 1103 (30 statt 32 ms)", Math.round(e115_2.motorResponseMs()) == 30
                && Math.round(d110_2.motorResponseMs()) == 32 && lt(s(TP25_2S, "m12025_11500").motorResponseMs(), s(TP25_2S, "m1103_11000").motorResponseMs()));
        c.that("1202,5/11500: mehr Topspeed als 1103/11000 mit 2,5″-Props",
                s(TP25_2S, "m12025_11500").topSpeedKmh() >= 1.02 * s(TP25_2S, "m1103_11000").topSpeedKmh());
        c.that("1202,5/11500: schwerer und kürzer in der Luft als 1103/11000", gt(e115_2.massGrams(), d110_2.massGrams())
                && lt(e115_2.hoverFlightTimeMin(), d110_2.hoverFlightTimeMin()) && lt(e115_2.mixedFlightTimeMin(), d110_2.mixedFlightTimeMin()));
        c.that("1202,5/11500: 75C-450-mAh-Akku zu schwach", warnings(TP2.with("m12025_11500")).contains("battery_weak"));
        c.that("1202,5/5500: leichter und länger in der Luft als 1404 auf 2,5″",
                lt(e55_4.massGrams(), f38_25.massGrams()) && gt(e55_4.hoverFlightTimeMin(), f38_25.hoverFlightTimeMin())
                        && gt(e55_4.thrustToWeight(), f38_25.thrustToWeight()));
        BuildStats e55_3inch = s(TP3_4S_450, "m12025_5500");
        c.that("1202,5/5500: mit 3″-Props überfordert (über 100 °C)", e55_3inch.sustainedMotorTempC() > 100.0
                && e55_3inch.sustainedMotorTempC() > e55_4.sustainedMotorTempC() + 20.0);
        c.that("1202,5/5500: nach 1 min Vollgas auf 2,5″ 4S unter 80 °C", e55_4.motorTempMinuteC() < 80.0);

        BuildStats f38 = s(TP3_4S, "m1404_3800");
        BuildStats f46 = s(TP3_4S, "propwash:m1404_4600");
        BuildStats f275_6 = s(TP3_6S, "m1404_2750");
        c.that("1404/3800: weniger Strom und kühler als 4600 KV", lt(f38.fullThrottleCurrentA(), f46.fullThrottleCurrentA())
                && lt(f38.sustainedMotorTempC(), f46.sustainedMotorTempC()));
        c.that("1404/3800: nach 1 min Vollgas 59 statt 88 °C", Math.round(f38.motorTempMinuteC()) == 59 && Math.round(f46.motorTempMinuteC()) == 88);
        c.that("1404/3800: weniger Punch als 4600 KV", lt(f38.thrustToWeight(), f46.thrustToWeight()));
        c.that("1404/2750: auf 6S weniger Strom, längere Flugzeit als 3800 auf 4S", lt(f275_6.fullThrottleCurrentA(), f38.fullThrottleCurrentA())
                && gt(f275_6.hoverFlightTimeMin(), f38.hoverFlightTimeMin()) && gt(f275_6.mixedFlightTimeMin(), f38.mixedFlightTimeMin()));
        c.that("1404/2750: 6S-Akku macht die Drohne 20 g schwerer", f275_6.massGrams() - f38.massGrams() > 18.0 && f275_6.massGrams() - f38.massGrams() < 23.0);
        c.that("1404/2750: auf 4S zu wenig Spannung", Bench.check(TP3_4S.with("m1404_2750")).warnings().contains("motor_undervoltage"));

        BuildStats g25_cw = s(CW35, "m1505_2500");
        BuildStats h17_cw = s(CW35, "m2004_1700");
        BuildStats g25_4 = s(TP35_4S, "m1505_2500");
        BuildStats g36_4 = s(TP35_4S, "m1505_3600");
        c.that("1505/2500: im Duct etwas mehr Schub als 2004", g25_cw.thrustToWeight() > h17_cw.thrustToWeight() * 1.05);
        c.that("1505/2500: im Cinewhoop zu heiß (drosselt)", g25_cw.warnings().contains("motor_thermal")
                && g25_cw.motorTempMinuteC() > h17_cw.motorTempMinuteC() + 50.0);
        c.that("1505/3600: auf 4S kräftig (T/W über 9)", g36_4.thrustToWeight() > 9.0 && g36_4.thrustToWeight() > g25_4.thrustToWeight() * 1.3);
        c.that("1505/3600: hoher Strom auf 4S", g36_4.fullThrottleCurrentA() > 2 * g25_4.fullThrottleCurrentA());
        c.that("1505/3600: 20-A-AIO zu klein", Bench.check(TP35_4S_AIO20.with("m1505_3600")).warnings().contains("esc_undersized"));
        c.that("2004/1700: im Duct ohne Drosselung (1 min Vollgas 83 °C)", Math.round(h17_cw.motorTempMinuteC()) == 83
                && !h17_cw.warnings().contains("motor_thermal"));
        c.that("2004/1700: im Duct ein Drittel weniger Strom als 1505", h17_cw.fullThrottleCurrentA() <= 0.7 * g25_cw.fullThrottleCurrentA());
        c.that("2004/1700: schwerer als 1505", gt(motor("m2004_1700").basics().massGrams(), motor("m1505_2500").basics().massGrams()));

        BuildStats i15 = s(BASH6, "m2207_1500");
        BuildStats i195 = s(BASH6, "m2207_1950");
        BuildStats i255_6 = s(BASH6, "m2207_2550");
        BuildStats i255_4 = s(RACE4_SOFT, "m2207_2550");
        c.that("2207/1500: längere Flugzeit als 1950 KV", gt(i15.mixedFlightTimeMin(), i195.mixedFlightTimeMin()));
        c.that("2207/1500: wenig Strom (76 statt 131 A)", Math.round(i15.fullThrottleCurrentA()) == 76 && Math.round(i195.fullThrottleCurrentA()) == 131);
        c.that("2207/1500: weniger Topspeed und Punch als 1950", lt(i15.topSpeedKmh(), i195.topSpeedKmh()) && lt(i15.thrustToWeight(), i195.thrustToWeight()));
        c.that("2207/1950: viel Punch (T/W über 11)", i195.thrustToWeight() > 11.0 && gt(i195.thrustToWeight(), i15.thrustToWeight()));
        c.that("2207/1950: wärmer als 1500", gt(i195.sustainedMotorTempC(), i15.sustainedMotorTempC()));
        c.that("2207/2550: auf 4S fast so kräftig wie 1950 auf 6S", i255_4.thrustToWeight() > i195.thrustToWeight() * 0.9);
        c.that("2207/2550: auf 6S Akku zu schwach", warnings(BASH6.with("m2207_2550")).contains("battery_weak"));
        c.that("2207/2550: auf 6S über 50 % mehr Strom", i255_6.fullThrottleCurrentA() > 1.5 * i255_4.fullThrottleCurrentA());

        BuildStats j185 = s(DEADCAT6, "m22075_1850");
        BuildStats j195 = s(DEADCAT6, "m2207_1950");
        c.that("2207,5/1850: kühler als 2207/1950", lt(j185.sustainedMotorTempC(), j195.sustainedMotorTempC()));
        c.that("2207,5/1850: reagiert schneller als 2207/1950", lt(j185.motorResponseMs(), j195.motorResponseMs()));
        c.that("2207,5/1850: hitzefest (Drosselung ab 145 °C)", motor("m22075_1850").thermal().derateStartC() >= 145.0);
        c.that("2207,5/1850: schwerer, etwas weniger Schub", gt(j185.massGrams(), j195.massGrams()) && lt(j185.thrustToWeight(), j195.thrustToWeight()));

        BuildStats k27 = s(RACE4, "m22075_2700");
        BuildStats k255 = s(RACE4, "m2207_2550");
        BuildStats k245 = s(RACE4, "m23065_2450");
        c.that("2207,5/2700: meister Punch und Topspeed auf 4S", gt(k27.thrustToWeight(), k255.thrustToWeight())
                && gt(k27.thrustToWeight(), k245.thrustToWeight()) && gt(k27.topSpeedKmh(), k255.topSpeedKmh()));
        c.that("2207,5/2700: hoher Strom, 1500 mAh bricht ein", gt(k27.fullThrottleCurrentA(), k255.fullThrottleCurrentA())
                && k27.warnings().contains("heavy_sag"));
        c.that("2306,5/2450: über 150 km/h auf 4S", k245.topSpeedKmh() > 150.0);
        c.that("2306,5/2450: leichter und sparsamer als 2207/2550 auf 4S", lt(k245.massGrams(), k255.massGrams())
                && lt(k245.fullThrottleCurrentA(), k255.fullThrottleCurrentA()) && gt(k245.hoverFlightTimeMin(), k255.hoverFlightTimeMin()));

        BuildStats l202 = s(RACE6, "m23065_2020");
        BuildStats l195 = s(RACE6, "m2207_1950");
        BuildStats l186 = s(RACE6, "propwash:m2306_1860");
        c.that("2306,5/2020: mehr Topspeed als 2207/1950 und 2306/1860", gt(l202.topSpeedKmh(), l195.topSpeedKmh()) && gt(l202.topSpeedKmh(), l186.topSpeedKmh()));
        c.that("2306,5/2020: leichter als 2207", lt(motor("m23065_2020").basics().massGrams(), motor("m2207_1950").basics().massGrams()));
        c.that("2306,5/2020: mehr Strom als 2207/1950", gt(l202.fullThrottleCurrentA(), l195.fullThrottleCurrentA()));
        c.that("2306,5/2020: mit 5,0″-Steigung Spannungseinbruch", warnings(Bench.spec("race_5_stretched", "stack_30_6s_60a", "m23065_2020",
                "p130_50_3b_gfn", "analog_race_800", "propwash:b6s_1300")).contains("heavy_sag"));

        BuildStats m15_2507 = s(LR6, "m2507_1500");
        BuildStats m15_2207 = s(LR6, "m2207_1500");
        BuildStats m18 = s(LR6, "m2507_1800");
        c.that("2507/1500: mit 6″ kühler als 2207/1500", m15_2507.sustainedMotorTempC() < m15_2207.sustainedMotorTempC() - 10.0);
        c.that("2507/1500: schwerer, kürzer in der Luft als 2207/1500", gt(m15_2507.massGrams(), m15_2207.massGrams())
                && lt(m15_2507.hoverFlightTimeMin(), m15_2207.hoverFlightTimeMin()));
        c.that("2207/1500: mit 6″ längere Schwebezeit als 2507", gt(m15_2207.hoverFlightTimeMin(), m15_2507.hoverFlightTimeMin()));
        c.that("2507/1800: sehr viel Schub (T/W über 12)", m18.thrustToWeight() > 12.0 && m18.thrustToWeight() > m15_2507.thrustToWeight() * 1.15);
        c.that("2507/1800: hitzefest (Drosselung ab 140 °C)", motor("m2507_1800").thermal().derateStartC() >= 140.0);
        c.that("2507/1800: hoher Strom (162 statt 112 A), heißer", Math.round(m18.fullThrottleCurrentA()) == 162
                && Math.round(m15_2507.fullThrottleCurrentA()) == 112 && m18.motorTempMinuteC() > m15_2507.motorTempMinuteC() + 8.0);

        BuildStats n130 = s(DC7, "m28065_1300");
        BuildStats n105 = s(DC7, "m28065_1050");
        BuildStats n2807 = s(DC7, "propwash:m2807_1300");
        BuildStats o15_dc = s(DC7, "m2807_1500");
        c.that("2806,5/1300: 31 min Schweben im 7″-Preset", n130.hoverFlightTimeMin() > 30.5);
        c.that("2806,5/1300: leichter und länger in der Luft als 2807/1300", lt(motor("m28065_1300").basics().massGrams(),
                motor("propwash:m2807_1300").basics().massGrams())
                && gt(n130.hoverFlightTimeMin(), n2807.hoverFlightTimeMin()) && gt(n130.mixedFlightTimeMin(), n2807.mixedFlightTimeMin()));
        c.that("2806,5/1300: kühler als 2807/1300", lt(n130.sustainedMotorTempC(), n2807.sustainedMotorTempC()));
        c.that("2806,5/1300: wenig Punch", n130.thrustToWeight() < 5.5);
        c.that("2806,5/1300: mit 8″ überfordert", warnings(LR8_6S.with("m28065_1300")).contains("motor_thermal"));
        c.that("2806,5/1050: auf 6S schwach", lt(n105.thrustToWeight(), n130.thrustToWeight()) && n105.hoverThrottlePercent() > n130.hoverThrottlePercent() + 5.0);
        BuildStats n105_8 = s(LR8_8S, "m28065_1050");
        BuildStats o11_8 = s(LR8_8S, "m2807_1100");
        c.that("2806,5/1050: auf 8S mit 8″ über 35 min Schweben", n105_8.hoverFlightTimeMin() > 35.0);
        c.that("2806,5/1050: 7″ mit 8S-LiPo zu heiß", warnings(DC7_8S.with("m28065_1050")).contains("motor_thermal"));
        BuildStats o11_7 = s(DC7_8S, "m2807_1100");
        c.that("2807/1100: 7″ auf 8S ohne Warnung, T/W fast 12", warnings(DC7_8S.with("m2807_1100")).isEmpty()
                && o11_7.thrustToWeight() > 11.0 && o11_7.thrustToWeight() < 12.5);
        c.that("2806,5/1050: auf 8S so ausdauernd wie 2807/1100, 2 g leichter", n105_8.hoverFlightTimeMin() >= o11_8.hoverFlightTimeMin()
                && lt(motor("m28065_1050").basics().massGrams(), motor("m2807_1100").basics().massGrams()));
        BuildStats o11 = s(LR8_6S, "m2807_1100");
        c.that("2807/1100: dreht 8″ ohne zu drosseln", !o11.warnings().contains("motor_thermal")
                && lt(o11.sustainedMotorTempC(), motor("m2807_1100").thermal().derateStartC()));
        c.that("2807/1100: schwer", motor("m2807_1100").basics().massGrams() > 47.0);
        BuildStats o15 = s(FS7, "m2807_1500");
        BuildStats n130_fs = s(FS7, "m28065_1300");
        c.that("2807/1500: kräftig mit 7″-3-Blatt (T/W über 12)", o15.thrustToWeight() > 12.0 && o15.thrustToWeight() > n130_fs.thrustToWeight() * 1.05);
        c.that("2807/1500: hoher Strom, Spannungseinbruch", o15.fullThrottleCurrentA() > 150.0 && o15.warnings().contains("heavy_sag"));
        c.that("2807/1500: kürzere Flugzeit als 1100 KV", lt(o15_dc.mixedFlightTimeMin(), s(DC7, "m2807_1100").mixedFlightTimeMin()));
        c.that("2807/1500: mit 3-Blatt warm (über 110 °C)", o15.sustainedMotorTempC() > 110.0);

        BuildStats p64_8 = s(XC10_8S, "m3115_640");
        BuildStats p64_6 = s(XC10_6S, "m3115_640");
        BuildStats p90 = s(XC10_6S, "m3115_900");
        BuildStats p105 = s(XC10_6S, "m3115_1050");
        c.that("3115/640: längste Flugzeit der 3115", gt(p64_8.hoverFlightTimeMin(), p90.hoverFlightTimeMin()) && gt(p64_8.mixedFlightTimeMin(), p90.mixedFlightTimeMin())
                && gt(p64_8.hoverFlightTimeMin(), p105.hoverFlightTimeMin()) && gt(p64_8.mixedFlightTimeMin(), p105.mixedFlightTimeMin()));
        c.that("3115/640: auf 6S träge", p64_6.thrustToWeight() < p90.thrustToWeight() * 0.75);
        c.that("3115/900: ausgewogen (T/W über 8 mit 10″)", p90.thrustToWeight() > 8.0 && p90.thrustToWeight() < 9.0);
        c.that("3115/900: mit 12″ überfordert", warnings(XC12_6S.with("m3115_900")).contains("motor_thermal"));
        c.that("3115/1050: spritziger als 900 KV", gt(p105.thrustToWeight(), p90.thrustToWeight()) && gt(p105.topSpeedKmh(), p90.topSpeedKmh()));
        c.that("3115/1050: hoher Strom, kürzere Reise", p105.fullThrottleCurrentA() > p90.fullThrottleCurrentA() * 1.2
                && lt(p105.mixedFlightTimeMin(), p90.mixedFlightTimeMin()) && p105.warnings().contains("heavy_sag"));

        BuildStats q33 = s(XC13, "m4214_330");
        BuildStats q40 = s(XC13, "m4214_400");
        BuildStats q53 = s(XC13, "m4214_530");
        c.that("4214/330: wenig Strom, längste Flugzeit", q33.fullThrottleCurrentA() < q40.fullThrottleCurrentA() * 0.75
                && gt(q33.mixedFlightTimeMin(), q40.mixedFlightTimeMin()) && gt(q33.mixedFlightTimeMin(), q53.mixedFlightTimeMin()));
        c.that("4214/330: auf 8S wenig Punch (T/W < 5)", q33.thrustToWeight() < 5.0);
        c.that("4214/400: X-Class-Standard (T/W 5–7)", q40.thrustToWeight() > 5.0 && q40.thrustToWeight() < 7.0);
        c.that("4214/530: enormer Punch (T/W über 7)", q53.thrustToWeight() > 7.0 && q53.thrustToWeight() > q40.thrustToWeight() * 1.25);
        c.that("4214/530: sehr hoher Strom, Einbruch, kurze Reise", q53.fullThrottleCurrentA() > 250.0 && q53.warnings().contains("heavy_sag")
                && lt(q53.mixedFlightTimeMin(), q40.mixedFlightTimeMin()));

        assertTrue(failed.isEmpty(), "nicht belegte Pro/Contra-Aussagen:\n" + String.join("\n", failed));
        System.out.println(c.count + " Pro/Contra-Aussagen belegt");
    }

    static boolean gt(double a, double b) {
        return a > b + Math.abs(b) * 0.01;
    }

    static boolean lt(double a, double b) {
        return a < b - Math.abs(b) * 0.01;
    }

    static final class Claims {
        final List<String> failed;
        int count;

        Claims(List<String> failed) {
            this.failed = failed;
        }

        void that(String claim, boolean holds) {
            count++;
            if (!holds) {
                failed.add(claim);
            }
        }
    }
}
