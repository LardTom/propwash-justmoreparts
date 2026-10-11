package de.lardtom.justmoreparts.power;

import de.lardtom.justmoreparts.bench.Bench;
import de.lardtom.propwash.api.build.BuildSpec;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

final class PowerReferences {
    static final Set<String> LION_WARNINGS = Set.of("battery_weak", "heavy_sag", "battery_overload");

    private PowerReferences() {
    }

    enum Metric { MASS, THRUST_TO_WEIGHT, HOVER_THROTTLE, HOVER_TIME, CRUISE_TIME, TOP_SPEED, MOTOR_TEMP, PROPS_IN_VIEW }

    enum Corridor {
        WHOOP(20, 60, 3.5, 7, 25, 50, 2.5, 9, 2.3, 9, 35, 85, 60, 20),
        TOOTHPICK(50, 230, 5, 9, 20, 35, 6, 16, 4, 13, 70, 110, Double.NaN, 15),
        TOOTHPICK_35(50, 230, 5, 9, 20, 35, 6, 16, 4, 13, 100, 130, Double.NaN, 15),
        CINEWHOOP(190, 420, 4, 7, 23, 35, 4.5, 13, 3.5, 11, 80, 150, 100, 25),
        FIVE_INCH(400, 560, 10, 15, 16, 25, 8, 14.5, 5.5, 11, 125, 190, Double.NaN, 15),
        LONG_RANGE(440, 1400, 3.5, 11, 20, 50, 12, 48, 8, 33, 90, 130, Double.NaN, 20),
        X_CLASS(1600, 3400, 5, 9, 25, 40, 12, 26, 8, 18, 100, 140, Double.NaN, 20);

        final double massMin;
        final double massMax;
        final double twMin;
        final double twMax;
        final double hoverMin;
        final double hoverMax;
        final double hoverTimeMin;
        final double hoverTimeMax;
        final double cruiseTimeMin;
        final double cruiseTimeMax;
        final double topSpeedMin;
        final double topSpeedMax;
        final double motorTempMax;
        final double propsInViewMax;

        Corridor(double massMin, double massMax, double twMin, double twMax, double hoverMin, double hoverMax, double hoverTimeMin,
                 double hoverTimeMax, double cruiseTimeMin, double cruiseTimeMax, double topSpeedMin, double topSpeedMax,
                 double motorTempMax, double propsInViewMax) {
            this.massMin = massMin;
            this.massMax = massMax;
            this.twMin = twMin;
            this.twMax = twMax;
            this.hoverMin = hoverMin;
            this.hoverMax = hoverMax;
            this.hoverTimeMin = hoverTimeMin;
            this.hoverTimeMax = hoverTimeMax;
            this.cruiseTimeMin = cruiseTimeMin;
            this.cruiseTimeMax = cruiseTimeMax;
            this.topSpeedMin = topSpeedMin;
            this.topSpeedMax = topSpeedMax;
            this.motorTempMax = motorTempMax;
            this.propsInViewMax = propsInViewMax;
        }
    }

    record Reference(String label, Corridor corridor, String[] parts, Set<String> allowedWarnings, Set<Metric> outside, String reason) {
        BuildSpec spec() {
            return Bench.spec(this.parts[0], this.parts[1], this.parts[2], this.parts[3], this.parts[4], this.parts[5]);
        }

        boolean uses(String part) {
            for (String p : this.parts) {
                if (p.equals(part)) {
                    return true;
                }
            }
            return false;
        }
    }

    static Reference ref(String label, Corridor corridor, String frame, String stack, String motor, String prop, String video, String battery) {
        return new Reference(label, corridor, new String[]{frame, stack, motor, prop, video, battery}, Set.of(), EnumSet.noneOf(Metric.class), "");
    }

    static Reference ref(String label, Corridor corridor, String frame, String stack, String motor, String prop, String video, String battery,
                         Set<String> allowedWarnings, Set<Metric> outside, String reason) {
        return new Reference(label, corridor, new String[]{frame, stack, motor, prop, video, battery}, allowedWarnings,
                outside.isEmpty() ? EnumSet.noneOf(Metric.class) : EnumSet.copyOf(outside), reason);
    }

