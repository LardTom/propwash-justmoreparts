package de.lardtom.justmoreparts.power;

import de.lardtom.justmoreparts.bench.Bench;
import de.lardtom.justmoreparts.bench.PartFiles;
import de.lardtom.justmoreparts.power.PowerReferences.Corridor;
import de.lardtom.justmoreparts.power.PowerReferences.Metric;
import de.lardtom.justmoreparts.power.PowerReferences.Reference;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.BuildCheck;
import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.build.BuildStats;
import de.lardtom.propwash.api.part.PartDefinition;
import de.lardtom.propwash.api.part.PartKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PowerBenchTest {
    private static final double TOLERANCE = 0.05;

    @Test
    public void everyPowerPartHasAReferenceBuild() {
        PartFiles.registerAll();
        List<String> missing = new ArrayList<>();
        for (PartKind kind : List.of(PartKind.BATTERY, PartKind.STACK)) {
            for (PartDefinition definition : PropwashApi.parts().all(kind)) {
                if (!PartFiles.NAMESPACE.equals(definition.id().getNamespace())) {
                    continue;
                }
                String path = definition.id().getPath();
                if (PowerReferences.ALL.stream().noneMatch(r -> r.uses(path))) {
                    missing.add(path);
                }
            }
        }
        assertTrue(missing.isEmpty(), "ohne Referenzaufbau: " + missing);
    }

    @Test
    public void referenceBuildsStayInsideTheirCorridor() {
        List<String> failures = new ArrayList<>();
        System.out.println(Bench.header());
        for (Reference reference : PowerReferences.ALL) {
            System.out.println(Bench.row(reference.label(), reference.spec()));
            BuildCheck check = Bench.check(reference.spec());
            BuildStats stats = Bench.stats(reference.spec());
            if (!check.problems().isEmpty()) {
                failures.add(reference.label() + ": nicht baubar " + check.problems());
                continue;
            }
            Set<String> warnings = new HashSet<>(check.warnings());
            warnings.addAll(stats.warnings());
            warnings.removeAll(reference.allowedWarnings());
            if (!warnings.isEmpty()) {
                failures.add(reference.label() + ": Warnungen " + warnings);
            }
            Corridor c = reference.corridor();
            range(failures, reference, Metric.MASS, stats.massGrams(), c.massMin, c.massMax);
            range(failures, reference, Metric.THRUST_TO_WEIGHT, stats.thrustToWeight(), c.twMin, c.twMax);
            range(failures, reference, Metric.HOVER_THROTTLE, stats.hoverThrottlePercent(), c.hoverMin, c.hoverMax);
            range(failures, reference, Metric.HOVER_TIME, stats.hoverFlightTimeMin(), c.hoverTimeMin, c.hoverTimeMax);
            range(failures, reference, Metric.CRUISE_TIME, stats.cruiseFlightTimeMin(), c.cruiseTimeMin, c.cruiseTimeMax);
            range(failures, reference, Metric.TOP_SPEED, stats.topSpeedKmh(), c.topSpeedMin, c.topSpeedMax);
            if (!Double.isNaN(c.motorTempMax)) {
                range(failures, reference, Metric.MOTOR_TEMP, stats.sustainedMotorTempC(), 0.0, c.motorTempMax);
            }
            range(failures, reference, Metric.PROPS_IN_VIEW, stats.propsInViewPercent(), 0.0, c.propsInViewMax);
        }
        for (Reference reference : PowerReferences.ALL) {
            if (!reference.reason().isEmpty()) {
                System.out.println("  " + reference.label() + ": " + reference.reason());
            }
        }
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }

    @Test
    public void selfTestBuildsAreClean() {
        Set<String> lion = PowerReferences.LION_WARNINGS;
        Object[][] builds = {
                {Set.of(), "propwash:whoop_65", "aio_whoop_1s_6a", "propwash:m0802_22000", "propwash:p31_3b", "propwash:analog_nano", "b1s_380_hv"},
                {Set.of(), "propwash:whoop_65", "aio_whoop_1s_6a", "propwash:m0802_22000", "propwash:p31_3b", "propwash:analog_nano", "b1s_250_hv"},
                {Set.of(), "propwash:cinewhoop_3", "aio_20_4s_35a", "propwash:m1404_4600", "propwash:p3_3b", "propwash:digital_lite", "b4s_850"},
                {Set.of(), "propwash:cinewhoop_3", "aio_20_4s_35a", "propwash:m1404_4600", "propwash:p3_3b", "propwash:digital_lite", "b3s_650"},
                {Set.of(), "propwash:freestyle_5", "stack_30_6s_60a", "propwash:m2207_1750", "propwash:p51_3b", "propwash:digital_hd",
                        "propwash:b6s_1300"},
                {Set.of(), "propwash:freestyle_5", "stack_30_6s_80a", "propwash:m2207_1750", "propwash:p51_3b", "propwash:digital_hd",
                        "propwash:b6s_1300"},
                {Set.of(), "propwash:long_range_7", "stack_30_6s_60a", "propwash:m2807_1300", "propwash:p7_2b", "propwash:digital_hd", "b6s_2200"},
                {lion, "propwash:long_range_7", "stack_30_8s_65a", "propwash:m2807_1300", "propwash:p7_2b", "propwash:digital_hd", "b6s_4500_lion"},
                {lion, "propwash:long_range_7", "stack_30_6s_60a", "propwash:m2807_1300", "propwash:p7_2b", "propwash:digital_hd", "b6s_3000_ss"},
                {Set.of("heavy_sag", "battery_overload"), "whoop_75_duct", "aio_whoop_2s_12a", "m0802_15500", "p40_20_2b_pc", "analog_whoop_25mw", "b2s_300_hv"},
                {Set.of(), "whoop_85_duct", "aio_whoop_2s_12a", "m1102_13500", "p45_17_4b_bi", "digital_micro", "b2s_450_hv"},
                {Set.of(), "tp_25_x", "aio_tp_4s_20a", "m12025_5500", "p65_25_3b_pc", "digital_micro", "b3s_450"},
                {Set.of(), "tp_35_stretched", "aio_tp_6s_20a", "m1505_2500", "p89_30_3b_pc", "digital_micro", "b6s_750"},
                {Set.of(), "cw_35_duct", "stack_20_6s_40a", "m2004_1700", "p89_25_5b_pc_duct", "digital_hd_pro", "b6s_850"},
                {Set.of(), "race_5_stretched", "stack_30_4s_55a", "m23065_2450", "p130_46_3b_hard", "digital_low_latency", "b4s_1500"},
                {Set.of(), "fs_5_bash", "stack_30_6s_60a", "m2207_1950", "p130_41_3b_soft", "digital_hd_pro", "b6s_1500"},
                {lion, "lr_8_x", "stack_30_8s_65a", "m2807_1100", "p203_45_2b_cfn", "digital_lr", "b8s_4500_lion"},
                {Set.of(), "xclass_10", "stack_30_8s_100a", "m3115_900", "p254_50_2b_carbon", "digital_hd_pro", "b6s_5000"},
                {Set.of(), "xclass_13", "stack_30_8s_100a", "m4214_400", "p330_70_2b_carbon", "digital_lr", "b8s_5000"}};
        List<String> failures = new ArrayList<>();
        for (Object[] b : builds) {
            BuildSpec spec = Bench.spec((String) b[1], (String) b[2], (String) b[3], (String) b[4], (String) b[5], (String) b[6]);
            BuildCheck check = Bench.check(spec);
            Set<String> warnings = new HashSet<>(check.warnings());
            warnings.addAll(Bench.stats(spec).warnings());
            @SuppressWarnings("unchecked")
            Set<String> allowed = (Set<String>) b[0];
            warnings.removeAll(allowed);
            if (!check.problems().isEmpty() || !warnings.isEmpty()) {
                failures.add(b[6] + " in " + b[1] + ": " + check.problems() + " " + warnings);
            }
        }
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }

    private static void range(List<String> failures, Reference reference, Metric metric, double value, double min, double max) {
        boolean inside = value >= min * (1 - TOLERANCE) - 1e-9 && value <= max * (1 + TOLERANCE) + 1e-9;
        if (!inside && !reference.outside().contains(metric)) {
            failures.add(String.format(java.util.Locale.ROOT, "%s: %s %.1f außerhalb %.1f–%.1f", reference.label(), metric, value, min, max));
        }
    }
}
