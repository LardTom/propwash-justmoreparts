package de.lardtom.justmoreparts.dev;

import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class SelfTest {
    public static final String PROPERTY = "justmoreparts.selftest";
    public static final String SMOKE_PROPERTY = "justmoreparts.smoketest";
    public static final String RESULT_FILE = "justmoreparts-selftest-result.txt";
    public static final String ONLY_ENV = "JMP_SELFTEST_ONLY";

    private static final boolean ENABLED = Boolean.getBoolean(PROPERTY);
    private static final boolean SMOKE = !ENABLED && Boolean.getBoolean(SMOKE_PROPERTY);
    private static final List<Step> CLIENT_STEPS = new ArrayList<>();
    private static final Set<String> NAMES = new HashSet<>();
    private static boolean started;

    private SelfTest() {
    }

    public static boolean enabled() {
        return ENABLED;
    }

    public static boolean smokeEnabled() {
        return SMOKE;
    }

    public static void clientStep(String name, int maxTicks, ClientStep body) {
        Objects.requireNonNull(body, "body");
        if (maxTicks <= 0) {
            throw new IllegalArgumentException("maxTicks muss > 0 sein");
        }
        if (started) {
            throw new IllegalStateException("Selbsttest-Schritt '" + name + "' zu spät registriert");
        }
        if (!NAMES.add(name)) {
            throw new IllegalArgumentException("Doppelter Selbsttest-Schritt: " + name);
        }
        CLIENT_STEPS.add(new Step(name, maxTicks, body));
    }

    public static List<Step> startClientSteps() {
        started = true;
        String only = System.getenv(ONLY_ENV);
        if (only == null || only.isBlank()) {
            return Collections.unmodifiableList(CLIENT_STEPS);
        }
        List<String> prefixes = List.of(only.split("\\|"));
        List<Step> kept = new ArrayList<>();
        for (Step step : CLIENT_STEPS) {
            if (prefixes.stream().anyMatch(prefix -> step.name().startsWith(prefix.trim()))) {
                kept.add(step);
            }
        }
        return Collections.unmodifiableList(kept);
    }

    public record Step(String name, int maxTicks, ClientStep body) {
    }

    @FunctionalInterface
    public interface ClientStep {
        StepResult tick(StepContext context);
    }

    public interface StepContext {
        int tick();

        @Nullable MinecraftServer server();

        CompletableFuture<Void> onServer(Consumer<MinecraftServer> action);

        Map<String, Object> state();

        void log(String message);
    }

    public record StepResult(Status status, String message) {
        public enum Status { PENDING, PASS, FAIL }

        private static final StepResult PENDING = new StepResult(Status.PENDING, "");
        private static final StepResult PASS = new StepResult(Status.PASS, "");

        public static StepResult pending() {
            return PENDING;
        }

        public static StepResult pass() {
            return PASS;
        }

        public static StepResult fail(String reason) {
            return new StepResult(Status.FAIL, reason);
        }

        public static StepResult check(boolean condition, String reason) {
            return condition ? PASS : fail(reason);
        }
    }

    public static Map<String, Object> newState() {
        return new HashMap<>();
    }
}