    static final List<Reference> ALL = List.of(
            ref("Whoop 65 Lite 1S 250", Corridor.WHOOP, "whoop_65_lite", "aio_whoop_1s_6a", "m0702_27000", "p31_14_3b_bi",
                    "analog_whoop_25mw", "b1s_250_hv"),
            ref("Whoop 65 1S 380", Corridor.WHOOP, "propwash:whoop_65", "aio_whoop_1s_6a", "m0702_27000", "p31_16_2b_pc",
                    "analog_whoop_25mw", "b1s_380_hv"),
            ref("Whoop 75 1S 450", Corridor.WHOOP, "whoop_75_duct", "aio_whoop_1s_6a", "m0802_19000", "p40_17_3b_pc",
                    "analog_whoop_25mw", "b1s_450_hv"),
            ref("Whoop 75 1S 550", Corridor.WHOOP, "whoop_75_duct", "aio_whoop_1s_6a", "m0802_19000", "p40_17_3b_pc",
                    "analog_whoop_25mw", "b1s_550_hv", Set.of(), Set.of(Metric.CRUISE_TIME), "größerer Akku: Reiseflug über 10 min"),
            ref("Whoop 75 1S 750", Corridor.WHOOP, "whoop_75_duct", "aio_whoop_2s_12a", "m1102_18000", "p40_14_5b_soft",
                    "analog_whoop_25mw", "b1s_750_hv", Set.of(), Set.of(Metric.CRUISE_TIME), "größter 1S-Akku: Reiseflug über 10 min"),
            ref("Whoop 75 2S 300", Corridor.WHOOP, "whoop_75_duct", "aio_whoop_2s_12a", "m0802_15500", "p40_20_2b_pc",
                    "analog_whoop_25mw", "b2s_300_hv", Set.of("heavy_sag", "battery_overload"), Set.of(Metric.HOVER_THROTTLE),
                    "45C-Akku (13,5 A) gegen 23 A Vollgas mit 40-mm-Props: ehrliches Contra des kleinen 2S-Akkus"),
            ref("Whoop 85 2S 450", Corridor.WHOOP, "whoop_85_duct", "aio_whoop_2s_12a", "m1102_13500", "p45_17_4b_bi",
                    "digital_micro", "b2s_450_hv"),
            ref("Toothpick 2,5\" 2S 650", Corridor.TOOTHPICK, "tp_25_x", "aio_tp_4s_20a", "m1103_8000", "p65_25_3b_pc",
                    "analog_whoop_25mw", "b2s_650", Set.of("battery_undersized"), Set.of(Metric.CRUISE_TIME, Metric.PROPS_IN_VIEW),
                    "2,5″-Rahmen bevorzugt 3S (Hinweis, der 2″-Rahmen trägt höchstens 30 g); großer Akku: Reiseflug über 8 min; 15,4 % Props im Bild (Rahmen)"),
            ref("Toothpick 2,5\" 3S 450", Corridor.TOOTHPICK, "tp_25_x", "aio_tp_4s_20a", "m12025_5500", "p65_25_3b_pc",
                    "digital_micro", "b3s_450"),
            ref("Toothpick 2,5\" 3S 650", Corridor.TOOTHPICK, "tp_25_x", "aio_tp_4s_20a", "m12025_5500", "p65_25_3b_pc",
                    "digital_micro", "b3s_650", Set.of(), Set.of(Metric.THRUST_TO_WEIGHT, Metric.HOVER_THROTTLE, Metric.CRUISE_TIME),
                    "schwerer Akku: weniger Schub/Gewicht, dafür längerer Reiseflug (ehrliches Contra)"),
            ref("Toothpick 2,5\" 4S 450", Corridor.TOOTHPICK, "tp_25_x", "aio_tp_4s_20a", "m12025_5500", "p65_25_3b_pc",
                    "digital_micro", "b4s_450"),
            ref("Toothpick 3\" 4S 650 (Propwash)", Corridor.TOOTHPICK, "tp_3_x", "aio_tp_4s_20a", "m1404_3800", "p76_25_3b_hard",
                    "digital_micro", "propwash:b4s_650"),
            ref("Toothpick 3,5\" 4S 850", Corridor.TOOTHPICK_35, "tp_35_stretched", "aio_20_4s_35a", "m1505_3600", "p89_30_3b_pc",
                    "digital_micro", "b4s_850"),
            ref("Toothpick 3,5\" 4S 1100", Corridor.TOOTHPICK_35, "tp_35_stretched", "aio_20_4s_35a", "m1505_3600", "p89_30_3b_pc",
                    "digital_micro", "b4s_1100", Set.of(), Set.of(Metric.HOVER_TIME, Metric.CRUISE_TIME),
                    "großer Akku für 3,5″: längere Flugzeit"),
            ref("Cinewhoop 2,5\" 4S 850", Corridor.CINEWHOOP, "cw_25_duct", "aio_tp_4s_20a", "m1404_3800", "p65_22_4b_bi",
                    "digital_micro", "b4s_850"),
            ref("Race 5\" 4S 1500", Corridor.FIVE_INCH, "race_5_stretched", "stack_30_4s_55a", "m23065_2450", "p130_46_3b_hard",
                    "digital_low_latency", "b4s_1500"),
            ref("Race 5\" 4S 1800", Corridor.FIVE_INCH, "race_5_stretched", "stack_30_4s_55a", "m23065_2450", "p130_46_3b_hard",
                    "digital_low_latency", "b4s_1800", Set.of(), Set.of(Metric.HOVER_TIME, Metric.CRUISE_TIME),
                    "größter 4S-Akku: längere Flugzeit"),
            ref("Toothpick 3\" 6S 550", Corridor.TOOTHPICK, "tp_3_x", "aio_tp_6s_20a", "m1404_2750", "p76_25_3b_hard",
                    "digital_micro", "b6s_550"),
            ref("Toothpick 3,5\" 6S 750", Corridor.TOOTHPICK_35, "tp_35_stretched", "aio_tp_6s_20a", "m1505_2500", "p89_30_3b_pc",
                    "digital_micro", "b6s_750"),
            ref("Cinewhoop 3,5\" 6S 850", Corridor.CINEWHOOP, "cw_35_duct", "aio_20_6s_45a", "m2004_1700", "p89_25_5b_pc_duct",
                    "digital_hd_pro", "b6s_850"),
            ref("Cinewhoop 3,5\" 6S 850 Stack", Corridor.CINEWHOOP, "cw_35_duct", "stack_20_6s_40a", "m2004_1700", "p89_25_5b_pc_duct",
                    "digital_hd_pro", "b6s_850"),
            ref("Bash 5\" 6S 1500", Corridor.FIVE_INCH, "fs_5_bash", "stack_30_6s_60a", "m2207_1950", "p130_41_3b_soft",
                    "digital_hd_pro", "b6s_1500"),
            ref("Bash 5\" 6S 1300 80 A", Corridor.FIVE_INCH, "fs_5_bash", "stack_30_6s_80a", "m2207_1950", "p130_41_3b_soft",
                    "digital_hd_pro", "propwash:b6s_1300"),
            ref("Long Range 6\" 6S 1800", Corridor.LONG_RANGE, "lr_6_x", "stack_30_6s_60a", "m2507_1500", "p152_33_3b_pc",
                    "digital_lr", "b6s_1800", Set.of(), Set.of(Metric.CRUISE_TIME), "Preset lr6_6s (FRAMES): Reiseflug 9,7 statt 10 min"),
            ref("Long Range 6\" 6S 2200", Corridor.LONG_RANGE, "lr_6_x", "stack_30_6s_60a", "m2507_1500", "p152_33_3b_pc",
                    "digital_lr", "b6s_2200", Set.of(), Set.of(Metric.TOP_SPEED), "LiPo ohne Einbruch: 1–3 km/h über dem Korridor"),
            ref("Long Range 6\" 6S 3000 Li-Ion", Corridor.LONG_RANGE, "lr_6_x", "stack_30_6s_60a", "m2507_1500", "p152_33_3b_pc",
                    "digital_lr", "b6s_3000_lion", LION_WARNINGS, Set.of(), "Li-Ion: wenig Strom (erlaubt wie propwash:long_range)"),
            ref("Long Range 6\" 6S 3000 Semi-Solid", Corridor.LONG_RANGE, "lr_6_x", "stack_30_6s_60a", "m2507_1500", "p152_33_3b_pc",
                    "digital_lr", "b6s_3000_ss", LION_WARNINGS, Set.of(), "Semi-Solid: mittlere C-Rate, Überlast bei Vollgas"),
            ref("Deadcat LR 7\" 6S 4500 Li-Ion", Corridor.LONG_RANGE, "lr_7_deadcat", "stack_30_6s_60a", "m28065_1300", "p178_40_2b_cfn",
                    "digital_lr", "b6s_4500_lion", LION_WARNINGS, Set.of(), "Li-Ion: wenig Strom"),
            ref("Long Range 8\" 6S2P 8400 Li-Ion", Corridor.LONG_RANGE, "lr_8_x", "stack_30_8s_65a", "m2807_1100", "p203_45_2b_cfn",
                    "digital_lr", "b6s_8400_lion", LION_WARNINGS, Set.of(), "Li-Ion: wenig Strom"),
            ref("Long Range 8\" 8S 4500 Li-Ion", Corridor.LONG_RANGE, "lr_8_x", "stack_30_8s_65a", "m2807_1100", "p203_45_2b_cfn",
                    "digital_lr", "b8s_4500_lion", LION_WARNINGS, Set.of(Metric.PROPS_IN_VIEW), "Li-Ion: wenig Strom; 10,1 % Props im Bild (Rahmen)"),
            ref("X-Class 10\" 6S 5000", Corridor.X_CLASS, "xclass_10", "stack_30_8s_100a", "m3115_900", "p254_50_2b_carbon",
                    "digital_hd_pro", "b6s_5000"),
            ref("X-Class 12\" 8S 2200", Corridor.X_CLASS, "xclass_12", "stack_30_8s_100a", "m4214_400", "p305_60_2b_cfn",
                    "digital_lr", "b8s_2200", Set.of("battery_undersized"), Set.of(Metric.HOVER_TIME, Metric.CRUISE_TIME),
                    "leichter Akku: kurze Flugzeit, Rahmen meldet „Akku schwach“ (ehrliches Contra)"),
            ref("X-Class 13\" 8S 5000", Corridor.X_CLASS, "xclass_13", "stack_30_8s_100a", "m4214_400", "p330_70_2b_carbon",
                    "digital_lr", "b8s_5000"),
            ref("X-Class 13\" 8S 5000 Semi-Solid", Corridor.X_CLASS, "xclass_13", "stack_30_8s_100a", "m4214_400", "p330_70_2b_carbon",
                    "digital_lr", "b8s_5000_ss", LION_WARNINGS, Set.of(), "Semi-Solid: wenig C, Überlast bei Vollgas"));
}
