package de.lardtom.justmoreparts.dev.client;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.dev.SelfTest;
import de.lardtom.justmoreparts.dev.SelfTest.StepResult;
import de.lardtom.propwash.api.PropwashApi;
import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class CompareShots {
    public static final String DIR = "compare";
    private static final String TAG = "jmp_compare";
    private static final String FUTURE = "future";
    private static final String SELECTED = "selected";
    private static final int ARENA_OFFSET = 48;
    private static final int WALL_Z = -3;
    private static final int WALL_HALF_WIDTH = 4;
    private static final int WALL_HEIGHT = 4;
    private static final int SETTLE_TICKS = 70;
    private static final List<Holder<Attribute>> REACH = List.of(Attributes.BLOCK_INTERACTION_RANGE, Attributes.ENTITY_INTERACTION_RANGE);
    private static final List<Pair> PAIRS = List.of(
            new Pair("motor", "m2207_1750", "m2207_1950", true),
            new Pair("motor_xclass", "m2807_1300", "m4214_400", false),
            new Pair("prop", "p51_3b", "p130_46_3b_hard", false),
            new Pair("prop_xclass", "p7_2b", "p330_70_2b_carbon", false),
            new Pair("battery", "b6s_1300", "b6s_2200", true),
            new Pair("battery_xclass", "b6s_1300", "b8s_5000", false),
            new Pair("stack", "stack_30", "stack_30_6s_60a", false),
            new Pair("frame", "freestyle_5", "deadcat_5", false),
            new Pair("frame_xclass", "long_range_7", "xclass_13", false),
            new Pair("video", "digital_hd", "digital_hd_pro", false));
    private static volatile @Nullable BlockPos origin;
    private static volatile @Nullable Vec3 home;

    private CompareShots() {
    }

    public static void register() {
        for (Pair pair : PAIRS) {
            for (View view : View.values()) {
                if (view == View.SWAP && !pair.swap()) {
                    continue;
                }
                Screenshots.step("vergleich: " + pair.name() + ", " + view.label, DIR + "/jmp_compare_" + pair.name() + "_" + view.suffix,
                        SETTLE_TICKS, new CompareScene(pair, view));
            }
        }
        SelfTest.clientStep("vergleich: aufräumen", 200, CompareShots::cleanupStep);
    }

    private enum View {
        FIRST("first", "erste Person, JMP rechts", false, CameraType.FIRST_PERSON, 180.0F, 14.0F, 0.55),
        SWAP("swap", "erste Person, JMP links", true, CameraType.FIRST_PERSON, 180.0F, 14.0F, 0.55),
        THIRD("third", "dritte Person von vorn", false, CameraType.THIRD_PERSON_FRONT, 0.0F, -6.0F, 1.45);

        private final String suffix;
        private final String label;
        private final boolean jmpLeft;
        private final CameraType camera;
        private final float yaw;
        private final float pitch;
        private final double groundOffset;

        View(String suffix, String label, boolean jmpLeft, CameraType camera, float yaw, float pitch, double groundOffset) {
            this.suffix = suffix;
            this.label = label;
            this.jmpLeft = jmpLeft;
            this.camera = camera;
            this.yaw = yaw;
            this.pitch = pitch;
            this.groundOffset = groundOffset;
        }

        private boolean mainIsRight() {
            return this.camera == CameraType.FIRST_PERSON;
        }
    }

    private record Pair(String name, String propwash, String jmp, boolean swap) {
        private ItemStack stack(boolean own) {
            return PropwashApi.partItems().stack(own ? JustMoreParts.id(this.jmp) : JustMoreParts.propwash(this.propwash));
        }
    }

    private static final class CompareScene implements Screenshots.Scene {
        private final Pair pair;
        private final View view;

        private CompareScene(Pair pair, View view) {
            this.pair = pair;
            this.view = view;
        }

        @Override
        public StepResult prepare(SelfTest.StepContext ctx) {
            Minecraft mc = Minecraft.getInstance();
            Map<String, Object> state = ctx.state();
            ItemStack left = this.pair.stack(this.view.jmpLeft);
            ItemStack right = this.pair.stack(!this.view.jmpLeft);
            if (left.isEmpty() || right.isEmpty()) {
                return StepResult.fail("Teil fehlt: " + this.pair.propwash() + " oder " + this.pair.jmp());
            }
            if (!state.containsKey(FUTURE)) {
                mc.gui.setScreen(null);
                mc.getTutorial().setStep(TutorialSteps.NONE);
                if (mc.player != null) {
                    mc.player.getInventory().clearContent();
                }
                state.put(FUTURE, ctx.onServer(server -> arrange(server, this.view, left, right)));
                return StepResult.pending();
            }
            CompletableFuture<?> future = (CompletableFuture<?>) state.get(FUTURE);
            if (!future.isDone()) {
                return StepResult.pending();
            }
            if (future.isCompletedExceptionally()) {
                return StepResult.fail("Aufbau: " + future.exceptionNow());
            }
            LocalPlayer player = mc.player;
            BlockPos o = origin;
            if (player == null || mc.level == null || o == null) {
                return StepResult.fail("kein Spieler");
            }
            if (!state.containsKey(SELECTED)) {
                if (!ItemStack.isSameItem(player.getInventory().getItem(0), left) || !ItemStack.isSameItem(player.getInventory().getItem(1), right)) {
                    return ctx.tick() > 300 ? StepResult.fail("Schnellleiste kommt nicht an") : StepResult.pending();
                }
                player.getInventory().setSelectedSlot(this.view.mainIsRight() ? 1 : 0);
                state.put(SELECTED, ctx.tick());
                return StepResult.pending();
            }
            ItemStack main = this.view.mainIsRight() ? right : left;
            ItemStack off = this.view.mainIsRight() ? left : right;
            AABB arena = arena(o);
            long frames = mc.level.getEntitiesOfClass(ItemFrame.class, arena).stream().filter(frame -> !frame.getItem().isEmpty()).count();
            long ground = mc.level.getEntitiesOfClass(ItemEntity.class, arena).size();
            long labels = mc.level.getEntitiesOfClass(Display.TextDisplay.class, arena).size();
            boolean hands = ItemStack.isSameItem(player.getMainHandItem(), main) && ItemStack.isSameItem(player.getOffhandItem(), off);
            boolean placed = player.position().distanceTo(Vec3.atBottomCenterOf(o)) < 0.2;
            if (frames != 2 || ground != 2 || labels != 2 || !hands || !placed) {
                return ctx.tick() > 400 ? StepResult.fail("Vergleichsaufbau unvollständig: Rahmen " + frames + ", Boden " + ground + ", Schilder "
                        + labels + ", Hände " + hands + ", Position " + placed) : StepResult.pending();
            }
            mc.options.setCameraType(this.view.camera);
            this.hold(ctx);
            return ctx.tick() - (int) state.get(SELECTED) > 10 ? StepResult.pass() : StepResult.pending();
        }

        @Override
        public void hold(SelfTest.StepContext ctx) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            player.setYRot(this.view.yaw);
            player.setXRot(this.view.pitch);
            player.yRotO = this.view.yaw;
            player.xRotO = this.view.pitch;
            player.setYHeadRot(this.view.yaw);
            player.yHeadRotO = this.view.yaw;
            player.setYBodyRot(this.view.yaw);
            player.yBodyRotO = this.view.yaw;
            player.setDeltaMovement(Vec3.ZERO);
        }

        @Override
        public void finish(SelfTest.StepContext ctx) {
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
            ctx.onServer(server -> {
                ServerPlayer player = player(server);
                BlockPos o = origin;
                if (o != null) {
                    clearEntities(player.level(), o);
                }
                player.getInventory().clearContent();
                player.inventoryMenu.sendAllDataToRemote();
            });
        }
    }

    private static void arrange(MinecraftServer server, View view, ItemStack left, ItemStack right) {
        ServerPlayer player = player(server);
        ServerLevel level = player.level();
        if (origin == null) {
            home = player.position();
            BlockPos at = player.blockPosition();
            int x = at.getX() + ARENA_OFFSET;
            int z = at.getZ();
            origin = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
        }
        BlockPos o = origin;
        clearEntities(level, o);
        wall(level, o, Blocks.CONCRETE.pick(DyeColor.WHITE).defaultBlockState());
        reach(player, false);
        player.getInventory().clearContent();
        player.getInventory().setItem(0, left.copy());
        player.getInventory().setItem(1, right.copy());
        player.setItemInHand(InteractionHand.OFF_HAND, (view.mainIsRight() ? left : right).copy());
        player.inventoryMenu.sendAllDataToRemote();
        player.teleportTo(level, o.getX() + 0.5, o.getY(), o.getZ() + 0.5, Set.of(), view.yaw, view.pitch, true);
        frame(level, o.offset(-1, 1, WALL_Z + 1), left);
        frame(level, o.offset(1, 1, WALL_Z + 1), right);
        double groundZ = o.getZ() + WALL_Z + 1.7;
        item(level, o.getX() + 0.5 - view.groundOffset, o.getY() + 0.02, groundZ, left);
        item(level, o.getX() + 0.5 + view.groundOffset, o.getY() + 0.02, groundZ, right);
        label(server, level, o.getX() - 0.5, o.getY() + 2.08, o.getZ() + WALL_Z + 1.06, left, !view.jmpLeft);
        label(server, level, o.getX() + 1.5, o.getY() + 2.08, o.getZ() + WALL_Z + 1.06, right, view.jmpLeft);
    }

    private static void frame(ServerLevel level, BlockPos pos, ItemStack stack) {
        ItemFrame frame = new ItemFrame(level, pos, Direction.SOUTH);
        frame.setItem(stack.copy(), false);
        frame.addTag(TAG);
        level.addFreshEntity(frame);
    }

    private static void item(ServerLevel level, double x, double y, double z, ItemStack stack) {
        ItemEntity entity = new ItemEntity(level, x, y, z, stack.copy(), 0.0, 0.0, 0.0);
        entity.setNeverPickUp();
        entity.setUnlimitedLifetime();
        entity.addTag(TAG);
        level.addFreshEntity(entity);
    }

    private static void label(MinecraftServer server, ServerLevel level, double x, double y, double z, ItemStack stack, boolean base) {
        Component text = Component.empty()
                .append(Component.literal(base ? "Propwash FPV" : "Just More Parts").withStyle(base ? ChatFormatting.GOLD : ChatFormatting.AQUA, ChatFormatting.BOLD))
                .append("\n")
                .append(stack.getHoverName());
        Tag encoded = ComponentSerialization.CODEC.encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), text).getOrThrow();
        String command = String.format(Locale.ROOT, "summon minecraft:text_display %.3f %.3f %.3f {text:%s,billboard:\"center\",alignment:\"center\",line_width:240,transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.4f,0.4f,0.4f]},Tags:[\"%s\"]}",
                x, y, z, encoded, TAG);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }

    private static void reach(ServerPlayer player, boolean restore) {
        Identifier id = JustMoreParts.id("compare_shots");
        for (Holder<Attribute> attribute : REACH) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            if (restore) {
                instance.removeModifier(id);
            } else {
                instance.addOrUpdateTransientModifier(new AttributeModifier(id, -64.0, AttributeModifier.Operation.ADD_VALUE));
            }
        }
    }

    private static void wall(ServerLevel level, BlockPos o, BlockState block) {
        for (int dx = -WALL_HALF_WIDTH; dx <= WALL_HALF_WIDTH; dx++) {
            for (int dy = 0; dy < WALL_HEIGHT; dy++) {
                level.setBlockAndUpdate(o.offset(dx, dy, WALL_Z), block);
            }
        }
    }

    private static AABB arena(BlockPos o) {
        return new AABB(o.getX() - 8, o.getY() - 2, o.getZ() - 8, o.getX() + 9, o.getY() + 8, o.getZ() + 9);
    }

    private static void clearEntities(ServerLevel level, BlockPos o) {
        for (Entity entity : level.getEntities((Entity) null, arena(o), e -> e.entityTags().contains(TAG))) {
            entity.discard();
        }
    }

    private static StepResult cleanupStep(SelfTest.StepContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        Map<String, Object> state = ctx.state();
        if (!state.containsKey(FUTURE)) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            state.put(FUTURE, ctx.onServer(server -> {
                ServerPlayer player = player(server);
                BlockPos o = origin;
                Vec3 back = home;
                if (o != null) {
                    clearEntities(player.level(), o);
                    wall(player.level(), o, Blocks.AIR.defaultBlockState());
                }
                player.getInventory().clearContent();
                player.inventoryMenu.sendAllDataToRemote();
                reach(player, true);
                if (back != null) {
                    player.teleportTo(player.level(), back.x, back.y, back.z, Set.of(), player.getYRot(), 0.0F, true);
                }
                origin = null;
                home = null;
            }));
            return StepResult.pending();
        }
        CompletableFuture<?> future = (CompletableFuture<?>) state.get(FUTURE);
        if (!future.isDone()) {
            return StepResult.pending();
        }
        return future.isCompletedExceptionally() ? StepResult.fail("Aufräumen: " + future.exceptionNow()) : StepResult.pass();
    }

    private static ServerPlayer player(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            throw new IllegalStateException("kein Spieler auf dem Server");
        }
        return players.getFirst();
    }
}
