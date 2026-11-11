package de.lardtom.justmoreparts.power.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.dev.SelfTest;
import de.lardtom.justmoreparts.dev.SelfTest.StepContext;
import de.lardtom.justmoreparts.dev.SelfTest.StepResult;
import de.lardtom.justmoreparts.dev.client.Screenshots;
import de.lardtom.justmoreparts.preset.JmpPresets;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.DroneData;
import de.lardtom.propwash.api.build.PaintSpec;
import de.lardtom.propwash.api.build.TuneSpec;
import de.lardtom.propwash.api.part.PartDefinition;
import de.lardtom.propwash.api.part.PartKind;
import de.lardtom.propwash.build.DroneBuild;
import de.lardtom.propwash.core.CoreComponents;
import de.lardtom.propwash.drone.DroneModule;
import de.lardtom.propwash.drone.QuadcopterEntity;
import de.lardtom.propwash.drone.client.render.DroneAssembly;
import de.lardtom.propwash.drone.client.render.DroneRenderConfig;
import de.lardtom.propwash.drone.client.render.DroneRenderResources;
import de.lardtom.propwash.drone.client.render.DroneVisualScale;
import de.lardtom.propwash.drone.client.render.DroneVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class PowerPlugSelfTests {
    public static final String PREFIX = "power: Akku gesteckt – ";
    public static final String SHOT_PREFIX = "bildschirmfotos: Akku gesteckt – ";
    public static final String FILE_PREFIX = "jmp_battery_plugged_";
    private static final Set<String> PLUG = Set.of("connector", "connector_grip", "bt_connector");
    private static final String PIGTAIL = "pigtail";
    private static final String MOUNTED = "_mounted";
    private static final String PAINT = "_paint";
    private static final int OFFSET_X = 72;
    private static final int OFFSET_Z = -72;
    private static final int FOV = 30;
    private static final double SIDE_DEGREES = 35.0;
    private static final double TOP_PITCH = 30.0;
    private static final double UNDER_PITCH = 12.0;
    private static final double RANGE_PER_RADIUS = 1.5;
    private static final double MIN_RANGE = 0.15;
    private static final double MAX_RANGE = 0.8;
    private static final int STAND = 2;
    private static final String FUTURE = "future";
    private static final AtomicReference<BlockPos> BASE = new AtomicReference<>();
    private static final AtomicInteger DRONE = new AtomicInteger(-1);
    private static int savedFov = -1;

    private PowerPlugSelfTests() {
    }

    public static void register() {
        SelfTest.clientStep(PREFIX + "gesteckte Stack-Varianten und Akku-Buchsen aus JMP geladen", 40, PowerPlugSelfTests::resourcesStep);
        if (!Screenshots.enabled()) {
            return;
        }
        SelfTest.clientStep(SHOT_PREFIX + "Fenster und Platz vorbereiten", 400, PowerPlugSelfTests::prepareStep);
        for (Shot shot : shots()) {
            Screenshots.step(SHOT_PREFIX + shot.label(), FILE_PREFIX + shot.file(), 25, new RearScene(shot));
        }
        SelfTest.clientStep(SHOT_PREFIX + "aufräumen", 200, PowerPlugSelfTests::cleanupStep);
    }

    private record Shot(String file, String label, @Nullable DroneData data, @Nullable DroneBuild builtin) {
        ItemStack stack() {
            if (this.builtin != null) {
                ItemStack stack = new ItemStack(DroneModule.DRONE_ITEM.get());
                stack.set(CoreComponents.DRONE_BUILD.get(), this.builtin);
                return stack;
            }
            return PropwashApi.builds().createDrone(this.data);
        }

        DroneBuild build() {
            DroneBuild build = this.stack().get(CoreComponents.DRONE_BUILD.get());
            if (build == null) {
                throw new IllegalStateException(this.file + ": Drohnen-Item ohne Aufbau");
            }
            return build;
        }
    }

    static List<Shot> shots() {
        List<Shot> out = new ArrayList<>();
        Set<String> frames = new TreeSet<>();
        List<JmpPresets.Entry> entries = new ArrayList<>(JmpPresets.ALL);
        entries.add(new JmpPresets.Entry("whoop65_1s", JmpPresets.SizeClass.WHOOP, "whoop_65_lite", "aio_whoop_1s_6a", "m0802_19000",
                "p31_14_3b_bi", "analog_whoop_25mw", "b1s_250_hv", 0x1B1D20, JmpPresets.NO_COLOR, 0xE6EDF3));
        entries.add(new JmpPresets.Entry("toothpick25_2s", JmpPresets.SizeClass.TOOTHPICK, "tp_25_x", "aio_whoop_2s_12a", "m1404_3800",
                "p65_22_4b_bi", "digital_micro", "b2s_300_hv", 0x1B1D20, JmpPresets.NO_COLOR, 0xF5C518));
        for (JmpPresets.Entry entry : entries) {
            if (frames.add(entry.frame())) {
                out.add(new Shot(entry.frame(), entry.frame() + " (" + entry.stack() + ", " + entry.battery() + ")", entry.data(), null));
            }
        }
        JmpPresets.Entry bash = JmpPresets.byPath("bash5_6s");
        out.add(new Shot(bash.frame() + "_unpainted", bash.frame() + " ohne Lack (" + bash.stack() + ")",
                new DroneData(bash.build(), PaintSpec.DEFAULT, TuneSpec.EMPTY, Optional.empty()), null));
        for (DroneBuild build : DroneBuild.PRESETS) {
            out.add(new Shot("propwash_" + build.frame().key(), "Gegenprobe Propwash " + build.frame().key(), null, build));
        }
        return out;
    }

    private static List<String> names(JsonObject model) {
        List<String> out = new ArrayList<>();
        JsonElement elements = model.get("elements");
        if (elements != null && elements.isJsonArray()) {
            for (JsonElement e : elements.getAsJsonArray()) {
                JsonObject element = e.getAsJsonObject();
                out.add(element.has("name") ? element.get("name").getAsString() : "");
            }
        }
        return out;
    }

    private static @Nullable JsonObject json(ResourceManager manager, Identifier file) {
        Optional<Resource> resource = manager.getResource(file);
        if (resource.isEmpty()) {
            return null;
        }
        try (Reader reader = resource.get().openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static Optional<String> pack(ResourceManager manager, Identifier file) {
        return manager.getResource(file).map(Resource::sourcePackId);
    }

    private static Identifier file(String folder, String key) {
        return JustMoreParts.id(folder + "/" + key + ".json");
    }

    private static StepResult resourcesStep(StepContext ctx) {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        List<String> errors = new ArrayList<>();
        int stacks = 0;
        Set<String> packs = new TreeSet<>();
        for (PartDefinition definition : PropwashApi.parts().all(PartKind.STACK)) {
            if (!JustMoreParts.MOD_ID.equals(definition.id().getNamespace())) {
                continue;
            }
            String id = definition.id().getPath();
            JsonObject base = json(manager, file("models/part/stack", id));
            if (base == null || names(base).stream().noneMatch(PLUG::contains)) {
                errors.add(id + ": Grundmodell fehlt oder ohne Stecker");
                continue;
            }
            for (String variant : List.of(id, id + PAINT)) {
                String mounted = variant.endsWith(PAINT) ? id + MOUNTED + PAINT : id + MOUNTED;
                Identifier baseItem = file("items/drone_render/stack", variant);
                Identifier mountedItem = file("items/drone_render/stack", mounted);
                Identifier baseModel = file("models/part/stack", variant);
                Identifier mountedModel = file("models/part/stack", mounted);
                Optional<String> itemPack = pack(manager, baseItem);
                Optional<String> modelPack = pack(manager, baseModel);
                if (itemPack.isEmpty() || !itemPack.equals(pack(manager, mountedItem)) || modelPack.isEmpty()
                        || !modelPack.equals(pack(manager, mountedModel))) {
                    errors.add(mounted + ": fehlt oder aus einem anderen Ressourcenpaket als " + variant);
                    continue;
                }
                packs.add(itemPack.get());
                JsonObject model = json(manager, mountedModel);
                List<String> got = model == null ? List.of() : names(model);
                if (got.stream().anyMatch(PLUG::contains) || got.stream().filter(PIGTAIL::equals).count() != 2) {
                    errors.add(mounted + ": Stecker-Element vorhanden oder nicht genau zwei " + PIGTAIL);
                    continue;
                }
                stacks++;
            }
        }
        int sockets = 0;
        for (PartDefinition definition : PropwashApi.parts().all(PartKind.BATTERY)) {
            if (!JustMoreParts.MOD_ID.equals(definition.id().getNamespace())) {
                continue;
            }
            String id = definition.id().getPath();
            for (String variant : List.of(id, id + PAINT, id + "_under", id + "_under" + PAINT)) {
                JsonObject model = json(manager, file("models/part/battery", variant));
                if (model == null) {
                    if (variant.equals(id)) {
                        errors.add(id + ": Akku-Modell fehlt");
                    }
                    continue;
                }
                if (socket(model) == null) {
                    errors.add(variant + ": keine Buchse");
                } else {
                    sockets++;
                }
            }
        }
        if (!errors.isEmpty()) {
            return StepResult.fail(String.join("; ", errors));
        }
        ctx.log(stacks + " gesteckte Stack-Modelle (_mounted und _mounted_paint) aus " + packs + ", " + sockets + " Akku-Modelle mit Buchse");
        return StepResult.check(stacks == 24 && sockets == 88, stacks + " statt 24 gesteckte Stack-Modelle, " + sockets + " statt 88 Akku-Buchsen");
    }

    private static float @Nullable [] socket(JsonObject model) {
        float[] box = null;
        JsonElement elements = model.get("elements");
        if (elements == null || !elements.isJsonArray()) {
            return null;
        }
        for (JsonElement e : elements.getAsJsonArray()) {
            JsonObject element = e.getAsJsonObject();
            String name = element.has("name") ? element.get("name").getAsString() : "";
            if (!name.equals("connector") && !name.equals("bt_connector")) {
                continue;
            }
            float[] next = new float[6];
            for (int axis = 0; axis < 3; axis++) {
                float a = (element.getAsJsonArray("from").get(axis).getAsFloat() - 8.0F) * DroneVisualScale.MILLIMETRES_PER_MODEL_UNIT;
                float b = (element.getAsJsonArray("to").get(axis).getAsFloat() - 8.0F) * DroneVisualScale.MILLIMETRES_PER_MODEL_UNIT;
                next[axis] = Math.min(a, b);
                next[axis + 3] = Math.max(a, b);
            }
            if (box == null) {
                box = next;
            } else {
                for (int axis = 0; axis < 3; axis++) {
                    box[axis] = Math.min(box[axis], next[axis]);
                    box[axis + 3] = Math.max(box[axis + 3], next[axis + 3]);
                }
            }
        }
        return box != null && box[3] > box[0] && box[4] > box[1] && box[5] > box[2] ? box : null;
    }

    static Vec3 plugCenter(DroneBuild build, Vec3 feet) {
        DroneAssembly assembly = DroneRenderResources.INSTANCE.assembly(build);
        DroneAssembly.Part battery = assembly.battery();
        Vector3f local = new Vector3f(battery.position());
        float[] box = null;
        if (battery.model() != null && battery.model().modelFile() != null) {
            JsonObject model = json(Minecraft.getInstance().getResourceManager(), battery.model().modelFile());
            box = model == null ? null : socket(model);
        }
        if (box != null) {
            float length = box[5] - box[2];
            local.add((box[0] + box[3]) * 0.5F, (box[1] + box[4]) * 0.5F, box[5] + 0.4F * length);
        } else {
            local.add(0.0F, 0.0F, 40.0F);
        }
        local.y -= assembly.hull().min().y();
        new Quaternionf().rotationY((float) Math.PI).transform(local);
        float scale = DroneVisualScale.blocksPerMillimetre(DroneRenderConfig.get().visualScale);
        return feet.add(local.x * scale, local.y * scale, local.z * scale);
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static void place(ServerPlayer player, double x, double y, double z) {
        player.teleportTo(player.level(), x, y, z, Set.of(), 0.0F, 0.0F, true);
    }

    private static void look(LocalPlayer player, Vec3 target) {
        Vec3 eye = player.getEyePosition();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.xRotO = pitch;
        player.setYHeadRot(yaw);
        player.yHeadRotO = yaw;
        player.setYBodyRot(yaw);
        player.yBodyRotO = yaw;
    }

    private static StepResult awaitServer(StepContext ctx, String what) {
        CompletableFuture<?> future = (CompletableFuture<?>) ctx.state().get(FUTURE);
        if (future == null) {
            return StepResult.fail(what + ": keine Server-Aktion");
        }
        if (future.isCompletedExceptionally()) {
            return StepResult.fail(what + ": " + future.exceptionNow());
        }
        return future.isDone() ? StepResult.pass() : StepResult.pending();
    }

    private static void spawnDrone(ServerLevel level, Vec3 pos, ItemStack stack) {
        QuadcopterEntity drone = DroneModule.DRONE.get().create(level, EntitySpawnReason.COMMAND);
        if (drone == null) {
            throw new IllegalStateException("Drohne nicht erzeugt");
        }
        drone.loadFromStack(stack);
        drone.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        level.addFreshEntity(drone);
        drone.resetTo(pos, new Quaternionf().rotationY((float) Math.PI));
        DRONE.set(drone.getId());
    }

    private static void discardDrone(MinecraftServer server) {
        int id = DRONE.getAndSet(-1);
        if (id < 0) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity instanceof QuadcopterEntity drone) {
                drone.discard();
            }
        }
    }

    private static StepResult prepareStep(StepContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        Map<String, Object> state = ctx.state();
        if (!state.containsKey(FUTURE)) {
            if (mc.gui.screen() != null) {
                mc.gui.setScreen(null);
            }
            StepResult resized = Screenshots.resizeWindow(ctx);
            if (resized.status() != StepResult.Status.PASS) {
                return resized;
            }
            savedFov = mc.options.fov().get();
            state.put(FUTURE, ctx.onServer(server -> {
                ServerPlayer player = player(server);
                ServerLevel level = player.level();
                for (String command : List.of("time set noon", "weather clear")) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
                }
                int x = player.getBlockX() + OFFSET_X;
                int z = player.getBlockZ() + OFFSET_Z;
                level.getChunk(x >> 4, z >> 4);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                BASE.set(new BlockPos(x, y, z));
                player.setGameMode(GameType.SPECTATOR);
                player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                place(player, x + 0.5, y + 1.0, z - 2.5);
            }));
            return StepResult.pending();
        }
        StepResult server = awaitServer(ctx, "Platz vorbereiten");
        if (server.status() != StepResult.Status.PASS) {
            return server;
        }
        BlockPos base = BASE.get();
        if (mc.player == null || mc.level == null || base == null || mc.player.blockPosition().distManhattan(base) > 6) {
            return ctx.tick() > 380 ? StepResult.fail("Spieler kam nicht am Platz an") : StepResult.pending();
        }
        return StepResult.pass();
    }

    private static StepResult cleanupStep(StepContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (ctx.tick() == 0) {
            if (mc.gui.screen() != null) {
                mc.gui.setScreen(null);
            }
            if (savedFov > 0) {
                mc.options.fov().set(savedFov);
            }
            if (mc.gui.hud.isHidden()) {
                mc.gui.hud.toggle();
            }
            ctx.state().put(FUTURE, ctx.onServer(server -> {
                discardDrone(server);
                ServerPlayer player = player(server);
                BlockPos base = BASE.get();
                if (base != null) {
                    for (int k = 0; k < STAND; k++) {
                        if (player.level().getBlockState(base.above(k)).is(Blocks.POLISHED_DEEPSLATE)) {
                            player.level().setBlockAndUpdate(base.above(k), Blocks.AIR.defaultBlockState());
                        }
                    }
                }
                player.setGameMode(GameType.CREATIVE);
            }));
            return StepResult.pending();
        }
        StepResult server = awaitServer(ctx, "Aufräumen");
        if (server.status() != StepResult.Status.PASS) {
            return server;
        }
        return Screenshots.restoreWindow(ctx);
    }

    private static final class RearScene implements Screenshots.Scene {
        private final Shot shot;
        private Vec3 aim = Vec3.ZERO;

        private RearScene(Shot shot) {
            this.shot = shot;
        }

        @Override
        public StepResult prepare(StepContext ctx) {
            Minecraft mc = Minecraft.getInstance();
            Map<String, Object> state = ctx.state();
            if (!state.containsKey(FUTURE)) {
                BlockPos base = BASE.get();
                if (base == null) {
                    return StepResult.fail("Platz nicht vorbereitet");
                }
                BlockPos stand = base;
                DroneBuild build = this.shot.build();
                Vec3 feet = Vec3.atBottomCenterOf(stand.above(STAND));
                this.aim = plugCenter(build, feet);
                boolean under = !build.frame().definition().geometry().batteryOnTop();
                double range = Math.clamp(DroneVisuals.estimatedRadiusBlocks(build) * RANGE_PER_RADIUS, MIN_RANGE, MAX_RANGE);
                double pitch = Math.toRadians(under ? UNDER_PITCH : TOP_PITCH);
                double yaw = Math.toRadians(180.0 + SIDE_DEGREES);
                Vec3 eye = this.aim.add(-Math.sin(yaw) * Math.cos(pitch) * range, Math.sin(pitch) * range, Math.cos(yaw) * Math.cos(pitch) * range);
                ItemStack stack = this.shot.stack();
                ctx.log(this.shot.file() + ": Abstand " + String.format(Locale.ROOT, "%.2f", range) + " Blöcke, Akku "
                        + (under ? "unten" : "oben"));
                state.put(FUTURE, ctx.onServer(server -> {
                    ServerPlayer player = player(server);
                    ServerLevel level = player.level();
                    for (int k = 0; k < STAND; k++) {
                        level.setBlockAndUpdate(stand.above(k), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                    }
                    player.setGameMode(GameType.SPECTATOR);
                    place(player, eye.x, eye.y - player.getEyeHeight(), eye.z);
                    spawnDrone(level, feet, stack);
                }));
                return StepResult.pending();
            }
            StepResult server = awaitServer(ctx, this.shot.file());
            if (server.status() != StepResult.Status.PASS) {
                return server;
            }
            if (mc.level == null || !(mc.level.getEntity(DRONE.get()) instanceof QuadcopterEntity drone)
                    || !drone.build().equals(this.shot.build())) {
                return ctx.tick() > 200 ? StepResult.fail(this.shot.file() + ": Drohne kam nicht beim Client an") : StepResult.pending();
            }
            mc.options.fov().set(FOV);
            if (!mc.gui.hud.isHidden()) {
                mc.gui.hud.toggle();
            }
            return StepResult.pass();
        }

        @Override
        public void hold(StepContext ctx) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                look(mc.player, this.aim);
            }
        }

        @Override
        public void finish(StepContext ctx) {
            Minecraft mc = Minecraft.getInstance();
            if (savedFov > 0) {
                mc.options.fov().set(savedFov);
            }
            if (mc.gui.hud.isHidden()) {
                mc.gui.hud.toggle();
            }
            ctx.onServer(PowerPlugSelfTests::discardDrone);
        }
    }
}
