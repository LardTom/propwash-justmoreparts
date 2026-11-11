package de.lardtom.justmoreparts.motor.client;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.dev.SelfTest;
import de.lardtom.justmoreparts.dev.SelfTest.StepResult;
import de.lardtom.justmoreparts.preset.JmpPresets;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.BuildCheck;
import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.build.BuildStats;
import de.lardtom.propwash.api.build.DroneData;
import de.lardtom.propwash.api.drone.AutonomyEnd;
import de.lardtom.propwash.api.drone.AutonomyOptions;
import de.lardtom.propwash.api.drone.AutonomyResult;
import de.lardtom.propwash.api.drone.BlockContact;
import de.lardtom.propwash.api.drone.CollisionFlags;
import de.lardtom.propwash.api.drone.CollisionQuery;
import de.lardtom.propwash.api.drone.CollisionRule;
import de.lardtom.propwash.api.drone.DroneView;
import de.lardtom.propwash.api.event.PropwashEvents;
import de.lardtom.propwash.api.part.PartKind;
import net.minecraft.client.Minecraft;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class CovWhoopMotorSelfTests {
    public static final String GROUP = "motor: Abdeckung cov-jmp – ";
    public static final String FITS_STEP = GROUP + "0802/27200 auf Whoop 75 · 1S und Propwash-Whoop baubar, erzeugt, zurückgelesen und schneller";
    public static final String COURSE_STEP = GROUP + "Renn-Whoop 65 im Indoor-Kurs ohne Absturz und ohne Kontakt, Kurs abgeschlossen";
    static final String NEW_0802 = "m0802_27200";
    static final String PRESET = "whoop65_race_1s";
    static final Identifier FLIGHT = JustMoreParts.id("cov_race_course");
    static final Identifier CONTACTS = JustMoreParts.id("cov_course_contacts");
    static final int OFFSET_X = 72;
    static final int MAX_TICKS = 2400;
    static final int SYNC_TICKS = 60;
    static final double TOUCH_GAP = 0.02;

    private static final Map<UUID, AutonomyEnd> ENDS = new ConcurrentHashMap<>();
    private static final AtomicReference<Tracker> TRACKER = new AtomicReference<>();
    private static boolean listening;

    private CovWhoopMotorSelfTests() {
    }

    public static void register() {
        SelfTest.clientStep(FITS_STEP, 200, CovWhoopMotorSelfTests::fitsStep);
        SelfTest.clientStep(COURSE_STEP, MAX_TICKS, CovWhoopMotorSelfTests::courseStep);
    }

    record Pair(String label, BuildSpec fitted, BuildSpec original) {
    }

    static Identifier pw(String path) {
        return Identifier.fromNamespaceAndPath(PropwashApi.MOD_ID, path);
    }

    static List<Pair> pairs() {
        BuildSpec whoop75 = JmpPresets.byPath("whoop75_1s").build();
        BuildSpec propwash = BuildSpec.of(pw("whoop_65"), pw("aio_whoop"), pw("m0802_22000"), pw("p31_4b"), pw("analog_nano"), pw("b1s_300_hv"));
        return List.of(new Pair("Whoop 75 · 1S", whoop75.withPart(PartKind.MOTOR, JustMoreParts.id(NEW_0802)), whoop75),
                new Pair("Propwash-Whoop", propwash.withPart(PartKind.MOTOR, JustMoreParts.id(NEW_0802)), propwash));
    }

    private static StepResult fitsStep(SelfTest.StepContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        Map<String, Object> state = ctx.state();
        List<Pair> pairs = pairs();
        int index = (int) state.getOrDefault("index", 0);
        if (index >= pairs.size()) {
            return StepResult.pass();
        }
        Pair pair = pairs.get(index);
        if (!state.containsKey("given")) {
            BuildCheck check = PropwashApi.builds().check(pair.fitted());
            if (!check.known() || !check.buildable() || !check.problems().isEmpty()) {
                return StepResult.fail(pair.label() + " mit " + NEW_0802 + ": bekannt " + check.known() + ", baubar " + check.buildable() + ", Probleme "
                        + check.problems() + ", unbekannt " + check.unknownParts());
            }
            Optional<BuildStats> fitted = PropwashApi.builds().analyze(pair.fitted());
            Optional<BuildStats> original = PropwashApi.builds().analyze(pair.original());
            if (fitted.isEmpty() || original.isEmpty()) {
                return StepResult.fail(pair.label() + ": keine Analyse");
            }
            if (!(fitted.get().topSpeedKmh() > original.get().topSpeedKmh())) {
                return StepResult.fail(String.format(Locale.ROOT, "%s: %s %.1f km/h nicht schneller als %s %.1f km/h", pair.label(), NEW_0802,
                        fitted.get().topSpeedKmh(), pair.original().motor(), original.get().topSpeedKmh()));
            }
            ctx.log(String.format(Locale.ROOT, "%s: %s %.1f km/h statt %.1f km/h mit %s, Warnungen %s %s", pair.label(), NEW_0802,
                    fitted.get().topSpeedKmh(), original.get().topSpeedKmh(), pair.original().motor(), check.warnings(), fitted.get().warnings()));
            DroneData data = DroneData.of(pair.fitted());
            state.put("given", ctx.onServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ItemStack drone = PropwashApi.builds().createDrone(data);
                player.setItemInHand(InteractionHand.MAIN_HAND, drone);
                player.inventoryMenu.sendAllDataToRemote();
            }));
            state.put("since", ctx.tick());
            return StepResult.pending();
        }
        CompletableFuture<?> given = (CompletableFuture<?>) state.get("given");
        if (given.isCompletedExceptionally()) {
            return StepResult.fail(pair.label() + ": Drohne nicht gegeben " + given.exceptionNow());
        }
        if (!given.isDone() || mc.player == null) {
            return StepResult.pending();
        }
        ItemStack held = mc.player.getMainHandItem();
        Optional<DroneData> read = PropwashApi.builds().isDrone(held) ? PropwashApi.builds().read(held) : Optional.empty();
        if (read.isEmpty() || !read.get().build().equals(pair.fitted())) {
            if (ctx.tick() - (int) state.get("since") > SYNC_TICKS) {
                return StepResult.fail(pair.label() + ": Drohne in der Hand liest den Bauplan nicht zurück: " + read.map(DroneData::build));
            }
            return StepResult.pending();
        }
        ctx.log(pair.label() + ": Drohne erzeugt und in der Hand zurückgelesen, Motor " + read.get().build().motor());
        ctx.onServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            player.getInventory().clearContent();
            player.inventoryMenu.sendAllDataToRemote();
        });
        state.remove("given");
        state.put("index", index + 1);
        return index + 1 >= pairs.size() ? StepResult.pass() : StepResult.pending();
    }

    static final class Tracker {
        final UUID drone;
        final BlockPos o;
        final RaceCourseSelfTests.Course course;
        final List<String> contacts = new CopyOnWriteArrayList<>();
        final List<String> damage = new CopyOnWriteArrayList<>();
        final AtomicInteger near = new AtomicInteger();

        Tracker(UUID drone, BlockPos o, RaceCourseSelfTests.Course course) {
            this.drone = drone;
            this.o = o;
            this.course = course;
        }

        void contact(BlockContact contact) {
            if (!this.drone.equals(contact.drone().getUUID())) {
                return;
            }
            Entity drone = contact.drone();
            BlockPos pos = contact.pos();
            AABB hull = drone.getBoundingBox();
            double gap = gap(hull, contact.state().getCollisionShape(contact.level(), pos).bounds().move(pos));
            boolean touching = gap <= TOUCH_GAP;
            boolean floor = pos.getY() < this.o.getY() && contact.side() == Direction.UP;
            boolean inFlight = this.course.passed > 0 && Double.isNaN(this.course.finishTime);
            if (!touching) {
                this.near.incrementAndGet();
                return;
            }
            if (floor && !inFlight) {
                return;
            }
            Vec3 v = drone.getDeltaMovement();
            this.contacts.add(String.format(Locale.ROOT, "%s an %d %d %d (Seite %s, Abstand %.3f, Aufprall %.2f m/s, %s, nach Tor %d, Drohne x %.2f "
                            + "y %.2f z %.2f, v %.2f/%.2f/%.2f Blöcke je Tick)", contact.state().getBlock().getName().getString(), pos.getX() - this.o.getX(),
                    pos.getY() - this.o.getY(), pos.getZ() - this.o.getZ(), contact.side(), gap, contact.impactSpeed(),
                    contact.level().isClientSide() ? "Client" : "Server", this.course.passed, drone.getX() - this.o.getX(), drone.getY() - this.o.getY(),
                    drone.getZ() - this.o.getZ(), v.x, v.y, v.z));
        }

        static double gap(AABB a, AABB b) {
            double dx = Math.max(0.0, Math.max(b.minX - a.maxX, a.minX - b.maxX));
            double dy = Math.max(0.0, Math.max(b.minY - a.maxY, a.minY - b.maxY));
            double dz = Math.max(0.0, Math.max(b.minZ - a.maxZ, a.minZ - b.maxZ));
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
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
        PropwashEvents.DRONE_DAMAGED.register(damage -> {
            Tracker tracker = TRACKER.get();
            if (tracker != null && tracker.drone.equals(damage.drone().getUUID())) {
                tracker.damage.add(String.format(Locale.ROOT, "Schaden %.2f (%s)", damage.originalAmount(), damage.source().getMsgId()));
            }
        });
        PropwashApi.drones().registerCollisionRule(CONTACTS, new CollisionRule() {
            @Override
            public CollisionFlags flags(CollisionQuery query) {
                return CollisionFlags.SOLID;
            }

            @Override
            public void onBlockContact(BlockContact contact) {
                Tracker tracker = TRACKER.get();
                if (tracker != null) {
                    tracker.contact(contact);
                }
            }
        });
    }

    private static StepResult courseStep(SelfTest.StepContext ctx) {
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
        if (setup.isCompletedExceptionally() || !errors.isEmpty() || !shared.containsKey("tracker")) {
            return finish(ctx, StepResult.fail(setup.isCompletedExceptionally() ? "Aufbau mit Ausnahme abgebrochen" : String.join("; ", errors)));
        }
        Tracker tracker = (Tracker) shared.get("tracker");
        RaceCourseSelfTests.Course course = tracker.course;
        if (!course.miss.isEmpty()) {
            return finish(ctx, StepResult.fail(course.miss));
        }
        AutonomyEnd end = ENDS.get(tracker.drone);
        if (end == null) {
            if (ctx.tick() > MAX_TICKS - 40) {
                return finish(ctx, StepResult.fail(String.format(Locale.ROOT, "Kurs nicht beendet: %d von %d Toren",
                        course.passed, RaceCourseSelfTests.GATES.length)));
            }
            return StepResult.pending();
        }
        if (end != AutonomyEnd.LANDED || course.passed < RaceCourseSelfTests.GATES.length || Double.isNaN(course.finishTime)) {
            return finish(ctx, StepResult.fail(String.format(Locale.ROOT, "Flug endet mit %s nach %d von %d Toren", end, course.passed,
                    RaceCourseSelfTests.GATES.length)));
        }
        if (!tracker.damage.isEmpty() || !tracker.contacts.isEmpty()) {
            return finish(ctx, StepResult.fail(String.format(Locale.ROOT, "Kurs abgeschlossen und gelandet, aber %d Abstürze %s und %d Kontakte mit Wänden, "
                    + "Toren, Decke oder Boden im Flug: %s", tracker.damage.size(), tracker.damage, tracker.contacts.size(),
                    tracker.contacts.subList(0, Math.min(8, tracker.contacts.size())))));
        }
        ctx.log(String.format(Locale.ROOT, "%s: %d von %d Toren und Sprint in %.1f s, Sprint %.1f km/h, Akku %.0f → %.0f %%, gelandet, 0 Abstürze, "
                        + "0 Kontakte mit Wänden, Toren und Decke, kein Bodenkontakt im Flug (%d Oberflächen-Meldungen ohne Berührung)", PRESET, course.passed,
                RaceCourseSelfTests.GATES.length, course.finishTime - course.startTime, course.sprintTop * 3.6, course.startCharge * 100.0,
                course.minCharge * 100.0, tracker.near.get()));
        return finish(ctx, StepResult.pass());
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
        ItemStack stack = PropwashApi.builds().createDrone(entry.data());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        Vec3 start = new Vec3(o.getX() + 0.5, o.getY(), o.getZ() + RaceCourseSelfTests.START_Z);
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
        RaceCourseSelfTests.Course course = new RaceCourseSelfTests.Course(o);
        Tracker tracker = new Tracker(drone.getUUID(), o, course);
        TRACKER.set(tracker);
        AutonomyResult result = PropwashApi.drones().autonomy().start(drone, player, course,
                AutonomyOptions.of(FLIGHT).label("COURSE").maxDuration(Duration.ofSeconds(100)).takeoff(RaceCourseSelfTests.TAKEOFF_M));
        if (result != AutonomyResult.STARTED) {
            errors.add("Autonomer Flug startet nicht: " + result);
            return;
        }
        state.put("tracker", tracker);
    }

    private static Map<BlockPos, BlockState> build(ServerLevel level, BlockPos o) {
        Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
        int half = RaceCourseSelfTests.HALF_WIDTH;
        int length = RaceCourseSelfTests.LENGTH;
        int height = RaceCourseSelfTests.HEIGHT;
        for (int dx = -half - 1; dx <= half + 1; dx++) {
            for (int dz = -1; dz <= length + 1; dz++) {
                for (int dy = -1; dy <= height; dy++) {
                    BlockPos pos = o.offset(dx, dy, dz);
                    saved.put(pos.immutable(), level.getBlockState(pos));
                    boolean shell = dy == -1 || dy == height || Math.abs(dx) == half + 1 || dz == -1 || dz == length + 1;
                    BlockState block = dy == -1 ? Blocks.SMOOTH_STONE.defaultBlockState()
                            : shell ? Blocks.GLASS.defaultBlockState() : Blocks.AIR.defaultBlockState();
                    for (RaceCourseSelfTests.Gate gate : RaceCourseSelfTests.GATES) {
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
        int half = RaceCourseSelfTests.HALF_WIDTH;
        return new AABB(o.getX() - half - 1, o.getY() - 1, o.getZ() - 1, o.getX() + half + 2, o.getY() + RaceCourseSelfTests.HEIGHT + 1,
                o.getZ() + RaceCourseSelfTests.LENGTH + 2);
    }

    private static StepResult finish(SelfTest.StepContext ctx, StepResult result) {
        TRACKER.set(null);
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
}
