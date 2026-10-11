package de.lardtom.justmoreparts.power;

import de.lardtom.justmoreparts.bench.Bench;
import de.lardtom.justmoreparts.bench.PartFiles;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.BuildCheck;
import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.build.BuildStats;
import de.lardtom.propwash.api.part.BatteryDefinition;
import de.lardtom.propwash.api.part.MotorDefinition;
import de.lardtom.propwash.api.part.PartDefinition;
import de.lardtom.propwash.api.part.PartKind;
import de.lardtom.propwash.api.part.StackDefinition;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PowerClaimsTest {
    private static final String CREATIVE_FC = "propwash:creative_fc";
    private static final String[] WHOOP_65_LITE = {"whoop_65_lite", "aio_whoop_1s_6a", "m0702_27000", "p31_14_3b_bi", "analog_whoop_25mw"};
    private static final String[] WHOOP_75_1S = {"whoop_75_duct", "aio_whoop_1s_6a", "m0802_19000", "p40_17_3b_pc", "analog_whoop_25mw"};
    private static final String[] WHOOP_75_2S = {"whoop_75_duct", "aio_whoop_2s_12a", "m0802_15500", "p40_20_2b_pc", "analog_whoop_25mw"};
    private static final String[] WHOOP_85_2S = {"whoop_85_duct", "aio_whoop_2s_12a", "m1102_13500", "p45_17_4b_bi", "digital_micro"};
    private static final String[] TP25_1103 = {"tp_25_x", "aio_tp_4s_20a", "m1103_8000", "p65_25_3b_pc", "analog_whoop_25mw"};
    private static final String[] TP25_12025 = {"tp_25_x", "aio_tp_4s_20a", "m12025_5500", "p65_25_3b_pc", "digital_micro"};
    private static final String[] TP3_1404 = {"tp_3_x", "aio_tp_4s_20a", "m1404_3800", "p76_25_3b_hard", "digital_micro"};
    private static final String[] TP3_6S = {"tp_3_x", "aio_tp_6s_20a", "m1404_2750", "p76_25_3b_hard", "digital_micro"};
    private static final String[] TP35_4S = {"tp_35_stretched", "aio_20_4s_35a", "m1505_3600", "p89_30_3b_pc", "digital_micro"};
    private static final String[] TP35_6S = {"tp_35_stretched", "aio_tp_6s_20a", "m1505_2500", "p89_30_3b_pc", "digital_micro"};
    private static final String[] CW35 = {"cw_35_duct", "aio_20_6s_45a", "m2004_1700", "p89_25_5b_pc_duct", "digital_hd_pro"};
    private static final String[] RACE5_4S = {"race_5_stretched", "stack_30_4s_55a", "m23065_2450", "p130_46_3b_hard", "digital_low_latency"};
    private static final String[] BASH5 = {"fs_5_bash", "stack_30_6s_60a", "m2207_1950", "p130_41_3b_soft", "digital_hd_pro"};
    private static final String[] FREESTYLE5 = {"propwash:freestyle_5", "stack_30_6s_60a", "m2207_1950", "p130_41_3b_soft", "digital_hd_pro"};
    private static final String[] LR6 = {"lr_6_x", "stack_30_6s_60a", "m2507_1500", "p152_33_3b_pc", "digital_lr"};
    private static final String[] LR7_DEADCAT = {"lr_7_deadcat", "stack_30_6s_60a", "m28065_1300", "p178_40_2b_cfn", "digital_lr"};
    private static final String[] LR4 = {"lr_4_x", "aio_20_6s_45a", "m2004_1700", "p102_30_3b_gfn", "digital_micro"};
    private static final String[] LR8 = {"lr_8_x", "stack_30_8s_65a", "m2807_1100", "p203_45_2b_cfn", "digital_lr"};
    private static final String[] XC10 = {"xclass_10", "stack_30_8s_100a", "m3115_900", "p254_50_2b_carbon", "digital_hd_pro"};
    private static final String[] XC12 = {"xclass_12", "stack_30_8s_100a", "m4214_400", "p305_60_2b_cfn", "digital_lr"};
    private static final String[] XC13 = {"xclass_13", "stack_30_8s_100a", "m4214_400", "p330_70_2b_carbon", "digital_lr"};

    private static BuildSpec spec(String[] base, String battery) {
        return Bench.spec(base[0], base[1], base[2], base[3], base[4], battery);
    }

    private static String[] with(String[] base, int index, String part) {
        String[] copy = base.clone();
        copy[index] = part;
        return copy;
    }

    private static BuildStats stats(String[] base, String battery) {
        return Bench.stats(spec(base, battery));
    }

    private static Set<String> warnings(String[] base, String battery) {
        BuildSpec spec = spec(base, battery);
        BuildCheck check = Bench.check(spec);
        Set<String> out = new HashSet<>(check.warnings());
        PropwashApi.builds().analyze(spec).ifPresent(s -> out.addAll(s.warnings()));
        return out;
    }

    private static List<String> problems(String[] base, String battery) {
        return Bench.check(spec(base, battery)).problems();
    }

    private static BatteryDefinition battery(String path) {
        PartFiles.registerAll();
        return PropwashApi.parts().get(BatteryDefinition.class, Bench.id(path)).orElseThrow();
    }

    private static StackDefinition stack(String path) {
        PartFiles.registerAll();
        return PropwashApi.parts().get(StackDefinition.class, Bench.id(path)).orElseThrow();
    }

    static boolean gt(double a, double b) {
        return a > b + Math.abs(b) * 0.01;
    }

    static boolean lt(double a, double b) {
        return a < b - Math.abs(b) * 0.01;
    }

    private static void fits(String[] base, String battery) {
        assertTrue(problems(base, battery).isEmpty(), battery + " in " + base[0] + ": " + problems(base, battery));
    }

    private static void doesNotFit(String[] base, String battery) {
        assertTrue(problems(base, battery).contains("battery_frame"), battery + " passt in " + base[0]);
    }

    @Test
    public void b1s250Lightest65mmPack() {
        BuildStats b250 = stats(WHOOP_65_LITE, "b1s_250_hv");
        BuildStats b300 = stats(WHOOP_65_LITE, "propwash:b1s_300_hv");
        BuildStats b380 = stats(WHOOP_65_LITE, "b1s_380_hv");
        assertEquals(1.2, mass("propwash:b1s_300_hv") - mass("b1s_250_hv"), 0.05);
        assertTrue(b250.thrustToWeight() > b300.thrustToWeight() && gt(b300.thrustToWeight(), b380.thrustToWeight()));
        assertTrue(lt(b250.hoverFlightTimeMin(), b300.hoverFlightTimeMin()));
        for (String other : List.of("b1s_380_hv", "b1s_450_hv", "b1s_550_hv", "b1s_750_hv")) {
            assertTrue(lt(b250.hoverFlightTimeMin(), stats(WHOOP_65_LITE, other).hoverFlightTimeMin()), other);
        }
        fits(WHOOP_65_LITE, "b1s_250_hv");
        fits(with(WHOOP_65_LITE, 0, "propwash:whoop_65"), "b1s_250_hv");
        assertFalse(warnings(WHOOP_65_LITE, "b1s_250_hv").contains("battery_weak"));
        assertTrue(warnings(with(WHOOP_65_LITE, 2, "m0802_19000"), "b1s_250_hv").contains("battery_weak"));
    }

    @Test
    public void b1s380MoreFlightTime() {
        BuildStats b250 = stats(WHOOP_65_LITE, "b1s_250_hv");
        BuildStats b380 = stats(WHOOP_65_LITE, "b1s_380_hv");
        assertTrue(b380.hoverFlightTimeMin() >= 1.3 * b250.hoverFlightTimeMin());
        assertTrue(lt(b380.thrustToWeight(), b250.thrustToWeight()));
        fits(with(WHOOP_65_LITE, 0, "propwash:whoop_65"), "b1s_380_hv");
    }

    @Test
    public void b1s450Standard75() {
        BuildStats s = stats(WHOOP_75_1S, "b1s_450_hv");
        assertTrue(s.hoverFlightTimeMin() >= 7.0);
        assertFalse(warnings(with(WHOOP_75_1S, 2, "m1102_18000"), "b1s_450_hv").contains("battery_weak"));
        doesNotFit(with(WHOOP_65_LITE, 0, "propwash:whoop_65"), "b1s_450_hv");
    }

    @Test
    public void b1s550AndB1s750LongerButHeavier() {
        BuildStats b450 = stats(WHOOP_75_1S, "b1s_450_hv");
        BuildStats b550 = stats(WHOOP_75_1S, "b1s_550_hv");
        BuildStats b750 = stats(WHOOP_75_1S, "b1s_750_hv");
        assertTrue(b550.hoverFlightTimeMin() >= 1.1 * b450.hoverFlightTimeMin());
        assertTrue(lt(b550.thrustToWeight(), b450.thrustToWeight()));
        assertTrue(gt(b750.hoverFlightTimeMin(), b550.hoverFlightTimeMin()));
        assertTrue(lt(b750.thrustToWeight(), b550.thrustToWeight()));
        for (String path : List.of("b1s_250_hv", "b1s_380_hv", "b1s_450_hv", "b1s_550_hv")) {
            assertTrue(stats(with(WHOOP_75_1S, 0, "whoop_85_duct"), path).hoverFlightTimeMin()
                    < stats(with(WHOOP_75_1S, 0, "whoop_85_duct"), "b1s_750_hv").hoverFlightTimeMin(), path);
        }
        String[] strong = {"whoop_75_duct", "aio_whoop_2s_12a", "m1102_18000", "p40_14_5b_soft", "analog_whoop_25mw"};
        Set<String> w = warnings(strong, "b1s_750_hv");
        assertFalse(w.contains("battery_weak") || w.contains("heavy_sag"), w.toString());
        assertTrue(warnings(with(WHOOP_85_2S, 2, "m1102_18000"), "b1s_750_hv").contains("battery_undersized"));
    }

    @Test
    public void b2s300LightButWeak() {
        BuildStats b2s = stats(WHOOP_75_2S, "b2s_300_hv");
        BuildStats b1s = stats(WHOOP_75_1S, "b1s_450_hv");
        assertTrue(b2s.thrustToWeight() > 1.3 * b1s.thrustToWeight());
        Set<String> w = warnings(WHOOP_85_2S, "b2s_300_hv");
        assertTrue(w.contains("battery_weak") && w.contains("heavy_sag"), w.toString());
        for (String prop : List.of("p40_20_2b_pc", "p40_17_3b_pc", "p40_15_4b_pc", "p40_14_5b_soft")) {
            Set<String> whoop75 = warnings(with(WHOOP_75_2S, 3, prop), "b2s_300_hv");
            assertTrue(whoop75.contains("battery_overload") && whoop75.contains("heavy_sag"), prop + " " + whoop75);
            assertTrue(warnings(with(WHOOP_75_2S, 3, prop), "b2s_450_hv").isEmpty(), prop);
        }
    }

    @Test
    public void b2s450DeliversWithoutSag() {
        assertTrue(warnings(WHOOP_85_2S, "b2s_450_hv").isEmpty());
        assertTrue(stats(WHOOP_85_2S, "b2s_450_hv").hoverFlightTimeMin() >= 7.0);
        assertEquals(8.1, battery("b2s_450_hv").basics().massGrams() - battery("b2s_300_hv").basics().massGrams(), 0.05);
    }

    @Test
    public void b2s650LongestOn2s() {
        BuildStats b650 = stats(TP25_1103, "b2s_650");
        for (String path : List.of("b2s_300_hv", "b2s_450_hv")) {
            assertTrue(gt(b650.hoverFlightTimeMin(), stats(TP25_1103, path).hoverFlightTimeMin()), path);
        }
        assertFalse(warnings(TP25_1103, "b2s_650").contains("heavy_sag"));
        assertTrue(warnings(TP25_1103, "b2s_650").contains("battery_undersized"));
        doesNotFit(WHOOP_85_2S, "b2s_650");
        doesNotFit(new String[]{"tp_2_x", "aio_whoop_2s_12a", "m1103_11000", "p51_20_3b_pc", "analog_whoop_25mw"}, "b2s_650");
    }

    @Test
    public void threeCellPacks() {
        BuildStats b450 = stats(TP25_12025, "b3s_450");
        BuildStats b650 = stats(TP25_12025, "b3s_650");
        assertTrue(gt(b450.thrustToWeight(), b650.thrustToWeight()));
        assertTrue(lt(b450.hoverFlightTimeMin(), b650.hoverFlightTimeMin()));
        Set<String> hot450 = warnings(TP25_1103, "b3s_450");
        assertTrue(hot450.contains("heavy_sag"), hot450.toString());
        assertFalse(warnings(TP25_1103, "b3s_650").contains("heavy_sag"));
    }

    @Test
    public void b4s450LightButLittleCurrent() {
        assertTrue(warnings(TP25_12025, "b4s_450").isEmpty());
        BuildStats b450 = stats(TP3_1404, "b4s_450");
        BuildStats b650 = stats(TP3_1404, "propwash:b4s_650");
        assertTrue(gt(b450.thrustToWeight(), b650.thrustToWeight()));
        assertTrue(lt(b450.hoverFlightTimeMin(), b650.hoverFlightTimeMin()));
        assertTrue(warnings(TP3_1404, "b4s_450").contains("battery_weak"));
    }

    @Test
    public void b4s850ForCinewhoopsAnd35() {
        assertFalse(warnings(TP35_4S, "b4s_850").contains("battery_weak"));
        fits(new String[]{"cw_25_duct", "aio_tp_4s_20a", "m1404_3800", "p65_22_4b_bi", "digital_micro"}, "b4s_850");
        assertTrue(lt(stats(TP3_1404, "b4s_850").thrustToWeight(), stats(TP3_1404, "propwash:b4s_650").thrustToWeight()));
    }

    @Test
    public void b4s1100() {
        assertTrue(stats(TP35_4S, "b4s_1100").hoverFlightTimeMin() >= 14.0);
        assertEquals(40.3, battery("b4s_1500").basics().massGrams() - battery("b4s_1100").basics().massGrams(), 0.05);
        Set<String> race = warnings(RACE5_4S, "b4s_1100");
        assertTrue(race.contains("battery_weak") && race.contains("heavy_sag"), race.toString());
    }

    @Test
    public void b4s1500And1800() {
        assertTrue(warnings(RACE5_4S, "b4s_1500").isEmpty());
        assertTrue(stats(RACE5_4S, "b4s_1500").hoverFlightTimeMin() >= 11.0);
        BuildStats b1500 = stats(RACE5_4S, "b4s_1500");
        BuildStats b1800 = stats(RACE5_4S, "b4s_1800");
        assertTrue(gt(b1800.hoverFlightTimeMin(), b1500.hoverFlightTimeMin()) && b1800.hoverFlightTimeMin() >= 13.0);
        assertTrue(lt(b1800.thrustToWeight(), b1500.thrustToWeight()));
        assertTrue(warnings(with(RACE5_4S, 0, "propwash:freestyle_5"), "b4s_1800").contains("battery_undersized"));
    }

    @Test
    public void sixCellToothpickPacks() {
        fits(TP3_6S, "b6s_550");
        doesNotFit(TP3_6S, "b6s_750");
        assertTrue(stats(TP3_6S, "b6s_550").topSpeedKmh() >= 90.0);
        assertTrue(lt(stats(TP35_6S, "b6s_550").hoverFlightTimeMin(), stats(TP35_6S, "b6s_750").hoverFlightTimeMin()));
        assertTrue(warnings(TP35_6S, "b6s_550").contains("battery_weak"));
        assertTrue(warnings(TP35_6S, "b6s_750").isEmpty());
        assertTrue(stats(TP35_6S, "b6s_750").hoverFlightTimeMin() >= 15.0);
    }

    @Test
    public void b6s850Cinewhoop() {
        assertFalse(warnings(CW35, "b6s_850").contains("battery_weak"));
        assertTrue(stats(CW35, "b6s_850").hoverFlightTimeMin() >= 10.0);
    }

    @Test
    public void b6s1500OnlyInBashFrames() {
        BuildStats b1500 = stats(BASH5, "b6s_1500");
        BuildStats b1300 = stats(BASH5, "propwash:b6s_1300");
        assertTrue(b1500.hoverFlightTimeMin() - b1300.hoverFlightTimeMin() >= 1.0);
        assertTrue(lt(b1500.thrustToWeight(), b1300.thrustToWeight()));
        doesNotFit(FREESTYLE5, "b6s_1500");
        fits(BASH5, "b6s_1500");
    }

    @Test
    public void b6s1800And2200LongRangeLipo() {
        BuildStats b1800 = stats(LR6, "b6s_1800");
        assertTrue(b1800.hoverFlightTimeMin() >= 14.0);
        assertFalse(warnings(LR6, "b6s_1800").contains("heavy_sag"));
        doesNotFit(FREESTYLE5, "b6s_1800");
        doesNotFit(BASH5, "b6s_1800");
        BuildStats b2200 = stats(LR6, "b6s_2200");
        BuildStats lion = stats(LR6, "b6s_3000_lion");
        assertTrue(b2200.hoverFlightTimeMin() >= 16.0);
        assertTrue(b2200.thrustToWeight() > 1.8 * lion.thrustToWeight());
        assertTrue(gt(lion.hoverFlightTimeMin(), b2200.hoverFlightTimeMin()));
    }

    @Test
    public void liIonLongestHoverWeakestPunch() {
        List<String> sixCell = List.of("b6s_1800", "b6s_2200", "b6s_3000_lion", "b6s_4500_lion", "b6s_3000_ss", "propwash:b6s_1300",
                "propwash:b6s_4000_lion");
        String longest = null;
        String weakest = null;
        double hover = -1.0;
        double tw = Double.MAX_VALUE;
        for (String path : sixCell) {
            BuildStats s = stats(LR7_DEADCAT, path);
            if (s.hoverFlightTimeMin() > hover) {
                hover = s.hoverFlightTimeMin();
                longest = path;
            }
            if (s.thrustToWeight() < tw) {
                tw = s.thrustToWeight();
                weakest = path;
            }
        }
        assertEquals("b6s_4500_lion", longest);
        assertTrue(weakest != null && weakest.endsWith("_lion"), weakest);
        BuildStats lion = stats(LR6, "b6s_3000_lion");
        assertTrue(lion.hoverFlightTimeMin() >= 23.5);
        Set<String> w = warnings(LR6, "b6s_3000_lion");
        assertTrue(w.contains("battery_weak") && w.contains("heavy_sag"), w.toString());
        assertTrue(lion.thrustToWeight() <= 0.6 * stats(LR6, "b6s_2200").thrustToWeight());
        BuildStats b4500 = stats(LR7_DEADCAT, "b6s_4500_lion");
        assertTrue(b4500.hoverFlightTimeMin() >= 31.0);
        assertTrue(gt(b4500.hoverFlightTimeMin(), stats(LR7_DEADCAT, "propwash:b6s_4000_lion").hoverFlightTimeMin()));
        assertTrue(b4500.thrustToWeight() > 5.0 && b4500.thrustToWeight() < 5.5);
    }

    @Test
    public void liIonCellsAreSturdy() {
        for (PartDefinition definition : PropwashApi.parts().all(PartKind.BATTERY)) {
            if (definition.id().getNamespace().equals(PartFiles.NAMESPACE) && definition.id().getPath().endsWith("_lion")) {
                assertEquals(1.5, definition.basics().durability(), 1e-9, definition.id().toString());
                assertTrue(((BatteryDefinition) definition).packSize().isPresent(), definition.id() + " ohne pack_mm");
            }
        }
        assertTrue(gt(battery("b6s_3000_ss").basics().durability(), battery("b6s_2200").basics().durability()));
    }

    @Test
    public void b6s8400TwoParallelStrings() {
        BuildStats s = stats(LR8, "b6s_8400_lion");
        assertTrue(s.hoverFlightTimeMin() >= 40.0);
        assertTrue(s.thrustToWeight() < 5.0);
        assertFalse(warnings(LR8, "b6s_8400_lion").contains("heavy_sag"));
        assertTrue(warnings(LR8, "b6s_4500_lion").contains("heavy_sag"));
        doesNotFit(LR7_DEADCAT, "b6s_8400_lion");
        fits(LR8, "b6s_8400_lion");
    }

    @Test
    public void b8s4500LiIon() {
        BuildStats s = stats(LR8, "b8s_4500_lion");
        assertTrue(s.hoverFlightTimeMin() >= 36.0);
        assertTrue(s.hoverFlightTimeMin() >= 0.88 * stats(LR8, "b6s_8400_lion").hoverFlightTimeMin());
        assertTrue(lt(battery("b8s_4500_lion").basics().massGrams(), battery("b6s_8400_lion").basics().massGrams()));
        assertTrue(warnings(LR8, "b8s_4500_lion").contains("heavy_sag"));
        assertTrue(s.thrustToWeight() < 5.0);
        Set<String> sixCellStack = warnings(with(LR8, 1, "stack_30_6s_60a"), "b8s_4500_lion");
        assertTrue(sixCellStack.contains("stack_voltage") && sixCellStack.contains("esc_overvoltage"), sixCellStack.toString());
    }

    @Test
    public void b6s5000XClassOnSixCells() {
        Set<String> w = warnings(XC10, "b6s_5000");
        assertTrue(w.isEmpty(), w.toString());
        assertTrue(Math.abs(stats(XC10, "b6s_5000").hoverFlightTimeMin() - 21.0) < 0.75);
        doesNotFit(LR7_DEADCAT, "b6s_5000");
        assertTrue(warnings(XC12, "b6s_5000").contains("battery_undersized"));
    }

    @Test
    public void semiSolidLighterThanLipo() {
        assertTrue(lt(battery("b6s_3000_ss").basics().massGrams(), battery("b6s_2200").basics().massGrams()));
        BuildStats ss = stats(LR6, "b6s_3000_ss");
        BuildStats lion = stats(LR6, "b6s_3000_lion");
        assertTrue(ss.thrustToWeight() >= 1.7 * lion.thrustToWeight());
        assertTrue(gt(ss.hoverFlightTimeMin(), lion.hoverFlightTimeMin()));
        assertTrue(warnings(LR6, "b6s_3000_ss").contains("battery_overload"));
        assertTrue(lt(battery("b6s_3000_lion").basics().massGrams(), battery("b6s_3000_ss").basics().massGrams()));
        for (String[] build : List.of(LR6, LR4, LR7_DEADCAT)) {
            assertTrue(gt(stats(build, "b6s_3000_lion").cruiseFlightTimeMin(), stats(build, "b6s_3000_ss").cruiseFlightTimeMin()), build[0]);
        }
        double lipo = battery("b8s_5000").basics().massGrams();
        assertTrue(battery("b8s_5000_ss").basics().massGrams() <= 0.67 * lipo);
        BuildStats ss13 = stats(XC13, "b8s_5000_ss");
        BuildStats lipo13 = stats(XC13, "b8s_5000");
        assertTrue(ss13.hoverFlightTimeMin() - lipo13.hoverFlightTimeMin() >= 2.5);
        assertTrue(lt(ss13.topSpeedKmh(), lipo13.topSpeedKmh()));
        Set<String> w = warnings(XC13, "b8s_5000_ss");
        assertTrue(w.contains("battery_weak") && w.contains("battery_overload"), w.toString());
    }

    @Test
    public void eightCellLipos() {
        BuildStats light = stats(XC12, "b8s_2200");
        BuildStats standard = stats(XC12, "b8s_5000");
        assertTrue(gt(light.thrustToWeight(), standard.thrustToWeight()));
        assertTrue(light.hoverFlightTimeMin() < 12.0);
        assertTrue(warnings(XC12, "b8s_2200").contains("battery_undersized"));
        assertTrue(warnings(XC13, "b8s_2200").contains("battery_undersized"));
        String[] deadcat8s = {"lr_7_deadcat", "stack_30_8s_65a", "m2807_1100", "p178_40_2b_cfn", "digital_lr"};
        fits(deadcat8s, "b8s_2200");
        fits(deadcat8s, "b8s_4500_lion");
        assertTrue(warnings(deadcat8s, "b8s_2200").isEmpty());
        assertTrue(warnings(XC13, "b8s_5000").isEmpty());
        assertTrue(stats(XC13, "b8s_5000").hoverFlightTimeMin() >= 16.5);
        assertTrue(warnings(with(XC13, 2, "m4214_530"), "b8s_5000").contains("heavy_sag"));
        double heaviestLipo = 0.0;
        String heaviest = "";
        for (PartDefinition definition : PropwashApi.parts().all(PartKind.BATTERY)) {
            BatteryDefinition b = (BatteryDefinition) definition;
            if (b.chemistry().equals(BatteryDefinition.LIPO) && b.basics().massGrams() > heaviestLipo) {
                heaviestLipo = b.basics().massGrams();
                heaviest = b.id().getPath();
            }
        }
        assertEquals("b8s_5000", heaviest);
    }

    @Test
    public void whoopAios() {
        StackDefinition one = stack("aio_whoop_1s_6a");
        StackDefinition two = stack("aio_whoop_2s_12a");
        assertEquals(2.9, one.brownoutVoltage().orElse(0.0), 1e-9);
        assertEquals(2.9, two.brownoutVoltage().orElse(0.0), 1e-9);
        PartFiles.registerAll();
        double builtin = PropwashApi.parts().get(Bench.id("propwash:aio_whoop")).orElseThrow().basics().massGrams();
        assertEquals(0.6, builtin - one.basics().massGrams(), 1e-9);
        Set<String> overvolted = warnings(with(WHOOP_75_2S, 1, "aio_whoop_1s_6a"), "b2s_300_hv");
        assertTrue(overvolted.contains("stack_voltage") && overvolted.contains("esc_overvoltage"), overvolted.toString());
        assertTrue(warnings(with(WHOOP_75_1S, 2, "m1102_18000"), "b1s_450_hv").contains("esc_undersized"));
        assertFalse(warnings(with(WHOOP_75_1S, 1, "aio_whoop_2s_12a"), "b1s_450_hv").contains("stack_voltage"));
        assertFalse(warnings(with(with(WHOOP_75_1S, 1, "aio_whoop_2s_12a"), 2, "m1102_18000"), "b1s_450_hv").contains("esc_undersized"));
        String[] tp2 = {"tp_2_x", "aio_whoop_2s_12a", "m1103_11000", "p51_20_3b_pc", "analog_whoop_25mw"};
        assertFalse(warnings(tp2, "b2s_450_hv").contains("esc_undersized"));
        assertEquals(1.0, two.basics().massGrams() - one.basics().massGrams(), 1e-9);
        Set<String> threeCell = warnings(with(TP25_12025, 1, "aio_whoop_2s_12a"), "b3s_450");
        assertTrue(threeCell.contains("stack_voltage") && threeCell.contains("esc_overvoltage"), threeCell.toString());
        for (String path : List.of("aio_whoop_1s_6a", "aio_whoop_2s_12a")) {
            BuildStats s = stats(with(WHOOP_75_1S, 1, path), "b1s_450_hv");
            assertFalse(s.warnings().contains("heavy_sag"), path);
        }
    }

    @Test
    public void toothpickAios() {
        assertTrue(stack("aio_tp_4s_20a").basics().massGrams() < stack("aio_tp_6s_20a").basics().massGrams());
        assertEquals(1.2, stack("aio_tp_6s_20a").basics().massGrams() - stack("aio_tp_4s_20a").basics().massGrams(), 1e-9);
        assertFalse(warnings(TP3_1404, "propwash:b4s_650").contains("esc_undersized"));
        for (String path : List.of("aio_tp_4s_20a", "aio_tp_6s_20a")) {
            assertTrue(warnings(with(TP35_4S, 1, path), "b4s_850").contains("esc_undersized"), path);
        }
        Set<String> overvolted = warnings(with(TP3_6S, 1, "aio_tp_4s_20a"), "b6s_550");
        assertTrue(overvolted.contains("stack_voltage") && overvolted.contains("esc_overvoltage"), overvolted.toString());
        assertTrue(warnings(TP35_6S, "b6s_750").isEmpty());
        StackDefinition six = stack("aio_tp_6s_20a");
        assertEquals(2, six.minCells());
        assertEquals(6, six.maxCells());
    }

    @Test
    public void twentyByTwentyBoards() {
        assertFalse(warnings(TP35_4S, "b4s_850").contains("esc_undersized"));
        assertTrue(stats(TP35_4S, "b4s_850").escLoadPercent() < 50.0);
        Set<String> overvolted = warnings(with(CW35, 1, "aio_20_4s_35a"), "b6s_850");
        assertTrue(overvolted.contains("stack_voltage") && overvolted.contains("esc_overvoltage"), overvolted.toString());
        assertTrue(warnings(with(RACE5_4S, 1, "aio_20_4s_35a"), "b4s_1500").contains("esc_undersized"));
        double strongest = 0.0;
        String strongestId = "";
        for (PartDefinition definition : PropwashApi.parts().all(PartKind.STACK)) {
            StackDefinition s = (StackDefinition) definition;
            if (s.id().toString().equals(CREATIVE_FC)) {
                continue;
            }
            if (s.mount().getPath().equals("s20") && s.escContinuousAmps() > strongest) {
                strongest = s.escContinuousAmps();
                strongestId = s.id().getPath();
            }
        }
        assertEquals("aio_20_6s_45a", strongestId);
        assertEquals(3.0, stack("stack_20_6s_40a").basics().massGrams() - stack("aio_20_6s_45a").basics().massGrams(), 1e-9);
        assertTrue(stack("stack_20_6s_40a").basics().durability() > stack("aio_20_6s_45a").basics().durability());
        String[] race6 = {"race_5_stretched", "aio_20_6s_45a", "m23065_2020", "p130_50_3b_gfn", "analog_race_800"};
        assertTrue(problems(race6, "propwash:b6s_1300").isEmpty());
        assertFalse(warnings(race6, "propwash:b6s_1300").contains("esc_undersized"));
        for (String motor : List.of("m2004_1700", "m1505_3600")) {
            assertFalse(warnings(with(with(CW35, 1, "stack_20_6s_40a"), 2, motor), "b6s_850").contains("esc_undersized"), motor);
        }
        assertTrue(warnings(with(BASH5, 1, "stack_20_6s_40a"), "propwash:b6s_1300").contains("esc_undersized"));
    }

    @Test
    public void thirtyByThirtyStacks() {
        assertEquals(5.0, stack("stack_30_6s_60a").basics().massGrams() - stack("stack_30_4s_55a").basics().massGrams(), 1e-9);
        assertFalse(warnings(with(RACE5_4S, 2, "m22075_2700"), "b4s_1500").contains("esc_undersized"));
        Set<String> race6 = warnings(RACE5_4S, "propwash:b6s_1300");
        assertTrue(race6.contains("stack_voltage") && race6.contains("esc_overvoltage"), race6.toString());
        for (String motor : List.of("m2207_1950", "m22075_1850", "m23065_2020", "m2507_1800", "propwash:m2207_1750")) {
            assertFalse(warnings(with(BASH5, 2, motor), "propwash:b6s_1300").contains("esc_undersized"), motor);
        }
        assertTrue(stats(BASH5, "propwash:b6s_1300").escLoadPercent() <= 55.0);
        Set<String> eight = warnings(with(LR8, 1, "stack_30_6s_60a"), "b8s_4500_lion");
        assertTrue(eight.contains("esc_overvoltage"), eight.toString());
        BuildStats eighty = stats(with(BASH5, 1, "stack_30_6s_80a"), "propwash:b6s_1300");
        assertTrue(eighty.escLoadPercent() <= 40.0);
        String[] lr6Strong = with(with(LR6, 1, "stack_30_6s_80a"), 2, "m2507_1800");
        Set<String> strong = warnings(lr6Strong, "b6s_1800");
        assertFalse(strong.contains("esc_undersized") || strong.contains("esc_current_limited"), strong.toString());
        assertFalse(warnings(with(with(LR6, 1, "stack_30_6s_80a"), 2, "m2807_1500"), "b6s_1800").contains("esc_undersized"));
        assertEquals(6.0, stack("stack_30_6s_80a").basics().massGrams() - stack("stack_30_6s_60a").basics().massGrams(), 1e-9);
        assertFalse(warnings(LR8, "b8s_4500_lion").contains("stack_voltage"));
        assertFalse(warnings(LR8, "b6s_8400_lion").contains("stack_voltage"));
        assertEquals(4.0, stack("stack_30_8s_65a").basics().massGrams() - stack("stack_30_6s_60a").basics().massGrams(), 1e-9);
        assertTrue(warnings(with(XC13, 1, "stack_30_8s_65a"), "b8s_5000").isEmpty());
        assertTrue(warnings(with(with(XC13, 1, "stack_30_8s_65a"), 2, "m4214_530"), "b8s_5000").contains("esc_undersized"));
        assertFalse(warnings(with(XC13, 2, "m4214_530"), "b8s_5000").contains("esc_undersized"));
        assertTrue(stats(XC13, "b8s_5000").escLoadPercent() <= 45.0);
        assertTrue(stack("stack_30_8s_100a").basics().massGrams() >= 1.7 * stack("stack_30_8s_65a").basics().massGrams());
    }

    private static String lightest(int cells) {
        PartFiles.registerAll();
        String out = "";
        double mass = Double.MAX_VALUE;
        for (PartDefinition definition : PropwashApi.parts().all(PartKind.BATTERY)) {
            BatteryDefinition b = (BatteryDefinition) definition;
            if (b.cells() == cells && b.basics().massGrams() < mass) {
                mass = b.basics().massGrams();
                out = b.id().getPath();
            }
        }
        return out;
    }

    private static double mass(String path) {
        return battery(path).basics().massGrams();
    }

    @Test
    public void numbersInTheTooltips() {
        assertEquals("b1s_250_hv", lightest(1));
        assertEquals("b4s_450", lightest(4));
        assertEquals("b6s_550", lightest(6));
        fits(TP25_12025, "b4s_450");
        assertEquals(15.1, mass("b6s_850") - mass("b6s_750"), 0.05);
        assertEquals(55.4, mass("b6s_2200") - mass("b6s_1800"), 0.05);
        assertEquals(35.4, mass("b6s_2200") - mass("b6s_3000_lion"), 0.05);
        assertTrue(mass("b8s_4500_lion") <= mass("b6s_8400_lion") - 245.0);
        assertTrue(mass("b8s_2200") <= 0.5 * mass("b8s_5000"));
        assertTrue(battery("b6s_8400_lion").maxContinuousCurrent() >= 1.8 * battery("b6s_4500_lion").maxContinuousCurrent());
        assertEquals(12.5, battery("b1s_250_hv").maxContinuousCurrent(), 1e-9);
        assertEquals(13.5, battery("b2s_300_hv").maxContinuousCurrent(), 1e-9);
        assertEquals(36.0, battery("b4s_450").maxContinuousCurrent(), 1e-9);
        assertEquals(44.0, battery("b6s_550").maxContinuousCurrent(), 1e-9);
        assertEquals(67.5, battery("b6s_750").maxContinuousCurrent(), 1e-9);
        assertEquals(85.0, battery("b6s_850").maxContinuousCurrent(), 1e-9);
        assertEquals(36.0, battery("b6s_3000_lion").maxContinuousCurrent(), 1e-9);
        assertEquals(300.0, battery("b6s_5000").maxContinuousCurrent(), 1e-9);
        assertEquals(300.0, battery("b8s_5000").maxContinuousCurrent(), 1e-9);
        assertEquals(100.0, battery("b8s_5000_ss").maxContinuousCurrent(), 1e-9);
        BuildStats ss13 = stats(XC13, "b8s_5000_ss");
        BuildStats lipo13 = stats(XC13, "b8s_5000");
        assertTrue(ss13.hoverFlightTimeMin() >= 21.0 && ss13.hoverFlightTimeMin() < 22.0 && lipo13.hoverFlightTimeMin() >= 16.5 && lipo13.hoverFlightTimeMin() < 17.5);
        assertTrue(stats(LR8, "b8s_4500_lion").hoverFlightTimeMin() < 40.0);
        fits(TP3_6S, "b6s_550");
        assertTrue(problems(TP35_6S, "b6s_750").isEmpty());
        for (String frame : List.of("propwash:freestyle_5", "propwash:race_5", "fs_5_bash", "fs_5_squashed", "race_5_stretched",
                "deadcat_5", "hframe_5")) {
            String[] base = {frame, "stack_30_6s_60a", "m2207_1950", "p130_41_3b_soft", "digital_hd_pro"};
            doesNotFit(base, "b6s_1800");
        }
        for (PartDefinition definition : PropwashApi.parts().all(PartKind.STACK)) {
            StackDefinition s = (StackDefinition) definition;
            if (!s.id().getNamespace().equals(PartFiles.NAMESPACE)) {
                continue;
            }
            double expected = s.allInOne() ? 1.0 : 1.25;
            assertEquals(expected, s.basics().durability(), 1e-9, s.id().toString());
            assertEquals(s.id().getPath().startsWith("aio_"), s.allInOne(), s.id().toString());
        }
        for (PartDefinition definition : PropwashApi.parts().all(PartKind.MOTOR)) {
            MotorDefinition m = (MotorDefinition) definition;
            if (m.statorLabel().startsWith("22") || m.statorLabel().startsWith("23")) {
                assertFalse(warnings(with(BASH5, 2, m.id().toString()), "propwash:b6s_1300").contains("esc_undersized"), m.id().toString());
            }
        }
    }

    @Test
    public void fourCellStacksReportSixCellsAndBurn() {
        for (String path : List.of("stack_30_4s_55a", "aio_20_4s_35a", "aio_tp_4s_20a")) {
            StackDefinition s = stack(path);
            assertEquals(4, s.maxCells(), path);
        }
        String[] freestyle = {"propwash:freestyle_5", "stack_30_4s_55a", "propwash:m2207_1750", "propwash:p51_3b", "propwash:digital_hd"};
        BuildCheck check = Bench.check(spec(freestyle, "propwash:b6s_1300"));
        assertTrue(check.buildable());
        assertTrue(check.warnings().contains("stack_voltage"), check.warnings().toString());
        assertTrue(stats(freestyle, "propwash:b6s_1300").warnings().contains("esc_overvoltage"));
        assertFalse(warnings(freestyle, "b4s_1500").contains("stack_voltage"));
        assertFalse(warnings(freestyle, "b4s_1500").contains("esc_overvoltage"));
    }
}
