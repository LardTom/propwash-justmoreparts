package de.lardtom.justmoreparts.bench;

import de.lardtom.justmoreparts.preset.JmpPresets;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.BuildCheck;
import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.build.BuildStats;
import de.lardtom.propwash.api.build.DronePreset;
import de.lardtom.propwash.api.part.PartDefinition;
import de.lardtom.propwash.api.part.PartKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FlightTimeOrderTest {
    static final List<PartKind> SLOTS = List.of(PartKind.FRAME, PartKind.STACK, PartKind.MOTOR, PartKind.PROP, PartKind.VIDEO, PartKind.BATTERY);

    static List<BuildSpec> presets() {
        PartFiles.registerAll();
        List<BuildSpec> out = new ArrayList<>();
        for (DronePreset preset : PropwashApi.builds().presets()) {
            out.add(preset.data().build());
        }
        for (JmpPresets.Entry entry : JmpPresets.ALL) {
            out.add(entry.build());
        }
        return out;
    }

    static Map<String, BuildSpec> singleSwaps() {
        Map<String, BuildSpec> out = new LinkedHashMap<>();
        for (BuildSpec preset : presets()) {
            out.putIfAbsent(preset.toString(), preset);
            for (PartKind kind : SLOTS) {
                for (PartDefinition part : PropwashApi.parts().all(kind)) {
                    BuildSpec variant = preset.withPart(kind, part.id());
                    out.putIfAbsent(variant.toString(), variant);
                }
            }
        }
        return out;
    }

    @Test
    public void everySingleSwapOfEveryPresetKeepsTheFlightTimeOrder() {
        List<String> broken = new ArrayList<>();
        int checked = 0;
        for (BuildSpec spec : singleSwaps().values()) {
            BuildCheck check = PropwashApi.builds().check(spec);
            if (!check.known() || !check.buildable()) {
                continue;
            }
            Optional<BuildStats> analysis = PropwashApi.builds().analyze(spec);
            if (analysis.isEmpty() || !Double.isFinite(analysis.get().hoverFlightTimeMin())) {
                continue;
            }
            BuildStats s = analysis.get();
            checked++;
            double hover = s.hoverFlightTimeMin();
            double cruise = s.cruiseFlightTimeMin();
            double mixed = s.mixedFlightTimeMin();
            double aggressive = s.aggressiveFlightTimeMin();
            if (!(Double.isFinite(cruise) && Double.isFinite(mixed) && Double.isFinite(aggressive)
                    && aggressive <= mixed && mixed <= cruise && cruise <= hover)) {
                broken.add(String.format(Locale.ROOT, "%s: aggressiv %.3f, gemischt %.3f, Reisen %.3f, Schweben %.3f", spec, aggressive, mixed,
                        cruise, hover));
            }
        }
        System.out.println(checked + " Varianten mit Flugzeit geprüft");
        assertTrue(checked > 1000, "Varianten " + checked);
        assertEquals(List.of(), broken);
    }
}
