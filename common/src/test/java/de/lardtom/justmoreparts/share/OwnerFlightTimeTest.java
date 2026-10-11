package de.lardtom.justmoreparts.share;

import de.lardtom.justmoreparts.bench.PartFiles;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.build.BuildStats;
import de.lardtom.propwash.api.part.PartKind;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OwnerFlightTimeTest {
    static final String OWNER_CODE = "PW1-040P7-XB1WG-MCZJ6-F5Y43-ECV5W-97WSK-4FGZR-0S2Z3-SM99K-E3K1Q-60V37-2HQ60-T30C3-3CGN3-0DWD6-S563R-ACSD6"
            + "-CTB29-SY4WS-KJE5Z-46DSJ-A06N3-8D1KH-37JGR-30CG1-RPR1H-X0";

    static BuildStats stats(BuildSpec spec) {
        return PropwashApi.builds().analyze(spec).orElseThrow();
    }

    static void check(List<String> out, String what, double value, double low, double high) {
        if (!(value >= low && value <= high)) {
            out.add(what + " " + value + " außerhalb " + low + "–" + high);
        }
    }

    @Test
    public void ownerWhoopFliesLikeTheRealOne() {
        PartFiles.registerAll();
        ShareDecoded decoded = ShareCodec.decode(OWNER_CODE);
        BuildSpec spec = decoded.require().build();
        assertEquals(Identifier.parse("propwash:whoop_65"), spec.frame());
        assertEquals(Identifier.parse("justmoreparts:m0702_27000"), spec.motor());
        assertEquals(Identifier.parse("propwash:p31_4b"), spec.prop());
        assertEquals(Identifier.parse("propwash:b1s_300_hv"), spec.battery());
        BuildStats owner = stats(spec);
        List<String> out = new ArrayList<>();
        check(out, "Schweben (digital) [min]", owner.hoverFlightTimeMin(), 3.0, 10.0 / 3.0);
        check(out, "Reisen [min]", owner.cruiseFlightTimeMin(), 3.0, 3.5);
        check(out, "Gemischt [min]", owner.mixedFlightTimeMin(), 2.0, 2.5);
        check(out, "Aggressiv [min]", owner.aggressiveFlightTimeMin(), 1.5, 2.0);
        check(out, "Landespannung [V]", owner.landingCellVoltage(), 3.3, 3.5);
        check(out, "Gewicht [g]", owner.massGrams(), 25.0, 30.0);
        check(out, "Vollgasstrom [A]", owner.fullThrottleCurrentA(), 12.0, 16.0);
        BuildStats lowKv = stats(spec.withPart(PartKind.MOTOR, Identifier.parse("justmoreparts:m0702_23000")));
        BuildStats threeBlade = stats(spec.withPart(PartKind.PROP, Identifier.parse("propwash:p31_3b")));
        BuildStats twoBlade = stats(spec.withPart(PartKind.PROP, Identifier.parse("justmoreparts:p31_16_2b_pc")));
        BuildStats analog = stats(spec.withPart(PartKind.VIDEO, Identifier.parse("justmoreparts:analog_whoop_25mw")));
        check(out, "23000 KV gemischt / 27000 KV", lowKv.mixedFlightTimeMin() / owner.mixedFlightTimeMin(), 1.15, 1.25);
        check(out, "23000 KV aggressiv / 27000 KV", lowKv.aggressiveFlightTimeMin() / owner.aggressiveFlightTimeMin(), 1.15, 1.25);
        check(out, "27000 KV mehr Strom gemischt [A]", owner.mixedCurrentA() - lowKv.mixedCurrentA(), 0.3, 3.0);
        check(out, "3-Blatt / 4-Blatt Schweben", threeBlade.hoverFlightTimeMin() / owner.hoverFlightTimeMin(), 1.10, 1.20);
        check(out, "3-Blatt / 4-Blatt Reisen", threeBlade.cruiseFlightTimeMin() / owner.cruiseFlightTimeMin(), 1.10, 1.20);
        check(out, "3-Blatt / 4-Blatt gemischt", threeBlade.mixedFlightTimeMin() / owner.mixedFlightTimeMin(), 1.10, 1.20);
        check(out, "3-Blatt / 4-Blatt aggressiv", threeBlade.aggressiveFlightTimeMin() / owner.aggressiveFlightTimeMin(), 1.10, 1.20);
        check(out, "3-Blatt länger gemischt [s]", 60.0 * (threeBlade.mixedFlightTimeMin() - owner.mixedFlightTimeMin()), 15.0, 30.0);
        check(out, "2-Blatt / 4-Blatt Schweben", twoBlade.hoverFlightTimeMin() / owner.hoverFlightTimeMin(), 1.10, 1.20);
        check(out, "2-Blatt / 4-Blatt Reisen", twoBlade.cruiseFlightTimeMin() / owner.cruiseFlightTimeMin(), 1.10, 1.20);
        check(out, "2-Blatt länger gemischt [s]", 60.0 * (twoBlade.mixedFlightTimeMin() - owner.mixedFlightTimeMin()), 15.0, 30.0);
        check(out, "2-Blatt länger aggressiv [s]", 60.0 * (twoBlade.aggressiveFlightTimeMin() - owner.aggressiveFlightTimeMin()), 15.0, 30.0);
        check(out, "digital statt analog: Schweben kürzer", 1.0 - owner.hoverFlightTimeMin() / analog.hoverFlightTimeMin(), 0.30, 0.35);
        assertEquals(4.1, owner.thrustToWeight(), 0.05);
        assertEquals(43.8, owner.hoverThrottlePercent(), 0.1);
        assertEquals(List.of(), out);
    }
}
