package de.lardtom.justmoreparts.motor.client;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.dev.SelfTest;
import de.lardtom.justmoreparts.dev.SelfTest.StepResult;
import de.lardtom.justmoreparts.preset.JmpPresets;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.BuildStats;
import de.lardtom.propwash.api.drone.AutonomyEnd;
import de.lardtom.propwash.api.drone.AutonomyOptions;
import de.lardtom.propwash.api.drone.AutonomyResult;
import de.lardtom.propwash.api.drone.DroneView;
import de.lardtom.propwash.api.event.PropwashEvents;
import de.lardtom.propwash.api.flight.AssistOutput;
import de.lardtom.propwash.api.flight.FlightAssist;
import de.lardtom.propwash.api.flight.FlightState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class RaceCourseSelfTests {
    public static final String STEP = "motor: Renn-Whoop 65 fliegt durch einen Indoor-Kurs";
    static final String PRESET = "whoop65_race_1s";
    static final Identifier FLIGHT = JustMoreParts.id("race_course");
    static final int OFFSET_X = 48;
    static final int HALF_WIDTH = 4;
    static final int LENGTH = 62;
    static final int HEIGHT = 6;
    static final double START_Z = 2.5;
    static final double TAKEOFF_M = 1.5;
    static final double FINISH_Z = 54.5;
    static final double GATE_SPEED = 8.0;
    static final double SPRINT_SPEED = 16.0;
    static final double HEADING_SOUTH = 180.0;
    static final double MIN_SPRINT_KMH = 20.0;
    static final double MIN_TW = 5.0;
    static final double MIN_HOVER_PERCENT = 20.0;
    static final double MIN_MIXED_MIN = 2.5;
    static final int MAX_TICKS = 2400;
    static final Gate[] GATES = {new Gate(8, -3, 1), new Gate(14, 1, 2), new Gate(20, -3, 0), new Gate(26, -1, 3)};

    private static final Map<UUID, AutonomyEnd> ENDS = new ConcurrentHashMap<>();
    private static boolean listening;

    private RaceCourseSelfTests() {
    }

    record Gate(int z, int x0, int y0) {
        double cx() {
            return this.x0 + 1.5;
        }

        double cy() {
            return this.y0 + 1.0;
        }

        double plane() {
            return this.z + 0.5;
        }

        boolean inside(double rx, double ry) {
            return rx >= this.x0 && rx <= this.x0 + 3.0 && ry >= this.y0 && ry <= this.y0 + 2.0;
        }
    }

    record Waypoint(Vec3 at, double speed) {
    }

    public static void register() {
        SelfTest.clientStep(STEP, MAX_TICKS, RaceCourseSelfTests::tick);
    }

    private static synchronized void listen() {
        if (listening) {
            return;
        }
        listening = true;
        PropwashEvents.AUTONOMY_ENDED.register((drone, owner, id, reason) -> {
            if (FLIGHT.equals(id)) {
                ENDS.put(drone.getUUID(), reason);
            }
        });
    }

    private static StepResult tick(SelfTest.StepContext ctx) {
        Map<String, Object> state = ctx.state();
        if (!state.containsKey("setup")) {
            listen();
            List<String> errors = new CopyOnWriteArrayList<>();
            Map<String, Object> shared = new ConcurrentHashMap<>();
            state.put("errors", errors);
            state.put("shared", shared);
            state.put("setup", ctx.onServer(server -> setup(server, shared, errors)));
            return StepResult.pending();
        }
        CompletableFuture<?> setup = (CompletableFuture<?>) state.get("setup");
        if (!setup.isDone()) {
            return StepResult.pending();
        }
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) state.get("errors");
        @SuppressWarnings("unchecked")
        Map<String, Object> shared = (Map<String, Object>) state.get("shared");
        if (setup.isCompletedExceptionally() || !errors.isEmpty() || !shared.containsKey("course")) {
            return finish(ctx, StepResult.fail(setup.isCompletedExceptionally() ? "Aufbau mit Ausnahme abgebrochen" : String.join("; ", errors)));
        }
        Course course = (Course) shared.get("course");
        UUID drone = (UUID) shared.get("drone");
        AutonomyEnd end = ENDS.get(drone);
        if (!course.miss.isEmpty()) {
            return finish(ctx, StepResult.fail(course.miss));
        }
        if (end == null) {
            if (ctx.tick() > MAX_TICKS - 40) {
                return finish(ctx, StepResult.fail(String.format(Locale.ROOT, "Kurs nicht beendet: %d von %d Toren", course.passed, GATES.length)));
            }
            return StepResult.pending();
        }
        if (end != AutonomyEnd.LANDED || course.passed < GATES.length || Double.isNaN(course.finishTime)) {
            return finish(ctx, StepResult.fail(String.format(Locale.ROOT, "Flug endet mit %s nach %d von %d Toren", end, course.passed, GATES.length)));
        }
        double kmh = course.sprintTop * 3.6;
        ctx.log(String.format(Locale.ROOT, "%s: %d von %d Toren und Sprint in %.1f s, Tore mit höchstens %.1f km/h, Sprint %.1f km/h, Akku %.0f → %.0f %%, gelandet",
                PRESET, course.passed, GATES.length, course.finishTime - course.startTime, course.gateTop * 3.6, kmh,
                course.startCharge * 100.0, course.minCharge * 100.0));
        if (!(shared.get("stats") instanceof BuildStats stats)) {
            return finish(ctx, StepResult.fail("keine Analyse des Presets"));
        }
        ctx.log(String.format(Locale.ROOT, "Werkbank: T/W %.1f, Schwebegas %.1f %%, %.0f km/h, gemischt %.1f min, Motor %.0f °C, %s",
                stats.thrustToWeight(), stats.hoverThrottlePercent(), stats.topSpeedKmh(), stats.mixedFlightTimeMin(),
                stats.sustainedMotorTempC(), stats.warnings()));
        if (!(stats.thrustToWeight() >= MIN_TW && stats.hoverThrottlePercent() >= MIN_HOVER_PERCENT && stats.mixedFlightTimeMin() >= MIN_MIXED_MIN)) {
            return finish(ctx, StepResult.fail(String.format(Locale.ROOT, "nicht spritzig und fliegbar: T/W %.1f, Schwebegas %.1f %%, gemischt %.1f min",
                    stats.thrustToWeight(), stats.hoverThrottlePercent(), stats.mixedFlightTimeMin())));
        }
        return finish(ctx, StepResult.check(kmh >= MIN_SPRINT_KMH, String.format(Locale.ROOT, "Sprint nur %.1f km/h", kmh)));
    }

    private static void setup(MinecraftServer server, Map<String, Object> state, List<String> errors) {
        try {
            prepare(server, state, errors);
        } catch (RuntimeException e) {
            errors.add("Aufbau: " + e);
        }
    }

    private static void prepare(MinecraftServer server, Map<String, Object> state, List<String> errors) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            errors.add("kein Spieler auf dem Server");
            return;
        }
        ServerPlayer player = players.getFirst();
        ServerLevel level = player.level();
        BlockPos at = player.blockPosition();
        int x = at.getX() + OFFSET_X;
        int z = at.getZ();
        BlockPos o = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
        state.put("origin", o);
        state.put("blocks", build(level, o));
        player.setGameMode(GameType.CREATIVE);
        player.teleportTo(level, o.getX() + 0.5, o.getY(), o.getZ() - 3.0, Set.of(), 0.0F, 10.0F, true);
        JmpPresets.Entry entry = JmpPresets.byPath(PRESET);
        PropwashApi.builds().analyze(entry.build()).ifPresent(stats -> state.put("stats", stats));
        ItemStack stack = PropwashApi.builds().createDrone(entry.data());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        Vec3 start = new Vec3(o.getX() + 0.5, o.getY(), o.getZ() + START_Z);
        BlockPos ground = BlockPos.containing(start.x, o.getY() - 1, start.z);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(start, Direction.UP, ground, false)));
        player.getInventory().clearContent();
        player.inventoryMenu.sendAllDataToRemote();
        Optional<DroneView> view = PropwashApi.drones().dronesNear(level, start, 2.0, false).stream().findFirst();
        if (view.isEmpty()) {
            errors.add("Drohne nicht gesetzt");
            return;
        }
        Entity drone = view.get().entity();
        state.put("drone", drone.getUUID());
        ENDS.remove(drone.getUUID());
        Course course = new Course(o);
        AutonomyResult result = PropwashApi.drones().autonomy().start(drone, player, course,
                AutonomyOptions.of(FLIGHT).label("COURSE").maxDuration(Duration.ofSeconds(100)).takeoff(TAKEOFF_M));
        if (result != AutonomyResult.STARTED) {
            errors.add("Autonomer Flug startet nicht: " + result);
            return;
        }
        state.put("course", course);
    }

    private static Map<BlockPos, BlockState> build(ServerLevel level, BlockPos o) {
        Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
        for (int dx = -HALF_WIDTH - 1; dx <= HALF_WIDTH + 1; dx++) {
            for (int dz = -1; dz <= LENGTH + 1; dz++) {
                for (int dy = -1; dy <= HEIGHT; dy++) {
                    BlockPos pos = o.offset(dx, dy, dz);
                    saved.put(pos.immutable(), level.getBlockState(pos));
                    boolean shell = dy == -1 || dy == HEIGHT || Math.abs(dx) == HALF_WIDTH + 1 || dz == -1 || dz == LENGTH + 1;
                    BlockState block = dy == -1 ? Blocks.SMOOTH_STONE.defaultBlockState()
                            : shell ? Blocks.GLASS.defaultBlockState() : Blocks.AIR.defaultBlockState();
                    for (Gate gate : GATES) {
                        if (!shell && dz == gate.z() && !(dx >= gate.x0() && dx < gate.x0() + 3 && dy >= gate.y0() && dy < gate.y0() + 2)) {
                            block = Blocks.STONE_BRICKS.defaultBlockState();
                        }
                    }
                    level.setBlockAndUpdate(pos, block);
                }
            }
        }
        for (Entity entity : level.getEntities((Entity) null, hall(o), e -> !(e instanceof Player))) {
            entity.discard();
        }
        return saved;
    }

    private static AABB hall(BlockPos o) {
        return new AABB(o.getX() - HALF_WIDTH - 1, o.getY() - 1, o.getZ() - 1, o.getX() + HALF_WIDTH + 2, o.getY() + HEIGHT + 1, o.getZ() + LENGTH + 2);
    }

    private static StepResult finish(SelfTest.StepContext ctx, StepResult result) {
        @SuppressWarnings("unchecked")
        Map<String, Object> shared = (Map<String, Object>) ctx.state().get("shared");
        if (shared != null && shared.containsKey("origin")) {
            BlockPos o = (BlockPos) shared.get("origin");
            @SuppressWarnings("unchecked")
            Map<BlockPos, BlockState> saved = (Map<BlockPos, BlockState>) shared.get("blocks");
            UUID drone = (UUID) shared.get("drone");
            ctx.onServer(server -> {
                ServerLevel level = server.getPlayerList().getPlayers().isEmpty() ? server.overworld()
                        : server.getPlayerList().getPlayers().getFirst().level();
                for (Entity entity : level.getEntities((Entity) null, hall(o), e -> !(e instanceof Player))) {
                    if (drone != null && drone.equals(entity.getUUID())) {
                        PropwashApi.drones().autonomy().stop(entity);
                    }
                    entity.discard();
                }
                if (saved != null) {
                    saved.forEach(level::setBlockAndUpdate);
                }
            });
        }
        return result;
    }

    static final class Course implements FlightAssist {
        final BlockPos o;
        final List<Waypoint> waypoints = new ArrayList<>();
        volatile int passed;
        volatile String miss = "";
        volatile double gateTop;
        volatile double sprintTop;
        volatile double startTime = Double.NaN;
        volatile double finishTime = Double.NaN;
        volatile double startCharge = Double.NaN;
        volatile double minCharge = 1.0;
        private int next;
        private double lastZ = Double.NaN;
        private boolean landing;

        Course(BlockPos o) {
            this.o = o;
            for (Gate gate : GATES) {
                this.waypoints.add(new Waypoint(this.at(gate.cx(), gate.cy(), gate.plane() - 2.0), GATE_SPEED));
                this.waypoints.add(new Waypoint(this.at(gate.cx(), gate.cy(), gate.plane() + 1.5), GATE_SPEED));
            }
            this.waypoints.add(new Waypoint(this.at(0.5, 2.0, FINISH_Z), SPRINT_SPEED));
        }

        private Vec3 at(double rx, double ry, double rz) {
            return new Vec3(this.o.getX() + rx, this.o.getY() + ry, this.o.getZ() + rz);
        }

        @Override
        public void update(FlightState s, AssistOutput out) {
            if (Double.isNaN(this.startTime)) {
                this.startTime = s.time();
                this.startCharge = s.batteryCharge();
            }
            this.minCharge = Math.min(this.minCharge, s.batteryCharge());
            double speed = Math.sqrt(s.vx() * s.vx() + s.vy() * s.vy() + s.vz() * s.vz());
            if (this.passed < GATES.length) {
                this.gateTop = Math.max(this.gateTop, speed);
            } else if (!this.landing) {
                this.sprintTop = Math.max(this.sprintTop, speed);
            }
            this.crossing(s);
            if (this.landing) {
                out.land();
                return;
            }
            Waypoint target = this.waypoints.get(this.next);
            double dx = target.at().x - s.x();
            double dy = target.at().y - s.y();
            double dz = target.at().z - s.z();
            double horizontal = Math.hypot(dx, dz);
            if (horizontal < 0.6 && Math.abs(dy) < 0.6) {
                if (this.next == this.waypoints.size() - 1) {
                    this.landing = true;
                    this.finishTime = s.time();
                    out.land();
                    return;
                }
                this.next++;
                target = this.waypoints.get(this.next);
                dx = target.at().x - s.x();
                dz = target.at().z - s.z();
                horizontal = Math.hypot(dx, dz);
            }
            double v = Math.min(target.speed(), Math.max(1.5, 1.2 * horizontal));
            if (horizontal > 1.0E-3) {
                out.velocity(dx / horizontal * v, dz / horizontal * v);
            } else {
                out.velocity(0.0, 0.0);
            }
            out.maxSpeed(target.speed());
            out.holdAltitude(target.at().y);
            out.heading(HEADING_SOUTH);
            out.label("COURSE");
        }

        private void crossing(FlightState s) {
            if (!Double.isNaN(this.lastZ) && this.passed < GATES.length && this.miss.isEmpty()) {
                Gate gate = GATES[this.passed];
                double plane = this.o.getZ() + gate.plane();
                if (this.lastZ < plane && s.z() >= plane) {
                    double rx = s.x() - this.o.getX();
                    double ry = s.y() - this.o.getY();
                    if (gate.inside(rx, ry)) {
                        this.passed++;
                    } else {
                        this.miss = String.format(Locale.ROOT, "Tor %d verfehlt bei x %.2f, y %.2f", this.passed + 1, rx, ry);
                    }
                }
            }
            this.lastZ = s.z();
        }

        @Override
        public void reset() {
            this.lastZ = Double.NaN;
        }
    }
}
