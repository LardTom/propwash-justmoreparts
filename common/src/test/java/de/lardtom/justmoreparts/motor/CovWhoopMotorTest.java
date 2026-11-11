package de.lardtom.justmoreparts.motor;

import de.lardtom.justmoreparts.bench.Bench;
import de.lardtom.justmoreparts.preset.JmpPresets;
import de.lardtom.propwash.api.build.BuildCheck;
import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.build.BuildStats;
import de.lardtom.propwash.api.part.PartKind;
import de.lardtom.propwash.apiimpl.BuildAccessImpl;
import de.lardtom.propwash.build.DroneBuild;
import de.lardtom.propwash.sim.DroneSim;
import de.lardtom.propwash.sim.FlightControllerConfig;
import de.lardtom.propwash.sim.PilotInput;
import de.lardtom.propwash.sim.SimEnvironment;
import de.lardtom.propwash.sim.SimTuning;
import de.lardtom.propwash.sim.StepResult;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CovWhoopMotorTest {
    static final String NEW_0802 = "m0802_27200";
    static final String OLD_JMP_0802 = "m0802_19000";
    static final String OLD_PROPWASH_0802 = "propwash:m0802_22000";
    static final String RACE_PRESET = "whoop65_race_1s";
    static final long FRAME = 10_000_000L;
    static final int SETTLE_STEPS = 300;
    static final int PUNCH_STEPS = 100;
    static final int AFTER_STEPS = 1500;
    static final double MIN_SAG_PER_CELL = 0.3;

    static BuildSpec propwashWhoop(String motor) {
        return Bench.spec("propwash:whoop_65", "propwash:aio_whoop", motor, "propwash:p31_4b", "propwash:analog_nano", "propwash:b1s_300_hv");
    }

    static BuildSpec whoop75(String motor) {
        return JmpPresets.byPath("whoop75_1s").build().withPart(PartKind.MOTOR, Bench.id(motor));
    }

    static void assertFitsWithoutProblems(String label, BuildSpec spec) {
        BuildCheck check = Bench.check(spec);
        assertTrue(check.known(), label + ": unbekannte Teile " + check.unknownParts());
        assertTrue(check.buildable(), label + ": nicht baubar " + check.problems());
        assertTrue(check.problems().isEmpty(), label + ": Probleme " + check.problems());
    }

    @Test
    public void new0802FitsTheJmpWhoop75AndIsFasterThanTheOld0802() {
        BuildSpec preset = JmpPresets.byPath("whoop75_1s").build();
        assertEquals(Bench.id(OLD_JMP_0802), preset.motor());
        BuildSpec fitted = whoop75(NEW_0802);
        assertFitsWithoutProblems("whoop75_1s mit " + NEW_0802, fitted);
        BuildStats before = Bench.stats(preset);
        BuildStats after = Bench.stats(fitted);
        assertTrue(after.topSpeedKmh() > before.topSpeedKmh(), String.format(Locale.ROOT, "whoop75_1s: %s %.1f km/h, %s %.1f km/h",
                NEW_0802, after.topSpeedKmh(), OLD_JMP_0802, before.topSpeedKmh()));
    }

    @Test
    public void new0802FitsThePropwashWhoopAndIsFasterThanThePropwash0802() {
        DroneBuild builtin = DroneBuild.WHOOP;
        BuildSpec original = propwashWhoop(OLD_PROPWASH_0802);
        assertEquals(builtin.frame().id(), original.frame());
        assertEquals(builtin.motor().id(), original.motor());
        assertEquals(builtin.prop().id(), original.prop());
        assertEquals(builtin.battery().id(), original.battery());
        BuildSpec fitted = propwashWhoop(NEW_0802);
        assertFitsWithoutProblems("propwash:whoop_65 mit " + NEW_0802, fitted);
        BuildStats before = Bench.stats(original);
        BuildStats after = Bench.stats(fitted);
        assertTrue(after.topSpeedKmh() > before.topSpeedKmh(), String.format(Locale.ROOT, "propwash:whoop_65: %s %.1f km/h, %s %.1f km/h",
                NEW_0802, after.topSpeedKmh(), OLD_PROPWASH_0802, before.topSpeedKmh()));
    }

    record Punch(double before, double first, double built, double instant, double quick, double late, boolean armed, double hoverStick,
                 double peakCurrent) {
        String text() {
            return String.format(Locale.ROOT, "Schweben %.2f V je Zelle bei Gas %.2f, Einbruch gegen das Schweben nach 20 ms %.3f V, nach 1 s %.3f V "
                            + "(Spitze %.1f A), nach dem Gas-Rücknehmen fehlen sofort %.3f V, nach 2 s %.3f V, nach 15 s %.3f V", this.before,
                    this.hoverStick, this.first, this.built, this.peakCurrent, this.instant, this.quick, this.late);
        }
    }

    static DroneSim hovering(DroneBuild build, double charge, PilotInput input, double hoverStick) {
        DroneSim sim = new DroneSim(build, new FlightControllerConfig(), new SimTuning(), 7L);
        SimEnvironment env = new SimEnvironment();
        StepResult result = new StepResult();
        sim.reset(0.0, 200.0, 0.0, 0.0);
        sim.setBatteryCharge(charge);
        sim.step(0L, input, env, result);
        input.armSwitch = true;
        sim.step(0L, input, env, result);
        input.throttle = hoverStick;
        for (int k = 0; k < SETTLE_STEPS; k++) {
            sim.step(FRAME, input, env, result);
        }
        return sim;
    }

    static Punch punch(BuildSpec spec, double charge) {
        double hoverStick = Bench.stats(spec).hoverThrottlePercent() / 100.0;
        DroneBuild build = BuildAccessImpl.requireBuild(spec);
        PilotInput input = new PilotInput();
        PilotInput steady = new PilotInput();
        DroneSim sim = hovering(build, charge, input, hoverStick);
        DroneSim reference = hovering(build, charge, steady, hoverStick);
        SimEnvironment env = new SimEnvironment();
        StepResult result = new StepResult();
        double before = sim.telemetry().batteryCellVoltage;
        input.throttle = 1.0;
        double first = Double.NaN;
        double peak = 0.0;
        for (int k = 0; k < PUNCH_STEPS; k++) {
            sim.step(FRAME, input, env, result);
            reference.step(FRAME, steady, env, result);
            peak = Math.max(peak, sim.telemetry().batteryCurrentA);
            if (k == 1) {
                first = reference.telemetry().batteryCellVoltage - sim.telemetry().batteryCellVoltage;
            }
        }
        double built = reference.telemetry().batteryCellVoltage - sim.telemetry().batteryCellVoltage;
        boolean armed = sim.telemetry().armed;
        input.throttle = hoverStick;
        double[] missing = new double[AFTER_STEPS];
        for (int k = 0; k < AFTER_STEPS; k++) {
            sim.step(FRAME, input, env, result);
            reference.step(FRAME, steady, env, result);
            missing[k] = reference.telemetry().batteryCellVoltage - sim.telemetry().batteryCellVoltage;
        }
        return new Punch(before, first, built, missing[0], missing[199], missing[AFTER_STEPS - 1], armed, hoverStick, peak);
    }

    @Test
    public void raceWhoopSagsNoticeablyDuringAOneSecondPunchAndComesBackInTwoStages() {
        BuildSpec spec = JmpPresets.byPath(RACE_PRESET).build();
        for (double charge : new double[]{1.0, 0.5}) {
            Punch p = punch(spec, charge);
            String where = String.format(Locale.ROOT, "%s bei %.0f %% Ladung: %s", RACE_PRESET, charge * 100.0, p.text());
            System.out.println(where);
            assertTrue(p.armed(), where + ": schaltet im Punch ab");
            assertTrue(p.built() >= MIN_SAG_PER_CELL, where + ": Einbruch unter " + MIN_SAG_PER_CELL + " V je Zelle");
            assertTrue(p.built() > p.first(), where + ": Einbruch wächst während des Vollgases nicht");
            assertTrue(p.instant() < 0.6 * p.built(), where + ": erste Stufe fehlt (sofort zurück)");
            assertTrue(p.instant() > 0.05 * p.built(), where + ": alles kommt sofort zurück, zweite Stufe fehlt");
            assertTrue(p.quick() < 0.6 * p.instant(), where + ": zweite Stufe zu langsam");
            assertTrue(p.late() < 0.5 * p.quick(), where + ": erholt sich nicht ganz");
        }
    }
}
